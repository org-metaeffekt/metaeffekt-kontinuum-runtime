package org.metaeffekt.kontinuum.runtime.testcases;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Roundtrip for test case 0004: four assets fetch the same already extracted inventory. Assets a and b configure an
 * inventory name on asset level, c and d use the default name. Assets b and d apply an extraction script, a and c
 * only copy the fetched inventory. The test checks that the generated pipeline names the inventories of the extract
 * and the prepare stage as configured and that the files are produced. See {@code test-cases/README.md} for the
 * layout and the placeholders of the templates.
 * <p>
 * Requires a Kontinuum checkout; skipped otherwise.
 */
@Slf4j
@AbstractTestCase.TestCaseMetadata(id = "tc-0004", configFileName = "tc-0004-01-config.yaml")
class TestCase0004Test extends AbstractTestCase {

    private static final String COPY_INVENTORY_SCRIPT = "util/util_copy-inventory.sh";

    private static final String EXTRACTED = "01_extracted";
    private static final String PREPARED = "02_prepared";

    @Test
    void mapsInventoryNames() throws IOException {
        Path scriptFile = getResultWorkingDir().resolve("pipeline.sh");
        String script = generateBashPipelineScript(resolveConfigTemplate(), scriptFile);

        assertContains(COPY_INVENTORY_SCRIPT + " " + fetchedInventory("a") + " " + inventory(EXTRACTED, "a", "my-a-inventory.xlsx"), script);
        assertParameter("output.inventory.file", "'" + inventory(EXTRACTED, "b", "my-b-inventory.xlsx") + "'", script);
        assertContains(COPY_INVENTORY_SCRIPT + " " + fetchedInventory("c") + " " + inventory(EXTRACTED, "c", "c-inventory.xlsx"), script);
        assertParameter("output.inventory.file", "'" + inventory(EXTRACTED, "d", "d-inventory.xlsx") + "'", script);

        assertParameter("output.inventory.file", "'" + inventory(PREPARED, "a", "my-a-inventory.xlsx") + "'", script);
        assertParameter("output.inventory.file", "'" + inventory(PREPARED, "b", "my-b-inventory.xlsx") + "'", script);
        assertParameter("output.inventory.file", "'" + inventory(PREPARED, "c", "c-inventory.xlsx") + "'", script);
        assertParameter("output.inventory.file", "'" + inventory(PREPARED, "d", "d-inventory.xlsx") + "'", script);

        assertParameter("input.kotlin.script.file", "'" + getTestCaseWorkbenchDir() + "/scripts/dummy-script.kts'", script);
    }

    @Test
    void producesNamedInventories() throws Exception {
        Path scriptFile = getResultWorkingDir().resolve("pipeline.sh");
        generateBashPipelineScript(resolveConfigTemplate(), scriptFile);

        runScript(scriptFile);

        Path inputInventory = getTestCaseWorkspaceDir().resolve("01_extracted/input-inventory.xlsx");
        assertCopied(inputInventory, inventory(EXTRACTED, "a", "my-a-inventory.xlsx"));
        assertProduced(inventory(EXTRACTED, "b", "my-b-inventory.xlsx"));
        assertCopied(inputInventory, inventory(EXTRACTED, "c", "c-inventory.xlsx"));
        assertProduced(inventory(EXTRACTED, "d", "d-inventory.xlsx"));

        assertProduced(inventory(PREPARED, "a", "my-a-inventory.xlsx"));
        assertProduced(inventory(PREPARED, "b", "my-b-inventory.xlsx"));
        assertProduced(inventory(PREPARED, "c", "c-inventory.xlsx"));
        assertProduced(inventory(PREPARED, "d", "d-inventory.xlsx"));
    }

    private Path fetchedInventory(String assetId) {
        return getResultProjectWorkspaceDir().resolve("00_fetched").resolve(assetId).resolve("input-inventory.xlsx");
    }

    private Path inventory(String stageDir, String assetId, String fileName) {
        return getResultProjectWorkspaceDir().resolve(stageDir).resolve(assetId).resolve(fileName);
    }

    private static void assertProduced(Path inventory) {
        assertTrue(Files.isRegularFile(inventory), "Inventory was not created: " + inventory);
    }

    private static void assertCopied(Path expectedInventory, Path inventory) throws IOException {
        assertProduced(inventory);
        assertEquals(-1, Files.mismatch(expectedInventory, inventory),
                "Copied inventory differs from " + expectedInventory + ".");
    }
}
