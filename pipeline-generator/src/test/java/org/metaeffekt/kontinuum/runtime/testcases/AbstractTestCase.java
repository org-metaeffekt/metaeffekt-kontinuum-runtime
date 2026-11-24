package org.metaeffekt.kontinuum.runtime.testcases;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.metaeffekt.kontinuum.runtime.generator.gitlab.GitlabPipeline;
import org.metaeffekt.kontinuum.runtime.generator.local.LocalPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Resolves the configuration templates of the test cases. See {@code test-cases/README.md} for the layout and the
 * placeholders of the templates.
 */
@Slf4j
abstract class AbstractTestCase {

    private static final Path TEST_CASES_BASE_DIR = Path.of("src/test/resources/test-cases").toAbsolutePath().normalize();
    private static final Path RESULTS_BASE_DIR = Path.of("target/test-cases").toAbsolutePath().normalize();

    // FIXME: read from local.properties on runtime-level
    private final Path KONTINUUM_DIR = Path.of("../../metaeffekt-kontinuum").toAbsolutePath().normalize();

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private static final long TIMEOUT_MINUTES = 10;

    protected static final Path SHARED_WORKBENCH_DIR = TEST_CASES_BASE_DIR.resolve("shared").resolve("workbench");

    static Path testCaseBaseDir(String testCase) {
        return TEST_CASES_BASE_DIR.resolve(testCase);
    }

    static Path testCaseWorkbenchDir(String testCase) {
        return testCaseBaseDir(testCase).resolve("workbench");
    }

    static Path testCaseWorkspaceDir(String testCase) {
        return testCaseBaseDir(testCase).resolve("workspace");
    }

    static Path resultBaseDir(String testCase) {
        return RESULTS_BASE_DIR.resolve(testCase);
    }

    /**
     * Replaces the placeholders of the configuration template of a test case with absolute paths and writes the
     * result to {@code target/test-cases/<test case>/}. Only this copy is to be used by the test. Fails the test if
     * a placeholder is left after the replacement.
     *
     * @param testCase       the test case, e.g. {@code tc-0001}
     * @param configFileName the file name of the template in the workbench of the test case
     *
     * @return the resolved configuration file
     *
     * @throws IOException if the template cannot be read or the resolved configuration cannot be written
     */
    static Path resolveConfigTemplate(String testCase, String configFileName) throws IOException {
        Map<String, Path> placeholders = Map.of(
                "workbench.dir", testCaseWorkbenchDir(testCase),
                "workspace.dir", testCaseWorkspaceDir(testCase),
                "shared.workbench.dir", SHARED_WORKBENCH_DIR);

        String config = Files.readString(testCaseWorkbenchDir(testCase).resolve(configFileName));
        for (Map.Entry<String, Path> placeholder : placeholders.entrySet()) {
            config = config.replace("${" + placeholder.getKey() + "}", placeholder.getValue().toString());
        }

        Matcher unresolved = PLACEHOLDER.matcher(config);
        if (unresolved.find()) {
            fail("Unknown placeholder ${" + unresolved.group(1) + "} in " + configFileName
                 + ", known placeholders: " + placeholders.keySet());
        }

        Path resolvedConfigDir = resultBaseDir(testCase);
        Files.createDirectories(resolvedConfigDir);
        Path resolvedConfig = resolvedConfigDir.resolve(configFileName);
        Files.writeString(resolvedConfig, config);
        return resolvedConfig;
    }

    protected void runScript(Path scriptFile) throws Exception {
        Path logFile = getResultBaseDir().resolve(getTestCaseId() + "-pipeline.log");
        log.info("Running pipeline script [{}], output is written to [{}].", scriptFile, logFile);
        Instant start = Instant.now();
        Process process = new ProcessBuilder(List.of("bash", scriptFile.toString()))
                .directory(RESULTS_BASE_DIR.toFile())
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();

        if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            fail("Execution of the pipeline script timed out after " + TIMEOUT_MINUTES + " minutes:\n"
                    + Files.readString(logFile));
        }
        log.info("Pipeline script [{}] finished with exit code [{}] in [{}] seconds.",
                scriptFile, process.exitValue(), Duration.between(start, Instant.now()).toSeconds());
        assertEquals(0, process.exitValue(),
                "Execution of the pipeline script failed:\n" + Files.readString(logFile));
    }

    protected Path resolveConfigTemplate() throws IOException {
        return resolveConfigTemplate(getTestCaseId(), getConfigFileName());
    }

    protected String generateBashPipelineScript(Path configFile, Path scriptFile) throws IOException {
        PipelineConfiguration configuration = new PipelineConfigurationLoader().readConfig(configFile.toFile());
        LocalConfiguration localConfiguration = LocalConfiguration.builder()
                .WORKBENCH_DIR(getTestCaseWorkbenchDir().toString())
                .WORKSPACE_DIR(getResultWorkspaceDir().toString())
                .KONTINUUM_DIR(getKontinuumDir().toString())
                .MAVEN_CLI_OPTS("-q")
                .build();
        String script = new LocalPipeline(configuration, localConfiguration).generatePipeline();

        scriptFile.toFile().getParentFile().mkdirs();
        Files.writeString(scriptFile, script);
        return script;
    }

    protected String generateGitlabPipelineScript(Path configFile, Path scriptFile) throws IOException {
        PipelineConfiguration configuration = new PipelineConfigurationLoader().readConfig(configFile.toFile());
        GitlabConfiguration gitlabConfiguration = GitlabConfiguration.builder()
                .WORKBENCH_DIR(getTestCaseWorkbenchDir().toString())
                .WORKSPACE_DIR(getResultWorkspaceDir().toString())
                .KONTINUUM_DIR(getKontinuumDir().toString())
                .MAVEN_CLI_OPTS("-q")
                .build();
        String script = new GitlabPipeline(configuration, gitlabConfiguration).generatePipeline();

        scriptFile.toFile().getParentFile().mkdirs();
        Files.writeString(scriptFile, script);
        return script;
    }

    @BeforeEach
    void requireKontinuum() {
        assumeTrue(Files.isDirectory(getKontinuumDir().resolve("processors")),
                "Kontinuum checkout not found at [" + getKontinuumDir() + "].");
    }

    @BeforeEach
    void cleanupResultsDir() {
        File file = resultBaseDir(getTestCaseId()).toFile();
        if (file.exists()) {
            delete(file);
        }
    }

    private static void delete(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                delete(child);
            }
        }
        assertTrue(file.delete(), "Cannot delete [" + file + "].");
    }

    protected Path getTestCaseWorkbenchDir() {
        return testCaseWorkbenchDir(getTestCaseId());
    }

    protected Path getTestCaseWorkspaceDir() {
        return testCaseWorkspaceDir(getTestCaseId());
    }

    protected Path getKontinuumDir(){
        return KONTINUUM_DIR;
    }

    protected Path getResultBaseDir() {
        return resultBaseDir(getTestCaseId());
    }

    protected Path getResultWorkingDir() {
        return getResultBaseDir().resolve("working");
    }

    protected Path getResultWorkspaceDir() {
        return getResultBaseDir().resolve("workspace");
    }

    protected Path getResultProjectWorkspaceDir() {
        return getResultWorkspaceDir().resolve(getTestCaseId());
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestCaseMetadata {
        String id();
        String configFileName();
    }

    private TestCaseMetadata getTestCaseMetadata(){
        TestCaseMetadata annotation = this.getClass().getAnnotation(TestCaseMetadata.class);
        if (annotation == null) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " does not have required annotation: " + TestCaseMetadata.class.getSimpleName());
        }
        return annotation;
    }

    protected String getTestCaseId() {
        return getTestCaseMetadata().id();
    }

    protected String getConfigFileName() {
        return getTestCaseMetadata().configFileName();
    }

    protected static void assertParameter(String key, String value, String script) {
        assertContains("-D" + key + "=" + value, script);
    }

    protected static void assertContains(String expected, String script) {
        assertTrue(script.contains(expected), "Generated script expected to contain [" + expected + "].");
    }

    protected static void assertNotContains(String expected, String script) {
        assertFalse(script.contains(expected), "Generated script expected to not contain [" + expected + "].");
    }

}
