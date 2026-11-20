package org.metaeffekt.kontinuum.runtime.testcases;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
class TestCase0001Test extends AbstractTestCase {

    private static final String TEST_CASE = "tc-0001";
    private static final String CONFIG_FILE_NAME = "tc-0001-01-config.yaml";

    @BeforeEach
    void requireKontinuum() {
        assumeTrue(Files.isDirectory(getKontinuumDir().resolve("processors")),
                "Kontinuum checkout not found at [" + getKontinuumDir() + "].");
    }

    @BeforeAll
    static void cleanupResultsDir() {
        File file = resultBaseDir(TEST_CASE).toFile();
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

    @Test
    void generatedScriptContainsTheResolvedAbsolutePaths() throws IOException {
        Path scriptFile = getResultWorkingDir().resolve(TEST_CASE + "-pipeline.sh");
        String script = generateScript(resolveConfigTemplate(), scriptFile);

        assertParameter("param.asset.url", "'file:" + getTestCaseWorkspaceDir() + "/02_prepared/app/app-inventory.xlsx'", script);

        assertTrue(script.contains(
                "-Dinput.kotlin.script.file='" + getTestCaseWorkbenchDir() + "/scripts/select-inventory.kts'"), script);
        assertTrue(script.contains(
                "-Dparam.reference.inventory.dir='" + SHARED_WORKBENCH_DIR + "/inventories/reference-inventory/inventory/'"), script);
        assertTrue(script.contains(
                "\"inventoryPath\":\"" + getTestCaseWorkbenchDir() + "/inventories\""), script);
        assertTrue(script.contains(
                "\"artifactIdPattern\":\"a-.*\""), script);
        assertFalse(script.contains("# --- PRE:"), "Mirror step is not required for this test case:\n" + script);
    }

    @Test
    void copiesInventoryOfMatchingArtifact() throws Exception {
        Path scriptFile = getResultWorkingDir().resolve(TEST_CASE + "-pipeline.sh");
        scriptFile.toFile().getParentFile().mkdirs();
        generateScript(resolveConfigTemplate(), scriptFile);

        runScript(scriptFile);

        Path outputInventory = getResultWorkspaceDir().resolve(TEST_CASE + "/01_extracted/a/scripted/a-inventory.xlsx");
        Path expectedInventory = getTestCaseBaseDir().resolve("workbench/inventories/a-1.15-inventory.xlsx");
        assertTrue(Files.isRegularFile(outputInventory), "Inventory was not created: " + outputInventory);
        assertEquals(-1, Files.mismatch(expectedInventory, outputInventory),
                "Copied inventory differs from " + expectedInventory);
    }

    @Override
    protected String getTestCaseId() {
        return TEST_CASE;
    }

    @Override
    protected String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

}
