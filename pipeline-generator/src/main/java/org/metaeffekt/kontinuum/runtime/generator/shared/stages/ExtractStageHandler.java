package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.ATTACH_METADATA;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.ENRICH_WITH_REFERENCE;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.SCAN_DIRECTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#EXTRACT} stage.
 * Responsible for extracting an inventory from fetched assets, attaching asset metadata,
 * and enriching the inventory with reference data.
 */
public class ExtractStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.EXTRACT;
    }

    @Override
    public void process(AssetExecutionContext context) {
        MavenProcessor inventoryExtraction = handleInventoryExtraction(context);
        MavenProcessor metadataAttachment = handleMetadataAttachment(context);
        MavenProcessor inventoryReferenceEnrichment = handleInventoryReferenceEnrichment(context);

        context.addSequential(
                inventoryExtraction,
                metadataAttachment,
                inventoryReferenceEnrichment
        );

        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.EXTRACT).toString());
        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());
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

        return mavenProcessor;
    }

    /**
     * Enriches the inventory using the configured reference inventory for the asset.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_enrich-with-reference.md">util_enrich-with-reference.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for reference inventory enrichment.
     */
    private MavenProcessor handleInventoryReferenceEnrichment(AssetExecutionContext context) {
        Asset asset = context.getAsset();

        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(ENRICH_WITH_REFERENCE);
        mavenProcessor.setStage(Stage.EXTRACT);
        mavenProcessor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());
        mavenProcessor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, asset.getReferenceDir(context.getEnvironment().getWorkbenchDirNormalized()));
        mavenProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());

        return mavenProcessor;
    }
}
