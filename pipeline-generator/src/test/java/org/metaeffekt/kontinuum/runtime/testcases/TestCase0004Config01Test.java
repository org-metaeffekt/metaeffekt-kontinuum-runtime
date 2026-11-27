package org.metaeffekt.kontinuum.runtime.testcases;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;
import org.metaeffekt.kontinuum.runtime.testcases.GeneratedPipeline.PipelineStep;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Test case 0004: four assets fetch the same already extracted inventory. Assets a and b configure an inventory name
 * on asset level, c and d use the default name. Assets b and d apply an extraction script, a and c only copy the
 * fetched inventory. The inventories of the extract and the prepare stage must be named as configured.
 */
@Slf4j
class TestCase0004Config01Test extends AbstractTestCase {

    private static final String COPY_INVENTORY_SCRIPT = "util/util_copy-inventory.sh";
    private static final String INPUT_INVENTORY = "input-inventory.xlsx";

    @Test
    void localPipeline_mapsInventoryNames() throws IOException {
        GeneratedPipeline pipeline = generateLocalPipeline();

        assertCopiedInventory(pipeline, "a", "my-a-inventory.xlsx");
        assertScriptedInventory(pipeline, "b", "my-b-inventory.xlsx");
        assertCopiedInventory(pipeline, "c", "c-inventory.xlsx");
        assertScriptedInventory(pipeline, "d", "d-inventory.xlsx");

        assertPreparedInventory(pipeline, "a", "my-a-inventory.xlsx");
        assertPreparedInventory(pipeline, "b", "my-b-inventory.xlsx");
        assertPreparedInventory(pipeline, "c", "c-inventory.xlsx");
        assertPreparedInventory(pipeline, "d", "d-inventory.xlsx");
    }

    @Test
    @Tag(ROUNDTRIP)
    void execution_producesNamedInventories() throws Exception {
        runLocalPipeline();

        Path inputInventory = getTestCaseWorkspaceDir().resolve("01_extracted").resolve(INPUT_INVENTORY);
        assertSameContent(inputInventory, stageFile(Stage.EXTRACT, "a", "my-a-inventory.xlsx"));
        assertFileExists(stageFile(Stage.EXTRACT, "b", "my-b-inventory.xlsx"));
        assertSameContent(inputInventory, stageFile(Stage.EXTRACT, "c", "c-inventory.xlsx"));
        assertFileExists(stageFile(Stage.EXTRACT, "d", "d-inventory.xlsx"));

        assertFileExists(stageFile(Stage.PREPARE, "a", "my-a-inventory.xlsx"));
        assertFileExists(stageFile(Stage.PREPARE, "b", "my-b-inventory.xlsx"));
        assertFileExists(stageFile(Stage.PREPARE, "c", "c-inventory.xlsx"));
        assertFileExists(stageFile(Stage.PREPARE, "d", "d-inventory.xlsx"));
    }

    private void assertCopiedInventory(GeneratedPipeline pipeline, String assetId, String fileName) {
        pipeline.step(Stage.EXTRACT, "copy-inventory", assetId).assertContains(COPY_INVENTORY_SCRIPT
                + " " + stageFile(Stage.FETCH, assetId, INPUT_INVENTORY)
                + " " + stageFile(Stage.EXTRACT, assetId, fileName));
    }

    private void assertScriptedInventory(GeneratedPipeline pipeline, String assetId, String fileName) {
        PipelineStep scriptStep = pipeline.step(Stage.EXTRACT, "execute-kotlin-script", assetId);
        scriptStep.assertParameter("input.kotlin.script.file", getTestCaseWorkbenchDir().resolve("scripts/dummy-script.kts"));
        scriptStep.assertParameter("output.inventory.file", stageFile(Stage.EXTRACT, assetId, fileName));
    }

    /**
     * Both steps of the prepare stage must write the inventory of the asset under its configured name.
     */
    private void assertPreparedInventory(GeneratedPipeline pipeline, String assetId, String fileName) {
        Path preparedInventory = stageFile(Stage.PREPARE, assetId, fileName);
        pipeline.step(Stage.PREPARE, "attach-metadata", assetId).assertParameter("output.inventory.file", preparedInventory);
        pipeline.step(Stage.PREPARE, "enrich-with-reference", assetId).assertParameter("output.inventory.file", preparedInventory);
    }
}
