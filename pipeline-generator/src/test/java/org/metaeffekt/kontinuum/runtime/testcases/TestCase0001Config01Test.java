package org.metaeffekt.kontinuum.runtime.testcases;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;
import org.metaeffekt.kontinuum.runtime.testcases.GeneratedPipeline.PipelineStep;

import java.io.IOException;

/**
 * Test case 0001: the already prepared inventory of asset {@code app} is fetched, and the extraction script of the
 * nested asset {@code a} selects the inventory of the single artifact matching {@code a-.*} from the inventories of
 * the workbench.
 */
@Slf4j
class TestCase0001Config01Test extends AbstractTestCase {

    @Test
    void localPipeline_resolvesAbsolutePaths() throws IOException {
        GeneratedPipeline pipeline = generateLocalPipeline();

        pipeline.step(Stage.FETCH, "download-asset", "app")
                .assertParameter("param.asset.url", "file:" + getTestCaseWorkspaceDir().resolve("02_prepared/app/app-inventory.xlsx"));

        PipelineStep selection = pipeline.step(Stage.EXTRACT, "execute-kotlin-script", "a");
        selection.assertParameter("input.kotlin.script.file", getTestCaseWorkbenchDir().resolve("scripts/select-inventory.kts"));
        selection.assertContains("\"inventoriesDir\":\"" + getTestCaseWorkbenchDir() + "/inventories\"");
        selection.assertContains("\"artifactIdPattern\":\"a-.*\"");

        pipeline.step(Stage.PREPARE, "enrich-with-reference", "app")
                // the generator writes directories with a trailing slash
                .assertParameter("param.reference.inventory.dir", getSharedWorkbenchDir().resolve("inventories/reference-inventory/inventory") + "/");

        // Mirror step is not required for this test case
        pipeline.assertNoSteps(Stage.PRE);
    }

    @Test
    @Tag(ROUNDTRIP)
    void execution_copiesMatchingInventory() throws Exception {
        runLocalPipeline();

        assertSameContent(getTestCaseWorkbenchDir().resolve("inventories/a-1.15-inventory.xlsx"),
                stageFile(Stage.EXTRACT, "a", "a-inventory.xlsx"));
    }

}
