package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Dashboard;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Options.EnrichmentOptions;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.List;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.CREATE_DASHBOARD;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#REPORT} stage.
 * Responsible for generating vulnerability dashboards. Unlike reports, dashboards are
 * asset-scoped: every asset listed in a dashboard entry receives its own dashboard.
 */
public class DashboardStageHandler implements AssetStageHandler {

    @Override
    public Stage getStage() {
        return Stage.REPORT;
    }

    @Override
    public void process(AssetExecutionContext context) {
        List<Dashboard> dashboards = context.getConfiguration().getDashboards();
        Asset asset = context.getAsset();
        if (dashboards == null || dashboards.isEmpty()) {
            return;
        }

        for (Dashboard dashboard : dashboards) {
            if (dashboard.getAssetIds() == null) {
                continue;
            }
            for (String assetId : dashboard.getAssetIds()) {
                if (asset.getId().equals(assetId)) {
                    context.addProcessor(handleDashboard(context, dashboard));
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
}
