package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Dashboard;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Options.EnrichmentOptions;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Report;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

import java.util.List;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#REPORT} stage.
 * Responsible for generating vulnerability dashboards and reports.
 * Based on report type:
 * <ul>
 *   <li>Software Distribution Annex (SDA): source aggregation &rarr; SDA generation &rarr; license aggregation &rarr; annex archive creation.</li>
 *   <li>License Documentation (LD): source aggregation &rarr; LD generation &rarr; license aggregation.</li>
 *   <li>Initial License Documentation (ILD): ILD generation &rarr; license aggregation.</li>
 *   <li>Other report types (e.g. VR, VSR, CR, CA): report generation only.</li>
 * </ul>
 * Dashboard generation runs independently for each configured dashboard and asset without dependencies from this stage.
 */
public class ReportStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.REPORT;
    }

    @Override
    public void process(AssetExecutionContext context) {
        handleDashboards(context);
        handleReports(context);
    }

    private void handleDashboards(AssetExecutionContext context) {
        List<Dashboard> dashboards = context.getConfiguration().getDashboards();
        Asset asset = context.getAsset();
        if (dashboards == null || dashboards.isEmpty()) {
            return;
        }

        for (Dashboard dashboard : dashboards) {
            for (String assetId : dashboard.getAssetIds()) {
                if (!assetId.equals(asset.getId())) {
                    continue;
                }

                context.addProcessor(handleDashboard(context, dashboard));
            }
        }
    }

    private void handleReports(AssetExecutionContext context) {
        List<Report> reports = context.getConfiguration().getReports();
        if (reports == null || reports.isEmpty()) {
            return;
        }

        for (Report report : reports) {
            if (report.getAssetIds() == null || !report.getAssetIds().contains(context.getAsset().getId())) {
                continue;
            }

            List<String> types = report.getTypes();
            List<SupportedLocale> locales = report.getLocales();

            for (String type : types) {
                ReportType reportType = ReportType.fromKey(type);

                switch (reportType) {
                    case SOFTWARE_DISTRIBUTION_ANNEX -> {
                        // run source-aggregation -> sda generation -> license aggregation -> annex archive creation
                        MavenProcessor sourceAggregationProcessor = handleSourceAggregation(context);
                        context.addProcessor(sourceAggregationProcessor);

                        for (SupportedLocale locale : locales) {
                            MavenProcessor reportProcessor = handleReportGeneration(context, report, type, locale);
                            context.addDependency(reportProcessor, sourceAggregationProcessor);

                            context.addSequential(handleLicenseAggregation(context, report, reportType, locale),
                                    reportProcessor,
                                    handleAnnexArchiveCreation(context, locale));
                        }
                    }
                    case LICENSE_DOCUMENTATION -> {
                        // run source-aggregation -> LD generation -> license aggregation
                        MavenProcessor sourceAggregationProcessor = handleSourceAggregation(context);
                        context.addProcessor(sourceAggregationProcessor);
                        for (SupportedLocale locale : locales) {
                            MavenProcessor licenseAggregationProcessor = handleLicenseAggregation(context, report, reportType, locale);
                            context.addProcessor(licenseAggregationProcessor);

                            MavenProcessor reportProcessor = handleReportGeneration(context, report, type, locale);
                            context.addDependency(reportProcessor, licenseAggregationProcessor);
                            context.addDependency(reportProcessor, sourceAggregationProcessor);
                            context.addProcessor(reportProcessor);
                        }
                    }
                    case INITIAL_LICENSE_DOCUMENTATION -> {
                        // run ILD generation -> license aggregation
                        for (SupportedLocale locale : locales) {
                            context.addSequential(handleLicenseAggregation(context, report, reportType, locale),
                                    handleReportGeneration(context, report, type, locale));
                        }
                    }
                    default -> {
                        // For all other types only run report generation
                        for (SupportedLocale locale : locales) {
                            MavenProcessor reportProc = handleReportGeneration(context, report, type, locale);
                            context.addProcessor(reportProc);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a vulnerability dashboard for the asset based on the vulnerability assessment and security policy.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/advise/advise_create-dashboard.md">advise_create-dashboard.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for dashboard creation.
     */
    private MavenProcessor handleDashboard(AssetExecutionContext context, Dashboard dashboard) {
        EnrichmentOptions enrichmentOptions = context.getConfiguration().getOptions() != null
                ? context.getConfiguration().getOptions().getEnrichment()
                : null;
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(CREATE_DASHBOARD);
        processor.setStage(Stage.REPORT);
        Asset asset = context.getAsset();

        processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                context.getStageDirForAsset(Stage.ADVISE).appendAssetInventory());
        processor.setProcessorParameter(OUTPUT_DASHBOARD_FILE,
                context.getStageDirForAsset(Stage.REPORT).appendDashboardFile());
        if (enrichmentOptions != null) {
            processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE,
                    enrichmentOptions.getSecurityPolicyFile(context.getEnvironment().getWorkbenchDirNormalized()));
            processor.setProcessorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS,
                    enrichmentOptions.getSecurityPolicyActiveIds() != null
                            ? String.join(",", enrichmentOptions.getSecurityPolicyActiveIds())
                            : null);
        }
        processor.setProcessorParameter(PARAM_TENANT_ID,
                dashboard.getTenant());
        processor.setProcessorParameter(PARAM_ASSET_ID,
                asset.getAssessmentId());
        processor.setProcessorParameter(PARAM_ASSESSMENT_CONTEXT,
                asset.getContext());
        processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR,
                context.getEnvironment().getMirrorDatabaseDirNormalized());

        return processor;
    }

    /**
     * Generates a report document (PDF) for the specified report type and locale from the grouped inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/report/report_create-document.md">report_create-document.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @param report The report configuration.
     * @param type The document type identifier (e.g. VR, LD, SDA, CR).
     * @param locale The target locale for document generation.
     * @return The configured {@link MavenProcessor} for document generation.
     */
    private MavenProcessor handleReportGeneration(AssetExecutionContext context, Report report, String type, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(CREATE_DOCUMENT);
        processor.setStage(Stage.REPORT);
        ReportType reportType = ReportType.fromKey(type);
        Asset asset = context.getAsset();

        processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getGroupedStage(report, reportType, locale).toString());

        if (ReportType.fromKey(type).equals(ReportType.CERT_REPORT)) {
            processor.setProcessorParameter(PARAM_OVERVIEW_ADVISORS, "[\"CERT_FR\"]");
        } else {
            processor.setProcessorParameter(PARAM_OVERVIEW_ADVISORS,
                    report.getOverviewAdvisors() == null || report.getOverviewAdvisors().isEmpty()
                            ? null
                            : String.join(", ", report.getOverviewAdvisors()));
        }

        if (ReportType.requiresVulnerabilityEnrichment(reportType)) {
            if (context.getConfiguration().getOptions() != null
                    && context.getConfiguration().getOptions().getEnrichment() != null) {
                processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE,
                        context.getConfiguration().getOptions().getEnrichment().getSecurityPolicyFile(context.getEnvironment().getWorkbenchDirNormalized()));
            }
        }

        processor.setProcessorParameter(OUTPUT_DOCUMENT_FILE, context.getStageDirForAsset(Stage.REPORT).appendReportFile(ReportType.fromKey(type), locale));

        processor.setProcessorParameter(PARAM_COMPUTED_INVENTORY_DIR, context.getStageDirForAsset(Stage.REPORT) + "computed/");
        processor.setProcessorParameter(PARAM_DOCUMENT_TYPE, type);
        processor.setProcessorParameter(PARAM_DOCUMENT_LANGUAGE, locale.getLanguage());

        processor.setProcessorParameter(PARAM_ASSET_ID, asset.getId());
        processor.setProcessorParameter(PARAM_ASSET_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_ASSET_VERSION, asset.getVersion());

        processor.setProcessorParameter(PARAM_PRODUCT_NAME, report.getProductName());
        processor.setProcessorParameter(PARAM_PRODUCT_VERSION, report.getProductVersion());
        processor.setProcessorParameter(PARAM_PRODUCT_WATERMARK, report.getProductWatermark());
        processor.setProcessorParameter(PARAM_PROPERTY_SELECTOR_ORGANIZATION, report.getOrganization());
        processor.setProcessorParameter(PARAM_PROPERTY_SELECTOR_CLASSIFICATION, report.getClassificationRating());
        processor.setProcessorParameter(PARAM_PROPERTY_SELECTOR_CONTROL, report.getControlRating());
        processor.setProcessorParameter(PARAM_ASSET_DESCRIPTOR_FILE, KontinuumUtils.normalizeDir(context.getEnvironment().getDescriptorsDirNormalized(), reportType.getAssetDescriptorFile()));
        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR,
                asset.getReferenceDir(context.getEnvironment().getWorkbenchDirNormalized()));
        processor.setProcessorParameter(PARAM_REFERENCE_LICENSE_DIR, null);
        processor.setProcessorParameter(PARAM_REFERENCE_COMPONENT_DIR, null);
        processor.setProcessorParameter(ENV_KONTINUUM_DIR,
                context.getEnvironment().getKontinuumDirNormalized());
        processor.setProcessorParameter(ENV_KONTINUUM_PROCESSORS_DIR,
                context.getEnvironment().getKontinuumProcessorsDirNormalized());
        processor.setProcessorParameter(ENV_WORKBENCH_DIR,
                context.getEnvironment().getWorkbenchDirNormalized());
        processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR,
                context.getEnvironment().getMirrorDatabaseDirNormalized());

        return processor;
    }

    /**
     * Aggregates source packages and artifacts for components listed in the inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_aggregate-sources.md">util_aggregate-sources.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for source aggregation.
     */
    private MavenProcessor handleSourceAggregation(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(AGGREGATE_SOURCES);
        processor.setStage(Stage.REPORT);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                context.getCurrentInventoryFile() != null
                        ? context.getCurrentInventoryFile()
                        : context.getStageDirForAsset(Stage.AGGREGATE).appendAssetInventory());
        processor.setProcessorParameter(OUTPUT_TARGET_DIR, context.getStageDirForAsset(Stage.REPORT).toString() + "sources/");
        processor.setProcessorParameter(PARAM_CONFIG_FILE, context.getEnvironment().getConfigDirNormalized() + "source-aggregation/config.yaml");
        processor.setProcessorParameter(PARAM_PROTOCOL_FILE, context.getStageDirForAsset(Stage.REPORT).toString() + "sources/protocol.log");

        return processor;
    }

    /**
     * Aggregates licenses and component terms metadata from the database for the given grouped inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_aggregate-licenses.md">util_aggregate-licenses.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @param report The report configuration.
     * @param reportType The report type being aggregated.
     * @param locale The target locale for the grouped inventory.
     * @return The configured {@link MavenProcessor} for license aggregation.
     */
    private MavenProcessor handleLicenseAggregation(AssetExecutionContext context, Report report, ReportType reportType, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(AGGREGATE_LICENSES);
        processor.setStage(Stage.REPORT);
        Asset asset = context.getAsset();

        processor.setProcessorParameter(ENV_TMD_PASSWORD, context.getEnvironment().TMD_PASSWORD);
        processor.setProcessorParameter(ENV_TMD_USERKEYS_FILE, context.getEnvironment().TMD_USERKEYS_FILE);
        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getGroupedStage(report, reportType, locale).appendAssetInventory());
        processor.setProcessorParameter(PARAM_REFERENCE_COMPONENT_PATH, context.getEnvironment().getWorkbenchDirNormalized() + "components/");
        processor.setProcessorParameter(PARAM_REFERENCE_LICENSE_PATH, context.getEnvironment().getWorkbenchDirNormalized() + "licenses/");

        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, asset.getReferenceDir(context.getEnvironment().getWorkbenchDirNormalized()));
        processor.setProcessorParameter(PARAM_TARGET_COMPONENT_DIR, context.getWorkspace().getStageDirForAsset(asset, Stage.REPORT).toString() + "components/");
        processor.setProcessorParameter(PARAM_TARGET_LICENSE_DIR, context.getWorkspace().getStageDirForAsset(asset, Stage.REPORT).toString() + "licenses/");

        return processor;
    }

    /**
     * Creates the software distribution annex zip archive including document PDF, components, licenses, and sources.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/report/report_create-annex-archive.md">report_create-annex-archive.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @param locale The target locale for the annex archive.
     * @return The configured {@link MavenProcessor} for annex archive creation.
     */
    private MavenProcessor handleAnnexArchiveCreation(AssetExecutionContext context, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(CREATE_ANNEX_ARCHIVE);
        processor.setStage(Stage.REPORT);

        processor.setProcessorParameter(OUTPUT_ANNEX_ARCHIVE_FILE, context.getStageDirForAsset(Stage.REPORT).appendAnnexArchiveFile(locale));
        processor.setProcessorParameter(INPUT_INVENTORY_COMPONENTS_DIR, context.getStageDirForAsset(Stage.REPORT).toString() + "components/");
        processor.setProcessorParameter(INPUT_INVENTORY_LICENSES_DIR, context.getStageDirForAsset(Stage.REPORT).toString() + "licenses/");
        processor.setProcessorParameter(INPUT_INVENTORY_SOURCES_DIR, context.getStageDirForAsset(Stage.REPORT).toString() + "sources/");

        if (locale.equals(SupportedLocale.DE_DE)) {
            processor.setProcessorParameter(INPUT_DOCUMENT_DE_PDF_FILE, context.getStageDirForAsset(Stage.REPORT).appendReportFile(ReportType.SOFTWARE_DISTRIBUTION_ANNEX, locale));
        } else {
            processor.setProcessorParameter(INPUT_DOCUMENT_EN_PDF_FILE, context.getStageDirForAsset(Stage.REPORT).appendReportFile(ReportType.SOFTWARE_DISTRIBUTION_ANNEX, locale));
        }

        return processor;
    }
}
