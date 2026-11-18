package org.metaeffekt.kontinuum.runtime.testcases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Roundtrip for test case 0003: a Windows appliance extraction archive is extracted, and the prepared inventory of
 * the SumatraPDF artifact found in the appliance is selected by an extraction script. The configuration
 * template is resolved, loaded, the local pipeline script is generated and executed, and the selected inventory is
 * compared with the expected inventory. See {@code test-cases/README.md} for the layout and the placeholders of the
 * templates.
 * <p>
 * The script is executed from the workspace, not from the workbench, so paths must not work by accident.
 * <p>
 * Requires a Kontinuum checkout next to the runtime; skipped otherwise.
 */
class TestCase0003Test {

    private static final String TEST_CASE = "tc-0003";

    private static final Path TEST_CASES_DIR = Path.of("src/test/resources/test-cases").toAbsolutePath().normalize();

    private static final Path KONTINUUM_DIR = Path.of("../../metaeffekt-kontinuum").toAbsolutePath().normalize();

    private static final Path WORKBENCH_DIR = TEST_CASES_DIR.resolve(TEST_CASE).resolve("workbench");

    private static final Path WORKSPACE_DIR = TEST_CASES_DIR.resolve(TEST_CASE).resolve("workspace");

    private static final Path SHARED_WORKBENCH_DIR = TEST_CASES_DIR.resolve("shared").resolve("workbench");

    private static final Path RESOLVED_CONFIG_DIR = Path.of("target/test-cases").resolve(TEST_CASE).toAbsolutePath();

    private static final String CONFIG_FILE_NAME = "tc-0003-01-config.yaml";

    private static final Map<String, Path> PLACEHOLDERS = Map.of(
            "workbench.dir", WORKBENCH_DIR,
            "workspace.dir", WORKSPACE_DIR,
            "shared.workbench.dir", SHARED_WORKBENCH_DIR);

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private static final long TIMEOUT_MINUTES = 10;

    @TempDir
    Path runtimeWorkspaceDir;

    @BeforeEach
    void requireKontinuum() {
        assumeTrue(Files.isDirectory(KONTINUUM_DIR.resolve("processors")),
                "Kontinuum checkout not found at " + KONTINUUM_DIR);
    }

    @Test
    void generatedScriptExtractsWindowsArchiveAndSelectsInventory() throws IOException {
        String script = generateScript(resolveConfigTemplate());

        assertTrue(script.contains("-Dparam.asset.url='file:" + WORKBENCH_DIR + "/extractions/windows-extract.tar'"),
                script);
        assertTrue(script.contains("extract/extract_windows-inventory.xml"), script);
        assertTrue(script.contains("-Dparam.exclude.pattern.file='" + WORKBENCH_DIR + "/configs/exclude-patterns.yaml'"),
                script);
        assertTrue(script.contains("-Dinput.kotlin.script.file='" + WORKBENCH_DIR + "/scripts/select-inventory.kts'"),
                script);
        assertTrue(script.contains("\"artifactIdPattern\":\"SumatraPDF-.*\""), script);
        assertTrue(script.contains("\"inventoriesDir\":\"" + WORKBENCH_DIR + "/inventories\""), script);
        assertFalse(script.contains("# --- PRE:"), "Mirror step is not required for this test case:\n" + script);
    }

    @Test
    void selectsSumatraPdfInventoryByArtifactInAppliance() throws Exception {
        Path scriptFile = runtimeWorkspaceDir.resolve(TEST_CASE + "-pipeline.sh");
        Files.writeString(scriptFile, generateScript(resolveConfigTemplate()));

        runScript(scriptFile);

        Path applianceInventory = runtimeWorkspaceDir.resolve(
                TEST_CASE + "/01_extracted/windows-appliance/windows-appliance-inventory.xlsx");
        assertTrue(Files.isRegularFile(applianceInventory), "Inventory was not extracted: " + applianceInventory);

        Path outputInventory = runtimeWorkspaceDir.resolve(
                TEST_CASE + "/01_extracted/sumatrapdf/sumatrapdf-inventory.xlsx");
        Path expectedInventory = WORKBENCH_DIR.resolve("inventories/SumatraPDF-3.5.2-inventory.xlsx");
        assertTrue(Files.isRegularFile(outputInventory), "Inventory was not created: " + outputInventory);
        assertEquals(-1, Files.mismatch(expectedInventory, outputInventory),
                "Copied inventory differs from " + expectedInventory);
    }

    /**
     * Replaces the placeholders of the configuration template with absolute paths and writes the result to
     * {@code target/test-cases/<test case>/}. Only this copy is used by the test.
     */
    private static Path resolveConfigTemplate() throws IOException {
        String config = Files.readString(WORKBENCH_DIR.resolve(CONFIG_FILE_NAME));
        for (Map.Entry<String, Path> placeholder : PLACEHOLDERS.entrySet()) {
            config = config.replace("${" + placeholder.getKey() + "}", placeholder.getValue().toString());
        }

        Matcher unresolved = PLACEHOLDER.matcher(config);
        if (unresolved.find()) {
            fail("Unknown placeholder ${" + unresolved.group(1) + "} in " + CONFIG_FILE_NAME
                 + ", known placeholders: " + PLACEHOLDERS.keySet());
        }

        Files.createDirectories(RESOLVED_CONFIG_DIR);
        Path resolvedConfig = RESOLVED_CONFIG_DIR.resolve(CONFIG_FILE_NAME);
        Files.writeString(resolvedConfig, config);
        return resolvedConfig;
    }

    private String generateScript(Path configFile) {
        PipelineConfiguration configuration = new PipelineConfigurationLoader().readConfig(configFile.toFile());
        LocalConfiguration localConfiguration = LocalConfiguration.builder()
                .WORKBENCH_DIR(WORKBENCH_DIR.toString())
                .WORKSPACE_DIR(runtimeWorkspaceDir.toString())
                .KONTINUUM_DIR(KONTINUUM_DIR.toString())
                .MAVEN_CLI_OPTS("-q")
                .build();
        return new LocalPipeline(configuration, localConfiguration).generatePipeline();
    }

    private void runScript(Path scriptFile) throws Exception {
        Path logFile = runtimeWorkspaceDir.resolve(TEST_CASE + "-pipeline.log");
        Process process = new ProcessBuilder(List.of("bash", scriptFile.toString()))
                .directory(runtimeWorkspaceDir.toFile())
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
}
