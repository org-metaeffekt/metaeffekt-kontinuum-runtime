package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Options.EnrichmentOptions;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.ENRICH_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#ADVISE} stage.
 * Responsible for enriching the asset inventory with vulnerability information such as CVEs
 * and other vulnerability-centric information from external databases.
 */
public class AdviseStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.ADVISE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        if (context.getConfiguration().requiresVulnerabilityEnrichment(context.getAsset())) {
            MavenProcessor processor = handleVulnerabilityEnrichment(context);

            Processor previousProcessor = context.getLastProcessor();
            if (previousProcessor != null) {
                context.addDependency(processor, previousProcessor);
            }
            Processor extractProcessor = context.getLastProcessor(Stage.EXTRACT);
            if (extractProcessor != null) {
                context.addDependency(processor, extractProcessor);
            }
            Processor preProcessor = context.getLastProcessor(Stage.PRE);
            if (preProcessor != null) {
                context.addDependency(processor, preProcessor);
            }

            context.addProcessor(processor);
        }
    }

    /**
     * Enriches the asset inventory with vulnerability information from external databases.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/advise/advise_enrich-inventory.md">advise_enrich-inventory.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for enriching the inventory with vulnerability data.
     */
    private MavenProcessor handleVulnerabilityEnrichment(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(ENRICH_INVENTORY);
        processor.setStage(Stage.ADVISE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                context.getStageDirForAsset(Stage.ADVISE).appendAssetInventory());
        processor.setProcessorParameter(PARAM_CORRELATION_DIR,
                context.getEnvironment().getCorrelationDirNormalized());

        EnrichmentOptions enrichment = (context.getConfiguration().getOptions() != null
                && context.getConfiguration().getOptions().getEnrichment() != null)
                ? context.getConfiguration().getOptions().getEnrichment()
                : new EnrichmentOptions();

        if (StringUtils.isNotBlank(enrichment.getSecurityPolicyFile())) {
            processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE,
                    enrichment.getSecurityPolicyFile(context.getEnvironment().getWorkbenchDirNormalized()));
        }
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

        return processor;
    }
}
