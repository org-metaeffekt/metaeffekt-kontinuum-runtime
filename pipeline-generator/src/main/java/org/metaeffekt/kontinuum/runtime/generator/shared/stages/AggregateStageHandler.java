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
}
