package org.metaeffekt.kontinuum.runtime.testcases;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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

}
