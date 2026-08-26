package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;

public class ExtractStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.EXTRACT;
    }

    @Override
    public void process(AssetExecutionContext context) {
        handleInventoryExtraction(context);
    }

    /**
     * Extracts an inventory from the fetched artifact.
     * @param context The asset execution context containing pipeline and asset information passes between all stages.
     */
    private void handleInventoryExtraction(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(DefaultProcessorCatalog.ProcessorIds.SCAN_DIRECTORY);
        mavenProcessor.setStage(Stage.EXTRACT);
        mavenProcessor.setProcessorParameter(ProcessorParameterKey.INPUT_EXTRACT_DIR, context.getCurrentInventoryDir());
        mavenProcessor.setProcessorParameter(ProcessorParameterKey.OUTPUT_INVENTORY_FILE, context.getWorkspace().getStageDirForAsset(asset, Stage.EXTRACT).appendAssetInventory());
        mavenProcessor.setProcessorParameter(ProcessorParameterKey.OUTPUT_SCAN_DIR, context.getCurrentInventoryPath() + "/scan");
        mavenProcessor.setProcessorParameter(ProcessorParameterKey.PARAM_REFERENCE_INVENTORY_DIR, asset.getReferenceDir(context.getEnvironment().getWorkbenchDirNormalized()));

        context.setCurrentInventoryDir(context.getWorkspace().getStageDirForAsset(context.getAsset(), Stage.EXTRACT).toString());
        context.setCurrentInventoryPath(context.getWorkspace().getStageDirForAsset(context.getAsset(), Stage.EXTRACT).appendAssetInventory());
        context.addProcessor(mavenProcessor);
    }
}
