package org.metaeffekt.kontinuum.runtime.models.shared;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkspaceTest {

    private static Workspace workspace;
    private static PipelineConfiguration pipelineConfiguration;
    private static EnvironmentConfiguration environmentConfiguration;
    private static PipelineConfiguration.ProjectProperties.Asset asset;

    @BeforeAll
    public static void setup() {
        pipelineConfiguration = TestUtils.buildMinimalPipelineConfiguration();
        environmentConfiguration = TestUtils.buildMinimalLocalConfiguration();
        workspace = new Workspace(pipelineConfiguration, environmentConfiguration);

        asset = pipelineConfiguration
                .getProjectProperties()
                .getAssets()
                .get(0);
    }

    @Nested
    public class GetStageDirForAssetTest {

        @Test
        public void getPreStageDirForAsset() {
            assertEquals("./workspace/project-id/.pre/asset-id/", workspace.getStageDirForAsset(asset, Stage.PRE).toString());
        }

        @Test
        public void getFetchStageDirForAsset() {
            assertEquals("./workspace/project-id/00_fetched/asset-id/", workspace.getStageDirForAsset(asset, Stage.FETCH).toString());
        }

        @Test
        public void getExtractStageDirForAsset() {
            assertEquals("./workspace/project-id/01_extracted/asset-id/", workspace.getStageDirForAsset(asset, Stage.EXTRACT).toString());
        }

        @Test
        public void getPrepareStageDirForAsset() {
            assertEquals("./workspace/project-id/02_prepared/asset-id/", workspace.getStageDirForAsset(asset, Stage.PREPARE).toString());
        }

        @Test
        public void getAggregateStageDirForAsset() {
            assertEquals("./workspace/project-id/03_aggregated/asset-id/", workspace.getStageDirForAsset(asset, Stage.AGGREGATE).toString());
        }

        @Test
        public void getResolveStageDirForAsset() {
            assertEquals("./workspace/project-id/04_resolved/asset-id/", workspace.getStageDirForAsset(asset, Stage.RESOLVE).toString());
        }

        @Test
        public void getScanStageDirForAsset() {
            assertEquals("./workspace/project-id/05_scanned/asset-id/", workspace.getStageDirForAsset(asset, Stage.SCAN).toString());
        }

        @Test
        public void getAdviseStageDirForAsset() {
            assertEquals("./workspace/project-id/06_advised/asset-id/", workspace.getStageDirForAsset(asset, Stage.ADVISE).toString());
        }

        @Test
        public void getGroupStageDirForAsset() {
            assertEquals("./workspace/project-id/07_grouped/asset-id/", workspace.getStageDirForAsset(asset, Stage.GROUP).toString());
        }

        @Test
        public void getReportStageDirForAsset() {
            assertEquals("./workspace/project-id/08_reported/asset-id/", workspace.getStageDirForAsset(asset, Stage.REPORT).toString());
        }

        @Test
        public void getSummarizeStageDirForAsset() {
            assertEquals("./workspace/project-id/09_summarized/asset-id/", workspace.getStageDirForAsset(asset, Stage.SUMMARIZE).toString());
        }

        @Test
        public void getPostStageDirForAsset() {
            assertEquals("./workspace/project-id/.post/asset-id/", workspace.getStageDirForAsset(asset, Stage.POST).toString());
        }
    }

    @Nested
    public class GetGroupedDirTest {

    }
}
