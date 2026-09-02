package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Options.EnrichmentOptions;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.ENRICH_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

public class AdviseStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.ADVISE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        if (context.getConfiguration().requiresVulnerabilityEnrichment()) {
            handleVulnerabilityEnrichment(context);
        }
    }

    public void handleVulnerabilityEnrichment(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(ENRICH_INVENTORY);
        processor.setStage(Stage.ADVISE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                context.getStageDirForAsset(Stage.ADVISE).appendAssetInventory());
        processor.setProcessorParameter(PARAM_CORRELATION_DIR,
                context.getEnvironment().getCorrelationDirNormalized());

        EnrichmentOptions enrichment = context.getConfiguration().getOptions().getEnrichment();

        processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE,
                enrichment.getSecurityPolicyFile(context.getEnvironment().getWorkbenchDirNormalized()));
        processor.setProcessorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS,
                enrichment.getSecurityPolicyActiveIds() != null
                        ? String.join(",", enrichment.getSecurityPolicyActiveIds())
                        : null);

        processor.setProcessorParameter(PARAM_ACTIVATE_MSRC, String.valueOf(enrichment.getActivateMsrc()));
        processor.setProcessorParameter(PARAM_ACTIVATE_NVD, String.valueOf(enrichment.getActivateNvd()));
        processor.setProcessorParameter(PARAM_ACTIVATE_CERTFR, String.valueOf(enrichment.getActivateCertFr()));
        processor.setProcessorParameter(PARAM_ACTIVATE_CERTEU, String.valueOf(enrichment.getActivateCertEu()));
        processor.setProcessorParameter(PARAM_ACTIVATE_CERTSEI, String.valueOf(enrichment.getActivateCertSei()));
        processor.setProcessorParameter(PARAM_ACTIVATE_KEV, String.valueOf(enrichment.getActivateKev()));
        processor.setProcessorParameter(PARAM_ACTIVATE_EPSS, String.valueOf(enrichment.getActivateEpss()));
        processor.setProcessorParameter(PARAM_ACTIVATE_EOL, String.valueOf(enrichment.getActivateEol()));
        processor.setProcessorParameter(PARAM_ACTIVATE_OSV, String.valueOf(enrichment.getActivateOsv()));
        processor.setProcessorParameter(PARAM_ACTIVATE_CSAF, String.valueOf(enrichment.getActivateCsaf()));

        PipelineConfiguration.ProjectProperties.Project project = context.getConfiguration()
                .getProjectProperties().getProject();
        processor.setProcessorParameter(PARAM_ASSESSMENT_DIRS,
                asset.getAssessmentDir(project, context.getEnvironment().getWorkbenchDirNormalized()));
        processor.setProcessorParameter(PARAM_CONTEXT_DIRS,
                asset.getContextDir(project, context.getEnvironment().getWorkbenchDirNormalized()));

        processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR,
                context.getEnvironment().getMirrorDatabaseDirNormalized());

        try {
            processor.setProcessorParameter(OUTPUT_TMP_DIR, context.getStageDirForAsset(Stage.ADVISE).appendVulnerabilityEnrichmentTempDir());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.ADVISE).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.ADVISE).toString());
        context.addProcessor(processor);
    }
}
