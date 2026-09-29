package org.metaeffekt.kontinuum.runtime.models.shared;

/**
 * Builds workspace directory paths for a pipeline project, rooted at the configured workspace directory.
 */
public class Workspace {

    public final String WORKSPACE_DIR;
    public final String MAVEN_INDEX_DIR = "workspace/maven-index/";

    /**
     * Creates a workspace rooted at the configured workspace directory for the pipeline's project.
     *
     * @param pipelineConfiguration    the pipeline configuration providing project properties
     * @param environmentConfiguration the environment configuration providing the workspace directory
     */
    public Workspace(PipelineConfiguration pipelineConfiguration, EnvironmentConfiguration environmentConfiguration) {
        WORKSPACE_DIR =
                environmentConfiguration.getWorkspaceDirNormalized() +
                pipelineConfiguration.getProjectProperties().getProject() + "/";
    }

    /**
     * Returns the stage-scoped directory path for the given asset.
     *
     * @param asset the asset the directory belongs to
     * @param stage the stage whose directory is used
     * @return the stage directory path for the asset
     */
    public AssetPath getStageDirForAsset(PipelineConfiguration.ProjectProperties.Asset asset, Stage stage) {
        return new AssetPath(WORKSPACE_DIR + stage.getStageDirectory() + "/" + asset + "/", asset);
    }

    /**
     * Returns the grouped report directory path for the given asset, report type and locale.
     *
     * @param asset      the asset the directory belongs to
     * @param reportType the report type selecting the workspace folder
     * @param locale     the locale whose identifier is appended, or null for none
     * @return the grouped directory path for the asset
     */
    public AssetPath getGroupedDirForAsset(PipelineConfiguration.ProjectProperties.Asset asset, ReportType reportType,
                                           SupportedLocale locale) {
        String localeStr = locale != null ? locale.getIdentifier() : "";
        return new AssetPath(
                WORKSPACE_DIR + Stage.GROUP.getStageDirectory() + "/" + asset + "/" + reportType.getWorkspaceFolder()
                + "/" + localeStr + "/", asset);
    }

    /**
     * Returns the grouped directory path for the given report, asset, report type and locale.
     *
     * @param report     the report providing the group id, or null
     * @param asset      the asset the directory belongs to, or null
     * @param reportType the report type selecting the workspace folder
     * @param locale     the locale whose identifier is appended, or null for none
     * @return the grouped directory path
     */
    public AssetPath getGroupedDir(PipelineConfiguration.Report report,
                                   PipelineConfiguration.ProjectProperties.Asset asset, ReportType reportType,
                                   SupportedLocale locale) {
        String groupName = report != null ? report.getGroupId() : (asset != null ? asset.toString() : "default");
        String localeStr = locale != null ? locale.getIdentifier() : "";
        return new AssetPath(
                WORKSPACE_DIR + Stage.GROUP.getStageDirectory() + "/" + groupName + "/" +
                reportType.getWorkspaceFolder()
                + "/" + localeStr + "/", asset);
    }

    /**
     * Directory holding the pre-report-filtered inventory of a single asset for a report entry.
     * Kept inside the report group directory so that per-report filters do not collide.
     *
     * @param report the report providing the group id, or null
     * @param asset  the asset whose prepared inventory is addressed
     * @return the prepared directory path for the asset
     */
    public AssetPath getGroupedPreparedDir(PipelineConfiguration.Report report,
                                           PipelineConfiguration.ProjectProperties.Asset asset) {
        String groupName = report != null ? report.getGroupId() : (asset != null ? asset.toString() : "default");
        return new AssetPath(
                WORKSPACE_DIR + Stage.GROUP.getStageDirectory() + "/" + groupName + "/prepared/" + asset + "/", asset);
    }

    /**
     * Group-keyed report output directory. Reports are generated once per report group, so their
     * artifacts live under {@code 08_reported/<groupId>/} rather than per asset.
     *
     * @param report the report providing the group id, or null
     * @return the group-scoped report directory path
     */
    public GroupPath getReportDir(PipelineConfiguration.Report report) {
        String groupName = report != null ? report.getGroupId() : "default";
        return new GroupPath(WORKSPACE_DIR + Stage.REPORT.getStageDirectory() + "/" + groupName + "/", groupName);
    }

    /**
     * Root directory holding all report and dashboard outputs of the report stage.
     *
     * @return the report root directory path
     */
    public String getReportRootDir() {
        return WORKSPACE_DIR + Stage.REPORT.getStageDirectory() + "/";
    }

    /**
     * A path helper bound to a single asset, exposing the directories and files of asset-scoped artifacts.
     */
    public record AssetPath(String dir, PipelineConfiguration.ProjectProperties.Asset asset) {

        /**
         * Returns the path to the asset's inventory spreadsheet.
         *
         * @return the asset inventory file path
         */
        public String appendAssetInventory() {
            return dir + asset + ".xlsx";
        }

        /**
         * Returns the path to the asset's dashboards directory.
         *
         * @return the dashboard directory path
         */
        public String appendDashboardDir() {
            return dir + "dashboards/";
        }

        /**
         * Returns the path to the asset's dashboard HTML file.
         *
         * @return the dashboard file path
         */
        public String appendDashboardFile() {
            return appendDashboardDir() + asset + ".html";
        }

        /**
         * Returns the path to the asset's overview HTML file.
         *
         * @return the overview file path
         */
        public String appendOverviewFile() {
            return dir + asset + "-overview.html";
        }

        /**
         * Returns the path to the asset's report PDF for the given report type and locale.
         *
         * @param reportType the report type whose key is part of the file name
         * @param locale     the locale whose identifier is part of the file name
         * @return the report file path
         */
        public String appendReportFile(ReportType reportType, SupportedLocale locale) {
            return dir + asset + "-" + reportType.getKey() + "-" + locale.getIdentifier() + ".pdf";
        }

        /**
         * Returns the path to the asset's annex archive for the given locale.
         *
         * @param locale the locale whose identifier is part of the file name
         * @return the annex archive file path
         */
        public String appendAnnexArchiveFile(SupportedLocale locale) {
            return dir + asset + "-" + locale + "-annex-archive.zip";
        }

        /**
         * Returns the path to the asset's SPDX file in the given format.
         *
         * @param format the format, using XML for the XML suffix and anything else for JSON
         * @return the SPDX file path
         */
        public String appendSpdxFile(String format) {
            if (format.equals("XML")) {
                return dir + asset + "-spdx" + ".xml";
            }
            return dir + asset + "-spdx" + ".json";
        }

        /**
         * Returns the path to the asset's portfolio manager reference directory.
         *
         * @return the portfolio manager reference directory path
         */
        public String appendPortfolioManagerReferenceDir() {
            return dir + "portfolio-manager/";
        }

        /**
         * Returns the path to the asset's portfolio manager reference inventory.
         *
         * @return the portfolio manager reference inventory file path
         */
        public String appendPortfolioManagerReferenceInventory() {
            return appendPortfolioManagerReferenceDir() + asset + "-pm-reference.xlsx";
        }

        /**
         * Returns the path to the asset's license analysis directory.
         *
         * @return the license analysis directory path
         */
        public String appendLicenseAnalysisDir() throws IllegalAccessException {
            if (!dir.contains(Stage.SCAN.getStageDirectory())) {
                throw new IllegalAccessException(
                        "Method appendLicenseAnalysisDir() should only be called if the base directory" +
                        "of the encompassing AssetPath object describes the workspace 05_scanned stage.");
            }
            return dir + "analysis/";
        }

        /**
         * Returns the path to the asset's vulnerability enrichment temporary directory.
         *
         * @return the vulnerability enrichment temporary directory path
         */
        public String appendVulnerabilityEnrichmentTempDir() throws IllegalAccessException {
            if (!dir.contains(Stage.ADVISE.getStageDirectory())) {
                throw new IllegalAccessException(
                        "Method appendVulnerabilityEnrichmentTempDir() should only be called if the base directory" +
                        "of the encompassing AssetPath object describes the workspace 06_advised stage.");
            }
            return dir + "vulnerability-enrichment-temp/";
        }

        /**
         * Returns the path to the asset's CycloneDX file in the given format.
         *
         * @param format the format, using XML for the XML suffix and anything else for JSON
         * @return the CycloneDX file path
         */
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
     * single asset; the report identity is the group name (the report id, or a position-derived
     * {@code group-N} for entries without an id).
     */
    public record GroupPath(String dir, String groupName) {

        /**
         * Returns the path to the group's report PDF for the given report type and locale.
         *
         * @param reportType the report type whose key is part of the file name
         * @param locale     the locale whose identifier is part of the file name
         * @return the report file path
         */
        public String appendReportFile(ReportType reportType, SupportedLocale locale) {
            return dir + groupName + "-" + reportType.getKey() + "-" + locale.getIdentifier() + ".pdf";
        }

        /**
         * Returns the path to the group's annex archive for the given locale.
         *
         * @param locale the locale whose identifier is part of the file name
         * @return the annex archive file path
         */
        public String appendAnnexArchiveFile(SupportedLocale locale) {
            return dir + groupName + "-" + locale.getIdentifier() + "-annex-archive.zip";
        }

        /**
         * Returns the path to the group's computed directory.
         *
         * @return the computed directory path
         */
        public String appendComputedDir() {
            return dir + "computed/";
        }

        /**
         * Returns the path to the group's merged inventory spreadsheet.
         *
         * @return the merged inventory file path
         */
        public String appendMergedInventoryFile() {
            return dir + groupName + "-merged-inventory.xlsx";
        }

        /**
         * Returns the path to the group's sources directory.
         *
         * @return the sources directory path
         */
        public String appendSourcesDir() {
            return dir + "sources/";
        }

        /**
         * Returns the path to the group's components directory.
         *
         * @return the components directory path
         */
        public String appendComponentsDir() {
            return dir + "components/";
        }

        /**
         * Returns the path to the group's licenses directory.
         *
         * @return the licenses directory path
         */
        public String appendLicensesDir() {
            return dir + "licenses/";
        }

        /**
         * Returns the path to the group's source aggregation log file.
         *
         * @return the source aggregation log file path
         */
        public String appendSourceAggregationLog() {
            return dir + "source-aggregation.log";
        }

        @Override
        public String toString() {
            return dir;
        }
    }

}
