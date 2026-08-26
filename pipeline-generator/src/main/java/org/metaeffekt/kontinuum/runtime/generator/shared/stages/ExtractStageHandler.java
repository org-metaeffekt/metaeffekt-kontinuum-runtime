package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.ENRICH_WITH_REFERENCE;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.PARAM_METADATA_ASSET_NAME;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.PARAM_METADATA_ASSET_PATH;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.PARAM_METADATA_ASSET_TYPE;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.PARAM_METADATA_ASSET_VERSION;

public class ExtractStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.EXTRACT;
    }

    @Override
    public void process(AssetExecutionContext context) {
        handleInventoryExtraction(context);
        handleMetadataAttachment(context);
        handleInventoryReferenceEnrichment(context);
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

    private void handleMetadataAttachment(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(DefaultProcessorCatalog.ProcessorIds.ATTACH_METADATA);
        mavenProcessor.setStage(Stage.EXTRACT);
        mavenProcessor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        mavenProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        mavenProcessor.setProcessorParameter(PARAM_METADATA_ASSET_ID, asset.getId());
        mavenProcessor.setProcessorParameter(PARAM_METADATA_ASSET_NAME, asset.getName());

        context.setCurrentInventoryDir(context.getWorkspace().getStageDirForAsset(context.getAsset(), Stage.EXTRACT).toString());
        context.setCurrentInventoryPath(context.getWorkspace().getStageDirForAsset(context.getAsset(), Stage.EXTRACT).appendAssetInventory());
        context.addProcessor(mavenProcessor);
    }

    private void handleInventoryReferenceEnrichment(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(ENRICH_WITH_REFERENCE);
        processor.setStage(Stage.EXTRACT);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, asset.getReferenceDir(context.getEnvironment().getWorkbenchDirNormalized()));
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.AGGREGATE).appendAssetInventory());

        context.setCurrentInventoryPath(context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.EXTRACT).toString());
        context.addProcessor(processor);
    }
}
