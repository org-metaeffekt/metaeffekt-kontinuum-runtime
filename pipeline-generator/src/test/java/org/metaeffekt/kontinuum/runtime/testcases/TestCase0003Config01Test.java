package org.metaeffekt.kontinuum.runtime.testcases;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;
import org.metaeffekt.kontinuum.runtime.testcases.GeneratedPipeline.PipelineStep;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case 0003: a Windows appliance extraction archive is extracted, and the prepared inventory of the SumatraPDF
 * artifact found in the appliance is selected by an extraction script.
 * <p>
 * The appliance inventory is also split into the parts application, os and driver by an extraction script. The
 * appliance contains no drivers, so the driver part must produce an empty inventory instead of failing.
 */
@Slf4j
class TestCase0003Config01Test extends AbstractTestCase {

    private static final String PART_DRIVER = "driver";
    private static final List<String> PARTS = List.of("application", "os", PART_DRIVER);

    @Test
    void localPipeline_extractsWindowsArchive() throws IOException {
        GeneratedPipeline pipeline = generateLocalPipeline();

        pipeline.step(Stage.FETCH, "download-asset", "windows-appliance")
                .assertParameter("param.asset.url", "file:" + getTestCaseWorkbenchDir() + "/extractions/windows-extract.tar");

        PipelineStep extraction = pipeline.step(Stage.EXTRACT, "extract-windows-inventory", "windows-appliance");
        extraction.assertContains("extract/extract_windows-inventory.xml");
        extraction.assertParameter("param.exclude.pattern.file", getTestCaseWorkbenchDir().resolve("configs/exclude-patterns.yaml"));

        PipelineStep selection = pipeline.step(Stage.EXTRACT, "execute-kotlin-script", "sumatrapdf");
        selection.assertParameter("input.kotlin.script.file", getTestCaseWorkbenchDir().resolve("scripts/select-inventory.kts"));
        selection.assertContains("\"artifactIdPattern\":\"SumatraPDF-.*\"");
        selection.assertContains("\"inventoriesDir\":\"" + getTestCaseWorkbenchDir() + "/inventories\"");

        // Mirror step is not required for this test case
        pipeline.assertNoSteps(Stage.PRE);
    }

    @Test
    @Tag(ROUNDTRIP)
    void execution_selectsSumatraPdfInventory() throws Exception {
        runLocalPipeline();

        assertFileExists(stageFile(Stage.EXTRACT, "windows-appliance", "windows-appliance-inventory.xlsx"));

        // compare the extracted inventories with the provided inventory to see, whether the right inventory was copied
        assertSameContent(getTestCaseWorkbenchDir().resolve("inventories/SumatraPDF-3.5.2-inventory.xlsx"),
                stageFile(Stage.EXTRACT, "sumatrapdf", "sumatrapdf-inventory.xlsx"));
    }

    @Test
    @Tag(ROUNDTRIP)
    void execution_createsEmptyDriverInventory() throws Exception {
        runLocalPipeline();
        logPartInventorySizes();

        Path driverInventory = partInventory(PART_DRIVER);
        assertFileExists(driverInventory);

        Inventory inventory = new InventoryReader().readInventory(driverInventory.toFile());
        assertTrue(inventory.getArtifacts().isEmpty(), "Inventory expected to be empty: " + driverInventory);
    }

    private void logPartInventorySizes() throws IOException {
        for (String part : PARTS) {
            Path inventoryFile = partInventory(part);
            int size = new InventoryReader().readInventory(inventoryFile.toFile()).getArtifacts().size();
            log.info("Part inventory [{}] contains [{}] artifacts.", inventoryFile, size);
        }
    }

    private Path partInventory(String part) {
        return stageFile(Stage.EXTRACT, part, part + "-inventory.xlsx");
    }

}
