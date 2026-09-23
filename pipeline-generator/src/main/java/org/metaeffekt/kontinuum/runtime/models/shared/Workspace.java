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

    /**
     * Directory holding the pre-report-filtered inventory of a single asset for a report entry.
     * Kept inside the report group directory so that per-report filters do not collide.
     */
    public AssetPath getGroupedPreparedDir(PipelineConfiguration.Report report, PipelineConfiguration.ProjectProperties.Asset asset) {
        String groupName = report != null ? report.getGroupId() : (asset != null ? asset.toString() : "default");
        return new AssetPath(WORKSPACE_DIR + Stage.GROUP.getStageDirectory() + "/" + groupName + "/prepared/" + asset + "/", asset);
    }

    /**
     * Group-keyed report output directory. Reports are generated once per report group, so their
     * artifacts live under {@code 08_reported/<groupId>/} rather than per asset.
     */
    public GroupPath getReportDir(PipelineConfiguration.Report report) {
        String groupName = report != null ? report.getGroupId() : "default";
        return new GroupPath(WORKSPACE_DIR + Stage.REPORT.getStageDirectory() + "/" + groupName + "/", groupName);
    }

    /**
     * Root directory holding all report and dashboard outputs of the report stage.
     */
    public String getReportRootDir() {
        return WORKSPACE_DIR + Stage.REPORT.getStageDirectory() + "/";
    }

    public record AssetPath(String dir, PipelineConfiguration.ProjectProperties.Asset asset) {

        public String appendAssetInventory() {
            return dir + asset + ".xlsx";
        }

        public String appendDashboardDir() {
            return dir + "dashboards/";
        }

        public String appendDashboardFile() {
            return appendDashboardDir() + asset + ".html";
        }

        public String appendOverviewFile() {
            return dir + asset + "-overview.html";
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

    /**
     * Paths for group-scoped report artifacts. Unlike {@link AssetPath} these are not bound to a
     * single asset; the report identity is the group name (report id or joined asset ids).
     */
    public record GroupPath(String dir, String groupName) {

        public String appendReportFile(ReportType reportType, SupportedLocale locale) {
            return dir + groupName + "-" + reportType.getKey() + "-" + locale.getIdentifier() + ".pdf";
        }

        public String appendAnnexArchiveFile(SupportedLocale locale) {
            return dir + groupName + "-" + locale.getIdentifier() + "-annex-archive.zip";
        }

        public String appendComputedDir() {
            return dir + "computed/";
        }

        public String appendMergedInventoryFile() {
            return dir + groupName + "-merged-inventory.xlsx";
        }

        public String appendSourcesDir() {
            return dir + "sources/";
        }

        public String appendComponentsDir() {
            return dir + "components/";
        }

        public String appendLicensesDir() {
            return dir + "licenses/";
        }

        public String appendSourceAggregationLog() {
            return dir + "source-aggregation.log";
        }

        @Override
        public String toString() {
            return dir;
        }
    }

}
