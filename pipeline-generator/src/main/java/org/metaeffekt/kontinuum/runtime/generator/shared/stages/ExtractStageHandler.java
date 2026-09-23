package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.ATTACH_METADATA;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.SCAN_DIRECTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#EXTRACT} stage.
 * Responsible for extracting an inventory from fetched assets and attaching asset metadata.
 * Assets whose url resolver already references an extracted inventory (a local {@code .xls}/{@code .xlsx}
 * file) skip this stage, since the fetched file already is an inventory.
 */
public class ExtractStageHandler implements AssetStageHandler {

    @Override
    public Stage getStage() {
        return Stage.EXTRACT;
    }

    @Override
    public void process(AssetExecutionContext context) {
        if (context.getAsset().isPreExtractedInventory()) {
            return;
        }

        MavenProcessor inventoryExtraction = handleInventoryExtraction(context);
        MavenProcessor metadataAttachment = handleMetadataAttachment(context);

        context.addSequential(
                inventoryExtraction,
                metadataAttachment
        );
    }

    /**
     * Extracts an inventory from the fetched artifact directory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/prepare/prepare_scan-directory.md">prepare_scan-directory.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for scanning and extracting the inventory.
     */
    private MavenProcessor handleInventoryExtraction(AssetExecutionContext context) {
        Asset asset = context.getAsset();

        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(SCAN_DIRECTORY);
        mavenProcessor.setStage(Stage.EXTRACT);
        mavenProcessor.setProcessorParameter(INPUT_EXTRACT_DIR, context.getStageDirForAsset(Stage.FETCH).toString());
        mavenProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());
        mavenProcessor.setProcessorParameter(OUTPUT_SCAN_DIR, context.getStageDirForAsset(Stage.EXTRACT) + "scan/");
        mavenProcessor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, asset.getReferenceDir(context.getEnvironment().getWorkbenchDirNormalized()));

        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.EXTRACT).toString());
        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());

        return mavenProcessor;
    }

    /**
     * Attaches asset metadata (such as asset ID and name) to the extracted inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/advise/advise_attach-metadata.md">advise_attach-metadata.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for attaching metadata.
     */
    private MavenProcessor handleMetadataAttachment(AssetExecutionContext context) {
        Asset asset = context.getAsset();

        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(ATTACH_METADATA);
        mavenProcessor.setStage(Stage.EXTRACT);
        mavenProcessor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());
        mavenProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());
        mavenProcessor.setProcessorParameter(PARAM_METADATA_ASSET_ID, asset.getId());
        mavenProcessor.setProcessorParameter(PARAM_METADATA_ASSET_NAME, asset.getName());

        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.EXTRACT).toString());
        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());

        return mavenProcessor;
    }
}
