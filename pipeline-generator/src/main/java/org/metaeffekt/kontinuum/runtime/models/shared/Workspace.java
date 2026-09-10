package org.metaeffekt.kontinuum.runtime.models.shared;

public class Workspace {

    public final String WORKSPACE_DIR;
    public final String MAVEN_INDEX_DIR = "workspace/maven-index/";

    public Workspace(PipelineConfiguration pipelineConfiguration, EnvironmentConfiguration environmentConfiguration) {
        WORKSPACE_DIR = environmentConfiguration.getWorkspaceDirNormalized() + pipelineConfiguration.getProjectProperties().getProject() + "/";
    }

    public AssetPath getStageDirForAsset(PipelineConfiguration.ProjectProperties.Asset asset, Stage stage) {
        return new AssetPath(WORKSPACE_DIR + stage.getStageDirectory() + "/" + asset + "/", asset);
    }

    public AssetPath getGroupedDirForAsset(PipelineConfiguration.ProjectProperties.Asset asset, ReportType reportType, SupportedLocale locale) {
        String localeStr = locale != null ? locale.getIdentifier() : "";
        return new AssetPath(WORKSPACE_DIR + Stage.GROUP.getStageDirectory() + "/" + asset + "/" + reportType.getWorkspaceFolder() + "/" + localeStr + "/", asset);
    }

    public AssetPath getGroupedDir(PipelineConfiguration.Report report, PipelineConfiguration.ProjectProperties.Asset asset, ReportType reportType, SupportedLocale locale) {
        String groupName = report != null ? report.getGroupId() : (asset != null ? asset.toString() : "default");
        String localeStr = locale != null ? locale.getIdentifier() : "";
        return new AssetPath(WORKSPACE_DIR + Stage.GROUP.getStageDirectory() + "/" + groupName + "/" + reportType.getWorkspaceFolder() + "/" + localeStr + "/", asset);
    }

    public record AssetPath(String dir, PipelineConfiguration.ProjectProperties.Asset asset) {

        public String appendAssetInventory() {
            return dir + asset + ".xlsx";
        }

        public String appendDashboardFile() {
            return dir + asset + ".html";
        }

        public String appendReportFile(ReportType reportType, SupportedLocale locale) { return dir + asset + "-" + reportType.getKey() + "-" + locale.getIdentifier() + ".pdf"; }

        public String appendAnnexArchiveFile(SupportedLocale locale) { return dir + asset + "-" + locale + "-annex-archive.zip"; }

        public String appendSpdxFile(String format) {
            if (format.equals("XML")) {
                return dir + asset + "-spdx" + ".xml";
            }
            return dir + asset + "-spdx" + ".json";
        }

        public String appendPortfolioManagerReferenceDir() {
            return dir + "portfolio-manager/";
        }

        public String appendPortfolioManagerReferenceInventory() {
            return appendPortfolioManagerReferenceDir() + asset + "-pm-reference.xlsx";
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
                return dir + asset + "-cyclonedx" + ".xml";
            }
            return dir + asset + "-cyclonedx" + ".json";
        }

        @Override
        public String toString() {
            return dir;
        }
    }

}
