package org.metaeffekt.kontinuum.runtime.testcases;

import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.PropertiesUtils;
import org.metaeffekt.kontinuum.runtime.generator.gitlab.GitlabPipeline;
import org.metaeffekt.kontinuum.runtime.generator.local.LocalPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base class of the test cases. The test case and its configuration are derived from the class name
 * {@code TestCase<NNNN>Config<NN>Test}, e.g. {@code TestCase0001Config02Test} uses the configuration template
 * {@code tc-0001/workbench/tc-0001-02-config.yaml} and writes its results to {@code target/test-cases/tc-0001-02/}.
 * <p>
 * Before the tests of a class, the result directory is cleaned and the placeholders of the configuration template
 * are replaced with absolute paths. The tests generate the local or GitLab pipeline from this resolved copy and check
 * it with {@link GeneratedPipeline}. Roundtrip tests (tagged {@value #ROUNDTRIP}) also execute the local pipeline. It
 * is executed from the result directory, not from the workbench, so paths must not work by accident. Roundtrips
 * require a Kontinuum checkout and are skipped otherwise.
 * <p>
 * One instance is used for all tests of a class, so the local pipeline is executed at most once and all roundtrip
 * tests of a class check the same result, which is kept after the run. See {@code test-cases/README.md} for the
 * layout and the placeholders of the templates.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractTestCase {

    /**
     * Tag of the tests that execute the local pipeline. Roundtrips take long and require a Kontinuum checkout, so they
     * can be excluded ({@code -DexcludedGroups=roundtrip}) or run on their own ({@code -Dgroups=roundtrip}).
     */
    protected static final String ROUNDTRIP = "roundtrip";

    private static final Pattern CLASS_NAME = Pattern.compile("TestCase(\\d{4})Config(\\d{2})Test");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private static final Path TEST_CASES_BASE_DIR = Path.of("src/test/resources/test-cases").toAbsolutePath().normalize();
    private static final Path RESULTS_BASE_DIR = Path.of("target/test-cases").toAbsolutePath().normalize();
    private static final Path SHARED_WORKBENCH_DIR = TEST_CASES_BASE_DIR.resolve("shared").resolve("workbench");

    private static final Path LOCAL_PROPERTIES_FILE = Path.of("../.local.properties").toAbsolutePath().normalize();
    private static final String KONTINUUM_DIR_PROPERTY = "ae.kontinuum.dir";
    private static final String DEFAULT_KONTINUUM_DIR = "../../metaeffekt-kontinuum";
    private static final Path KONTINUUM_DIR = resolveKontinuumDir();

    private static final String LOCAL_PIPELINE_FILE = "pipeline.sh";
    private static final String GITLAB_PIPELINE_FILE = "gitlab-ci.yml";
    private static final String PIPELINE_LOG_FILE = "pipeline.log";

    private static final long TIMEOUT_MINUTES = 10;

    /** The test case, e.g. {@code tc-0001}. */
    private final String testCaseId;

    /** The configuration of the test case, e.g. {@code tc-0001-02}. */
    private final String configId;

    private Path resolvedConfigFile;
    private String projectId;

    private boolean localPipelineExecuted;
    private Throwable localPipelineFailure;

    protected AbstractTestCase() {
        Matcher className = CLASS_NAME.matcher(getClass().getSimpleName());
        if (!className.matches()) {
            throw new IllegalStateException("Test case class " + getClass().getSimpleName()
                                            + " must be named TestCase<NNNN>Config<NN>Test, e.g. TestCase0001Config01Test.");
        }
        testCaseId = "tc-" + className.group(1);
        configId = testCaseId + "-" + className.group(2);
    }

    @BeforeAll
    void prepareResultDir() throws IOException {
        FileUtils.deleteDirectory(getResultBaseDir().toFile());
        resolvedConfigFile = resolveConfigTemplate();
        projectId = loadConfiguration().getProjectProperties().getProject().getId();
    }

    /**
     * Generates the local pipeline script of the configuration and writes it to the result directory.
     *
     * @return the generated script
     *
     * @throws IOException if the script cannot be written
     */
    protected GeneratedPipeline generateLocalPipeline() throws IOException {
        LocalConfiguration localConfiguration = LocalConfiguration.builder()
                .WORKBENCH_DIR(getTestCaseWorkbenchDir().toString())
                .WORKSPACE_DIR(getResultWorkspaceDir().toString())
                .KONTINUUM_DIR(KONTINUUM_DIR.toString())
                .MAVEN_CLI_OPTS("-q")
                .build();
        String script = new LocalPipeline(loadConfiguration(), localConfiguration).generatePipeline();
        write(getResultWorkingDir().resolve(LOCAL_PIPELINE_FILE), script);
        return new GeneratedPipeline(script);
    }

    /**
     * Generates the GitLab pipeline of the configuration and writes it to the result directory.
     *
     * @return the generated pipeline
     *
     * @throws IOException if the pipeline cannot be written
     */
    protected GeneratedPipeline generateGitlabPipeline() throws IOException {
        GitlabConfiguration gitlabConfiguration = GitlabConfiguration.builder()
                .WORKBENCH_DIR(getTestCaseWorkbenchDir().toString())
                .WORKSPACE_DIR(getResultWorkspaceDir().toString())
                .KONTINUUM_DIR(KONTINUUM_DIR.toString())
                .MAVEN_CLI_OPTS("-q")
                .build();
        String pipeline = new GitlabPipeline(loadConfiguration(), gitlabConfiguration).generatePipeline();
        write(getResultWorkingDir().resolve(GITLAB_PIPELINE_FILE), pipeline);
        return new GeneratedPipeline(pipeline);
    }

    /**
     * Generates and executes the local pipeline of the configuration. The pipeline is executed at most once per test
     * class; later calls reuse the result or fail with the error of the first execution. Skips the test if no
     * Kontinuum checkout is available.
     *
     * @throws Exception if the pipeline cannot be generated or executed
     */
    protected void runLocalPipeline() throws Exception {
        assumeTrue(Files.isDirectory(KONTINUUM_DIR.resolve("processors")),
                "Kontinuum checkout not found at [" + KONTINUUM_DIR + "].");
        if (localPipelineExecuted) {
            if (localPipelineFailure != null) {
                fail("Execution of the local pipeline already failed in an earlier test.", localPipelineFailure);
            }
            return;
        }

        localPipelineExecuted = true;
        try {
            generateLocalPipeline();
            runScript(getResultWorkingDir().resolve(LOCAL_PIPELINE_FILE));
        } catch (Exception | AssertionError e) {
            localPipelineFailure = e;
            throw e;
        }
    }

    protected Path getTestCaseWorkbenchDir() {
        return TEST_CASES_BASE_DIR.resolve(testCaseId).resolve("workbench");
    }

    protected Path getTestCaseWorkspaceDir() {
        return TEST_CASES_BASE_DIR.resolve(testCaseId).resolve("workspace");
    }

    /**
     * Returns the workbench shared by all test cases.
     *
     * @return the shared workbench directory
     */
    protected Path getSharedWorkbenchDir() {
        return SHARED_WORKBENCH_DIR;
    }

    /**
     * Returns the workspace directory of the project, named after the project id of the configuration.
     *
     * @return the project workspace directory in the result directory
     */
    protected Path getResultProjectWorkspaceDir() {
        return getResultWorkspaceDir().resolve(projectId);
    }

    /**
     * Returns a file of an asset in the project workspace, e.g. {@code 01_extracted/a/a-inventory.xlsx}.
     *
     * @param stage    the stage whose directory contains the file
     * @param assetId  the asset whose directory contains the file
     * @param fileName the name of the file
     *
     * @return the file in the result directory
     */
    protected Path stageFile(Stage stage, String assetId, String fileName) {
        return getResultProjectWorkspaceDir().resolve(stage.getStageDirectory()).resolve(assetId).resolve(fileName);
    }

    protected static void assertFileExists(Path file) {
        assertTrue(Files.isRegularFile(file), "File was not created: " + file);
    }

    /**
     * Fails the test if the actual file does not exist or differs from the expected file.
     */
    protected static void assertSameContent(Path expected, Path actual) throws IOException {
        assertFileExists(actual);
        assertEquals(-1, Files.mismatch(expected, actual), "File " + actual + " differs from " + expected + ".");
    }

    private void runScript(Path scriptFile) throws Exception {
        Path logFile = getResultBaseDir().resolve(PIPELINE_LOG_FILE);
        log.info("Running pipeline script [{}], output is written to [{}].", scriptFile, logFile);
        Instant start = Instant.now();
        Process process = new ProcessBuilder(List.of("bash", scriptFile.toString()))
                .directory(getResultBaseDir().toFile())
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

    private PipelineConfiguration loadConfiguration() {
        return new PipelineConfigurationLoader().readConfig(resolvedConfigFile.toFile());
    }

    /**
     * Replaces the placeholders of the configuration template with absolute paths and writes the result to the
     * result directory. Only this copy is to be used by the test. Fails the test if the template does not exist or a
     * placeholder is left after the replacement.
     *
     * @return the resolved configuration file
     *
     * @throws IOException if the template cannot be read or the resolved configuration cannot be written
     */
    private Path resolveConfigTemplate() throws IOException {
        String configFileName = configId + "-config.yaml";
        Path template = getTestCaseWorkbenchDir().resolve(configFileName);
        assertTrue(Files.isRegularFile(template),
                "Configuration template of " + getClass().getSimpleName() + " not found: " + template);

        Map<String, Path> placeholders = Map.of(
                "workbench.dir", getTestCaseWorkbenchDir(),
                "workspace.dir", getTestCaseWorkspaceDir(),
                "shared.workbench.dir", SHARED_WORKBENCH_DIR);

        String config = Files.readString(template);
        for (Map.Entry<String, Path> placeholder : placeholders.entrySet()) {
            config = config.replace("${" + placeholder.getKey() + "}", placeholder.getValue().toString());
        }

        Matcher unresolved = PLACEHOLDER.matcher(config);
        if (unresolved.find()) {
            fail("Unknown placeholder ${" + unresolved.group(1) + "} in " + configFileName
                 + ", known placeholders: " + placeholders.keySet());
        }

        Path resolvedConfig = getResultBaseDir().resolve(configFileName);
        write(resolvedConfig, config);
        return resolvedConfig;
    }

    private Path getResultBaseDir() {
        return RESULTS_BASE_DIR.resolve(configId);
    }

    private Path getResultWorkingDir() {
        return getResultBaseDir().resolve("working");
    }

    private Path getResultWorkspaceDir() {
        return getResultBaseDir().resolve("workspace");
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    /**
     * Returns the Kontinuum checkout configured by {@code ae.kontinuum.dir} in the {@code .local.properties} of the
     * runtime. Falls back to a checkout next to the runtime, so setups without the property keep working.
     */
    private static Path resolveKontinuumDir() {
        // values in .local.properties may be quoted
        String configuredDir = StringUtils.unwrap(StringUtils.trim(readLocalProperties().getProperty(KONTINUUM_DIR_PROPERTY)), '"');
        if (StringUtils.isEmpty(configuredDir)) {
            return Path.of(DEFAULT_KONTINUUM_DIR).toAbsolutePath().normalize();
        }
        return Path.of(configuredDir).toAbsolutePath().normalize();
    }

    private static Properties readLocalProperties() {
        if (!Files.isRegularFile(LOCAL_PROPERTIES_FILE)) {
            return new Properties();
        }
        return PropertiesUtils.loadPropertiesFile(LOCAL_PROPERTIES_FILE.toFile(), true);
    }

}
