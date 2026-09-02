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

                EnrichmentOptions enrichmentOptions = context.getConfiguration().getOptions().getEnrichment();
                MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(CREATE_DASHBOARD);
                processor.setStage(Stage.REPORT);
                PipelineConfiguration.ProjectProperties.Project project = context.getConfiguration()
                        .getProjectProperties()
                        .getProject();

                processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                        context.getStageDirForAsset(Stage.ADVISE).appendAssetInventory());
                processor.setProcessorParameter(OUTPUT_DASHBOARD_FILE,
                        context.getStageDirForAsset(Stage.REPORT).appendDashboardFile());
                processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE,
                        enrichmentOptions.getSecurityPolicyFile(context.getEnvironment().getWorkbenchDirNormalized()));
                processor.setProcessorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS,
                        enrichmentOptions.getSecurityPolicyActiveIds() != null
                                ? String.join(",",
                                context.getConfiguration().getOptions()
                                        .getEnrichment()
                                        .getSecurityPolicyActiveIds()) : null);
                processor.setProcessorParameter(PARAM_TENANT_ID,
                        project.getTenant());
                processor.setProcessorParameter(PARAM_ASSET_ID,
                        asset.getAssessmentId());
                processor.setProcessorParameter(PARAM_ASSESSMENT_CONTEXT,
                        asset.getContext());
                processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR,
                        context.getEnvironment().getMirrorDatabaseDirNormalized());

                context.addProcessor(processor);
            }
        }
    }

    private void handleReports(AssetExecutionContext context) {
        List<Report> reports = context.getConfiguration().getReports();
        if (reports == null || reports.isEmpty()) {
            return;
        }

        for (Report report : reports) {
            for (String assetId : report.getAssetIds()) {
                if (!assetId.equals(context.getAsset().getId())) {
                    continue;
                }

                List<String> types = report.getTypes();
                List<SupportedLocale> locales = report.getLocales();

                assert !locales.isEmpty();
                assert !types.isEmpty();


                boolean hasSda = false;
                for (SupportedLocale locale : locales) {
                    for (String type : types) {
                        handleReportGeneration(context, report, type, locale);

                        if (type.equals(ReportType.SOFTWARE_DISTRIBUTION_ANNEX.getKey())) {
                            hasSda = true;
                        }
                    }
                }

                if (hasSda) {
                    for (SupportedLocale locale : locales) {
                        handleLicenseAggregation(context, locale);
                        handleSourceAggregation(context, locale);
                        handleAnnexArchiveCreation(context, locale);
                    }
                }
            }
        }
    }

    private void handleReportGeneration(AssetExecutionContext context, Report report, String type, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(CREATE_DOCUMENT);
        processor.setStage(Stage.REPORT);
        ReportType reportType = ReportType.fromKey(type);
        Asset asset = context.getAsset();

        processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getGroupedStageForAsset(ReportType.fromKey(type), locale).toString());

        if (ReportType.fromKey(type).equals(ReportType.CERT_REPORT)) {
            processor.setProcessorParameter(PARAM_OVERVIEW_ADVISORS, "[\"CERT_FR\"]");
        } else {
            processor.setProcessorParameter(PARAM_OVERVIEW_ADVISORS,
                    report.getOverviewAdvisors() == null || report.getOverviewAdvisors().isEmpty()
                            ? null
                            : String.join(", ", report.getOverviewAdvisors()));
        }

        if (ReportType.requiresVulnerabilityEnrichment(reportType)) {
            processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE,
                    context.getConfiguration().getOptions().getEnrichment().getSecurityPolicyFile(context.getEnvironment().getWorkbenchDirNormalized()));
        }

        processor.setProcessorParameter(OUTPUT_DOCUMENT_FILE, context.getStageDirForAsset(Stage.REPORT).appendReportFile(ReportType.fromKey(type), locale));

        processor.setProcessorParameter(PARAM_COMPUTED_INVENTORY_DIR, context.getStageDirForAsset(Stage.REPORT) + "computed/");
        processor.setProcessorParameter(PARAM_DOCUMENT_TYPE, type);
        processor.setProcessorParameter(PARAM_DOCUMENT_LANGUAGE, locale.getLanguage());

        processor.setProcessorParameter(PARAM_ASSET_ID, asset.getId());
        processor.setProcessorParameter(PARAM_ASSET_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_ASSET_VERSION, asset.getVersion());

        processor.setProcessorParameter(PARAM_PRODUCT_NAME,
                context.getConfiguration().getProjectProperties().getProject().getName());
        processor.setProcessorParameter(PARAM_PRODUCT_VERSION,
                context.getConfiguration().getProjectProperties().getProject().getVersion());
        processor.setProcessorParameter(PARAM_PRODUCT_WATERMARK, report.getWatermark());
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

        context.addProcessor(processor);
    }

    private void handleLicenseAggregation(AssetExecutionContext context, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(AGGREGATE_LICENSES);
        processor.setStage(Stage.REPORT);
        Asset asset = context.getAsset();

        processor.setProcessorParameter(ENV_TMD_PASSWORD, context.getEnvironment().TMD_PASSWORD);
        processor.setProcessorParameter(ENV_TMD_USERKEYS_FILE, context.getEnvironment().TMD_USERKEYS_FILE);
        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getGroupedStageForAsset(ReportType.SOFTWARE_DISTRIBUTION_ANNEX, locale).appendAssetInventory());
        processor.setProcessorParameter(PARAM_REFERENCE_COMPONENT_PATH, "../components"); // FIXME: Why this path?
        processor.setProcessorParameter(PARAM_REFERENCE_LICENSE_PATH, "../licenses");  // FIXME: Why this path?

        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, asset.getReferenceDir(context.getEnvironment().getWorkbenchDirNormalized())); // FIXME: Why this path?
        processor.setProcessorParameter(PARAM_TARGET_COMPONENT_DIR, context.getWorkspace().getStageDirForAsset(asset, Stage.REPORT).toString() + "components/");
        processor.setProcessorParameter(PARAM_TARGET_LICENSE_DIR, context.getWorkspace().getStageDirForAsset(asset, Stage.REPORT).toString() + "licenses/");

        context.addProcessor(processor);
    }

    private void handleSourceAggregation(AssetExecutionContext context, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(AGGREGATE_SOURCES);
        processor.setStage(Stage.REPORT);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getGroupedStageForAsset(ReportType.SOFTWARE_DISTRIBUTION_ANNEX, locale).appendAssetInventory());
        processor.setProcessorParameter(OUTPUT_TARGET_DIR, context.getStageDirForAsset(Stage.REPORT).toString() + "sources/");
        processor.setProcessorParameter(PARAM_CONFIG_FILE, context.getEnvironment().getConfigDirNormalized() + "source-aggregation/config.yaml" );

        context.addProcessor(processor);
    }

    private void handleAnnexArchiveCreation(AssetExecutionContext context, SupportedLocale locale) {
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

        context.addProcessor(processor);
    }
}
