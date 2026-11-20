package org.metaeffekt.kontinuum.runtime.testcases;

import org.metaeffekt.kontinuum.runtime.generator.local.LocalPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resolves the configuration templates of the test cases. See {@code test-cases/README.md} for the layout and the
 * placeholders of the templates.
 */
abstract class AbstractTestCase {

    static final Path TEST_CASES_DIR = Path.of("src/test/resources/test-cases").toAbsolutePath().normalize();

    static final Path SHARED_WORKBENCH_DIR = TEST_CASES_DIR.resolve("shared").resolve("workbench");

    // FIXME: read from local.properties on runtime-level
    private final Path KONTINUUM_DIR = Path.of("../../metaeffekt-kontinuum").toAbsolutePath().normalize();

    private static final Path RESOLVED_CONFIG_ROOT = Path.of("target/test-cases").toAbsolutePath();

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    static Path workbenchDir(String testCase) {
        return TEST_CASES_DIR.resolve(testCase).resolve("workbench");
    }

    static Path workspaceDir(String testCase) {
        return TEST_CASES_DIR.resolve(testCase).resolve("workspace");
    }

    static final long TIMEOUT_MINUTES = 10;

    private Path resultBaseDir = Path.of("target/test-cases").toAbsolutePath().normalize() ;

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
    static Path resolve(String testCase, String configFileName) throws IOException {
        Map<String, Path> placeholders = Map.of(
                "workbench.dir", workbenchDir(testCase),
                "workspace.dir", workspaceDir(testCase),
                "shared.workbench.dir", SHARED_WORKBENCH_DIR);

        String config = Files.readString(workbenchDir(testCase).resolve(configFileName));
        for (Map.Entry<String, Path> placeholder : placeholders.entrySet()) {
            config = config.replace("${" + placeholder.getKey() + "}", placeholder.getValue().toString());
        }

        Matcher unresolved = PLACEHOLDER.matcher(config);
        if (unresolved.find()) {
            fail("Unknown placeholder ${" + unresolved.group(1) + "} in " + configFileName
                 + ", known placeholders: " + placeholders.keySet());
        }

        Path resolvedConfigDir = RESOLVED_CONFIG_ROOT.resolve(testCase);
        Files.createDirectories(resolvedConfigDir);
        Path resolvedConfig = resolvedConfigDir.resolve(configFileName);
        Files.writeString(resolvedConfig, config);
        return resolvedConfig;
    }

    protected void runScript(Path scriptFile) throws Exception {
        Path logFile = getResultDir().resolve(getTestCaseId() + "-pipeline.log");
        Process process = new ProcessBuilder(List.of("bash", scriptFile.toString()))
                .directory(resultBaseDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();

        if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            fail("Execution of the pipeline script timed out after " + TIMEOUT_MINUTES + " minutes:\n"
                    + Files.readString(logFile));
        }
        assertEquals(0, process.exitValue(),
                "Execution of the pipeline script failed:\n" + Files.readString(logFile));
    }

    protected Path resolveConfigTemplate() throws IOException {
        return AbstractTestCase.resolve(getTestCaseId(), getConfigFileName());
    }

    protected String generateScript(Path configFile, Path scriptFile) throws IOException {
        PipelineConfiguration configuration = new PipelineConfigurationLoader().readConfig(configFile.toFile());
        LocalConfiguration localConfiguration = LocalConfiguration.builder()
                .WORKBENCH_DIR(getTestCasesBaseDir().resolve("workbench").toString())
                .WORKSPACE_DIR(getResultWorkspaceDir().toString())
                .KONTINUUM_DIR(getKontinuumDir().toString())
                .MAVEN_CLI_OPTS("-q")
                .build();
        String script = new LocalPipeline(configuration, localConfiguration).generatePipeline();

        scriptFile.toFile().getParentFile().mkdirs();
        Files.writeString(scriptFile, script);
        return script;
    }

    protected abstract String getTestCaseId();
    protected abstract String getConfigFileName();

    protected Path getTestCasesBaseDir() {
        return workbenchDir(getTestCaseId());
    }

    protected Path getKontinuumDir(){
        return KONTINUUM_DIR;
    }

    protected Path getResultDir() {
        return resultBaseDir.resolve(getTestCaseId());
    }

    protected Path getResultWorkingDir() {
        return getResultDir().resolve("working");
    }

    protected Path getResultWorkspaceDir() {
        return getResultDir().resolve("workspace");
    }

    protected static void assertParameter(String key, String value, String script) {
        String expected = "-D" + key + "=" + value;
        assertTrue(script.contains(expected), "Generated script expected to contain [" + expected + "].");
    }

}
