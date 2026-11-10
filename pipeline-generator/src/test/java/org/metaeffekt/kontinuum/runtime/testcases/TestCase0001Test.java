package org.metaeffekt.kontinuum.runtime.testcases;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the extraction script of test case 0001 and checks that the inventory of the matching artifact is copied.
 * Requires a Kontinuum checkout next to the runtime; skipped otherwise.
 */
class TestCase0001Test {

    private static final Path TEST_CASES_DIR = Path.of("src/test/resources/test-cases").toAbsolutePath();

    private static final Path KONTINUUM_DIR = Path.of("../../metaeffekt-kontinuum").toAbsolutePath().normalize();

    @TempDir
    Path workspaceDir;

    @BeforeEach
    void requireKontinuum() {
        assumeTrue(Files.isDirectory(KONTINUUM_DIR.resolve("processors")),
                "Kontinuum checkout not found at " + KONTINUUM_DIR);
    }

    @Test
    void copiesInventoryOfMatchingArtifact() throws Exception {
        Asset app = new PipelineConfigurationLoader()
                .readConfig(TEST_CASES_DIR.resolve("tc-0001/workbench/tc-0001-01-config.yaml").toFile())
                .getProjectProperties().getAssets().get(0);
        Asset a = app.getAssets().get(0);

        Path inputInventory = TEST_CASES_DIR.resolve(app.getUrlResolver().getUrl().substring("file:".length()));
        Path outputInventory = workspaceDir.resolve(a.getExtractionScriptInventoryFileName());

        runExtractionScript(a, inputInventory, outputInventory);

        Path expectedInventory = TEST_CASES_DIR.resolve("tc-0001/workbench/inventories/a-1.15-inventory.xlsx");
        assertTrue(Files.isRegularFile(outputInventory), "Inventory was not created: " + outputInventory);
        assertEquals(-1, Files.mismatch(expectedInventory, outputInventory),
                "Copied inventory differs from " + expectedInventory);
    }

    private void runExtractionScript(Asset asset, Path inputInventory, Path outputInventory) throws Exception {
        List<String> command = List.of(
                "mvn", "-q",
                "-f", KONTINUUM_DIR.resolve("processors/util/util_execute-kotlin-script.xml").toString(),
                "process-resources",
                "-Dinput.kotlin.script.file=" + TEST_CASES_DIR.resolve(asset.getExtractionScript().getScript()),
                "-Dinput.inventory.file=" + inputInventory,
                "-Doutput.inventory.file=" + outputInventory,
                "-Dparam.asset.id=" + asset.getId(),
                "-Dparam.script.params=" + new ObjectMapper().writeValueAsString(asset.getExtractionScript().getParams()));

        Path logFile = workspaceDir.resolve("extraction-script.log");
        Process process = new ProcessBuilder(command)
                .directory(TEST_CASES_DIR.toFile())
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();

        assertEquals(0, process.waitFor(), "Execution of the extraction script failed:\n" + Files.readString(logFile));
    }
}
