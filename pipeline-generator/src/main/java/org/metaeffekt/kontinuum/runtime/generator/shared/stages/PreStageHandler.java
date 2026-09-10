package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.DOWNLOAD_INDEX;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#PRE} stage.
 * Responsible for preliminary setup tasks such as updating and downloading the vulnerability index.
 */
public class PreStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.PRE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        if (context.getConfiguration().requiresVulnerabilityEnrichment()) {
            context.addProcessor(handleVulnerabilityIndexDownload(context));
        }
    }

    /**
     * Downloads the vulnerability index if the mirror is not currently up to date.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/mirror/mirror_download-index.md">mirror_download-index.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for downloading the index.
     */
    private MavenProcessor handleVulnerabilityIndexDownload(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(DOWNLOAD_INDEX);
        processor.setStage(Stage.PRE);

        processor.setProcessorParameter(PARAM_MIRROR_ARCHIVE_URL, context.getEnvironment().VULNERABILITY_MIRROR_URL);
        processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR, context.getEnvironment().getMirrorDir());

        return processor;
    }
}
