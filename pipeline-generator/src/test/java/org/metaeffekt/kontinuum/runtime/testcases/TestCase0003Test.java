package org.metaeffekt.kontinuum.runtime.testcases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
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
 * The appliance inventory is also split into the parts application, os and driver by an extraction script. The
 * appliance contains no drivers, so the driver part must produce an empty inventory instead of failing.
 * <p>
 * The script is executed from the workspace, not from the workbench, so paths must not work by accident.
 * <p>
 * Requires a Kontinuum checkout next to the runtime; skipped otherwise.
 */
@AbstractTestCase.TestCaseMetadata(id = "tc-0003",configFileName = "tc-0003-01-config.yaml")
class TestCase0003Test extends AbstractTestCase {

    @Test
    void generatedScriptExtractsWindowsArchiveAndSelectsInventory() throws IOException {
        Path scriptFile = getResultWorkingDir().resolve("pipeline.sh");
        String script = generateBashPipelineScript(resolveConfigTemplate(), scriptFile);

        assertParameter("param.asset.url", "'file:" + getTestCaseWorkbenchDir() + "/extractions/windows-extract.tar'", script);
        assertContains("extract/extract_windows-inventory.xml", script);
        assertParameter("param.exclude.pattern.file", "'" + getTestCaseWorkbenchDir() + "/configs/exclude-patterns.yaml'", script);
        assertParameter("input.kotlin.script.file", "'" + getTestCaseWorkbenchDir() + "/scripts/select-inventory.kts'", script);
        assertContains("\"artifactIdPattern\":\"SumatraPDF-.*\"", script);
        assertContains("\"inventoriesDir\":\"" + getTestCaseWorkbenchDir() + "/inventories\"", script);

        // Mirror step is not required for this test case
        assertNotContains("# --- PRE:", script);
    }

    @Test
    void selectsSumatraPdfInventoryByArtifactInAppliance() throws Exception {
        Path scriptFile = getResultWorkingDir().resolve("pipeline.sh");
        generateBashPipelineScript(resolveConfigTemplate(), scriptFile);

        runScript(scriptFile);

        Path applianceInventory = getResultProjectWorkspaceDir().resolve("01_extracted/windows-appliance/windows-appliance-inventory.xlsx");
        assertTrue(Files.isRegularFile(applianceInventory), "Inventory was not extracted: " + applianceInventory);

        // compare the extracted inventories with the provided inventory to see, whether the right inventory was copied
        Path outputInventory = getResultProjectWorkspaceDir().resolve("01_extracted/sumatrapdf/sumatrapdf-inventory.xlsx");
        Path expectedInventory = getTestCaseWorkbenchDir().resolve("inventories/SumatraPDF-3.5.2-inventory.xlsx");
        assertTrue(Files.isRegularFile(outputInventory), "Inventory was not created: " + outputInventory);
        assertEquals(-1, Files.mismatch(expectedInventory, outputInventory), "Copied inventory differs from " + expectedInventory);
    }

    @Test
    void createsEmptyDriverInventory() throws Exception {
        Path scriptFile = getResultWorkingDir().resolve("pipeline.sh");
        generateBashPipelineScript(resolveConfigTemplate(), scriptFile);

        runScript(scriptFile);

        Path driverInventory = getResultProjectWorkspaceDir().resolve("01_extracted/driver/driver-inventory.xlsx");
        assertTrue(Files.isRegularFile(driverInventory), "Inventory was not created: " + driverInventory);

        Inventory inventory = new InventoryReader().readInventory(driverInventory.toFile());
        assertTrue(inventory.getArtifacts().isEmpty(), "Inventory expected to be empty: " + driverInventory);
    }

}

