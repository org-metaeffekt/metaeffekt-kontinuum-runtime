package org.metaeffekt.kontinuum.runtime.models.shared;

import java.util.List;
import java.util.stream.Collectors;

import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;

public class Workspace {

    public final String WORKSPACE_DIR;
    public final String MAVEN_INDEX_DIR = "workspace/maven-index/";

    public Workspace(PipelineConfiguration pipelineConfiguration, EnvironmentConfiguration environmentConfiguration) {
        WORKSPACE_DIR = environmentConfiguration.getWorkspaceDirNormalized() + pipelineConfiguration.getProjectProperties().getProject() + "/";
    }

    public AssetPath getStageDirForAsset(PipelineConfiguration.ProjectProperties.Asset asset, Stage stage) {
        return new AssetPath(WORKSPACE_DIR + stage.getStageDirectory() + "/" + asset + "/", asset);
    }

    public record AssetPath(String dir, PipelineConfiguration.ProjectProperties.Asset assetName) {

        public String appendAssetInventory() {
            return dir + assetName + ".xlsx";
        }

        public String appendDashboardFile() {
            return dir + assetName + ".html";
        }

        public String appendReportFile(ReportType reportType) { return dir + assetName + "-" + reportType.getKey() + ".pdf"; }

        public String appendSpdxFile(String format) {
            if (format.equals("XML")) {
                return dir + assetName + "-spdx" + ".xml";
            }
            return dir + assetName + "-spdx" + ".json";
        }

        public String appendPortfolioManagerReferenceDir() {
            return dir + assetName + "portfolio-manager/";
        }

        public String appendPortfolioManagerReferenceInventory() {
            return appendPortfolioManagerReferenceDir() + assetName + "-pm-reference.xlsx";
        }

        public String appendLicenseAnalysisDir() throws IllegalAccessException {
            if (!dir.contains(Stage.SCAN.getStageDirectory())) {
                throw new IllegalAccessException("Method appendLicenseAnalysisDir() should only be called if the base directory" +
                        "of the encompassing AssetPath object describes the workspace 05_scanned stage.");
            }
            return dir + "analysis/";
        }

        public String appendVulnerabilityEnrichmentTempDir() throws IllegalAccessException {
            if (!dir.contains(Stage.ADVISE.getStageDirectory())) {
                throw new IllegalAccessException("Method appendVulnerabilityEnrichmentTempDir() should only be called if the base directory" +
                        "of the encompassing AssetPath object describes the workspace 06_advised stage.");
            }
            return dir + "vulnerability-enrichment-temp/";
        }

        public String appendCycloneDxFile(String format) {
            if (format.equals("XML")) {
                return dir + assetName + "-cyclonedx" + ".xml";
            }
            return dir + assetName + "-cyclonedx" + ".json";
        }

        @Override
        public String toString() {
            return dir;
        }
    }

}
