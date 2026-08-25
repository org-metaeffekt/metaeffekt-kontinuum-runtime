package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Dashboard;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Options.EnrichmentOptions;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Report;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ReportType;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;
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
                MavenProcessor processor = context.getProcessorCatalog().getProcessorById(CREATE_DASHBOARD);
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
                        context.getEnvironment().getMirrorDatabaseDir());

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
                if (types == null || types.isEmpty()) {
                    continue;
                }

                for (String type : types) {
                    if (type == null) {
                        continue;
                    }

                    addReportProcessor(context, report, type);
                }
            }
        }
    }

    private void addReportProcessor(AssetExecutionContext context, Report report, String type) {
        MavenProcessor processor = context.getProcessorCatalog().getProcessorById(CREATE_DOCUMENT);
        ReportType reportType = ReportType.fromKey(type);
        Asset asset = context.getAsset();

        if (ReportType.requiresScan(reportType)) {
            processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getStageDirForAsset(Stage.SCAN).toString());
        } else if (ReportType.requiresVulnerabilityEnrichment(reportType)) {
            processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getStageDirForAsset(Stage.ADVISE).toString());
        } else if (context.getPlan().isRequireResolve()) {
            processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getStageDirForAsset(Stage.RESOLVE).toString());
        } else if (context.getPlan().isRequireAggregation()) {
            processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getStageDirForAsset(Stage.AGGREGATE).toString());
        } else {
            processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getStageDirForAsset(Stage.PREPARE).toString());
        }

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

        processor.setProcessorParameter(OUTPUT_DOCUMENT_FILE, context.getStageDirForAsset(Stage.REPORT).appendReportFile(ReportType.fromKey(type)));

        processor.setProcessorParameter(PARAM_COMPUTED_INVENTORY_DIR,
                context.getStageDirForAsset(Stage.REPORT).toString());
        processor.setProcessorParameter(PARAM_DOCUMENT_TYPE, type);
        processor.setProcessorParameter(PARAM_DOCUMENT_LANGUAGE, report.getLanguage());

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
                context.getEnvironment().getMirrorDatabaseDir());

        context.addProcessor(processor);
    }
}
