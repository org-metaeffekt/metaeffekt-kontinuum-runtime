package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.Objects;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

public class AggregateStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.AGGREGATE;
    }

    @Override
    public void process(AssetExecutionContext context) {

        // Use portfolio manager inventory from previous stage
        if (Objects.nonNull(context.getConfiguration().getPortfolioManager())) {
            handleInventoryReferenceEnrichment(context, context.getPortfolioManagerReferenceInventoryDir());
            handleAssetFilter(context); // FIXME: Remove when enrich inventory is fixed and doesnt include reference inventory assets anymore
        } else {
            handleInventoryReferenceEnrichment(context, context.getAsset().getReferenceDir(context.getEnvironment().getWorkbenchDirNormalized()));
        }
    }

    private void handleInventoryReferenceEnrichment(AssetExecutionContext context, String referenceInventoryDir) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(ENRICH_WITH_REFERENCE);
        processor.setStage(Stage.AGGREGATE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, referenceInventoryDir);
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.AGGREGATE).appendAssetInventory());

        context.setCurrentInventoryPath(context.getStageDirForAsset(Stage.AGGREGATE).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.AGGREGATE).toString());
        context.addProcessor(processor);
    }

    // FIXME: This is currently necessary because the enrich-with-reference process is faulty.
    private void handleAssetFilter(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(EXECUTE_KOTLIN_SCRIPT);
        processor.setStage(Stage.AGGREGATE);

        processor.setProcessorParameter(INPUT_KOTLIN_SCRIPT_FILE, context.getEnvironment().getScriptsDirNormalized() + "inventory.asset.filter.kts");
        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(PARAM_ASSET_ID, context.getAsset().getId());

        context.setCurrentInventoryPath(context.getStageDirForAsset(Stage.AGGREGATE).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.AGGREGATE).toString());
        context.addProcessor(processor);
    }
}
