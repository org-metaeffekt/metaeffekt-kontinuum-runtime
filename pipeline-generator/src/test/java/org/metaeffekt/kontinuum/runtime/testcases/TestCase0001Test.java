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
 * Roundtrip for test case 0001: the configuration template is resolved, loaded, the local pipeline script is
 * generated and executed, and the inventory selected by the extraction script is compared with the expected
 * inventory. See {@code test-cases/README.md} for the layout and the placeholders of the templates.
 * <p>
 * The script is executed from the workspace, not from the workbench, so paths must not work by accident.
 * <p>
 * Requires a Kontinuum checkout next to the runtime; skipped otherwise.
 */
@Slf4j
@AbstractTestCase.TestCaseMetadata(id = "tc-0001",configFileName = "tc-0001-01-config.yaml")
class TestCase0001Test extends AbstractTestCase {

    @Test
    void generatedScriptContainsTheResolvedAbsolutePaths() throws IOException {
        Path scriptFile = getResultWorkingDir().resolve("pipeline.sh");
        String script = generateBashPipelineScript(resolveConfigTemplate(), scriptFile);

        assertParameter("param.asset.url", "'file:" + getTestCaseWorkspaceDir() + "/02_prepared/app/app-inventory.xlsx'", script);
        assertParameter("input.kotlin.script.file", "'" + getTestCaseWorkbenchDir() + "/scripts/select-inventory.kts'", script);
        assertParameter("param.reference.inventory.dir", "'" + SHARED_WORKBENCH_DIR + "/inventories/reference-inventory/inventory/'", script);

        assertContains("\"inventoryPath\":\"" + getTestCaseWorkbenchDir() + "/inventories\"", script);
        assertContains("\"artifactIdPattern\":\"a-.*\"", script);

        // Mirror step is not required for this test case
        assertNotContains("# --- PRE:", script);
    }

    @Test
    void copiesInventoryOfMatchingArtifact() throws Exception {
        Path scriptFile = getResultWorkingDir().resolve("pipeline.sh");
        scriptFile.toFile().getParentFile().mkdirs();
        generateBashPipelineScript(resolveConfigTemplate(), scriptFile);

        runScript(scriptFile);

        Path outputInventory = getResultWorkspaceDir().resolve("tc-0001" + "/01_extracted/a/scripted/a-inventory.xlsx");
        Path expectedInventory = getTestCaseWorkbenchDir().resolve("inventories/a-1.15-inventory.xlsx");
        assertTrue(Files.isRegularFile(outputInventory), "Inventory was not created: " + outputInventory);
        assertEquals(-1, Files.mismatch(expectedInventory, outputInventory),
                "Copied inventory differs from " + expectedInventory + ".");
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
