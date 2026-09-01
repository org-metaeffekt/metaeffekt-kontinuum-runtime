package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.DOWNLOAD_INDEX;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

public class PreStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.PRE;
    }

    @Override
    public void process(AssetExecutionContext context) {

        if (context.getConfiguration().requiresVulnerabilityEnrichment()) {
            handleVulnerabilityIndexDownload(context);
        }
    }

    private void handleVulnerabilityIndexDownload(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(DOWNLOAD_INDEX);
        processor.setStage(Stage.PRE);

        processor.setProcessorParameter(PARAM_MIRROR_ARCHIVE_URL, context.getEnvironment().VULNERABILITY_MIRROR_URL);
        processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR, context.getEnvironment().getMirrorDir());
        context.addProcessor(processor);
    }
}
