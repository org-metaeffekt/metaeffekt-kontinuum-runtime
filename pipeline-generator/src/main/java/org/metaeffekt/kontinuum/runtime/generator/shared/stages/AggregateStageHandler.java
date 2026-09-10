package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.Objects;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#AGGREGATE} stage.
 * Responsible for enriching the asset inventory with reference data downloaded from Portfolio Manager
 * and filtering duplicate asset entries when Portfolio Manager is configured.
 */
public class AggregateStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.AGGREGATE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        if (Objects.nonNull(context.getConfiguration().getPortfolioManager())) {
            MavenProcessor enrichment = handleInventoryReferenceEnrichment(context);
            MavenProcessor filter = handleAssetFilter(context);

            context.addSequential(enrichment, filter);
        }
    }

    /**
     * Enriches the asset inventory using the reference inventory downloaded from Portfolio Manager.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_enrich-with-reference.md">util_enrich-with-reference.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for reference inventory enrichment.
     */
    private MavenProcessor handleInventoryReferenceEnrichment(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(ENRICH_WITH_REFERENCE);
        processor.setStage(Stage.AGGREGATE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, context.getPortfolioManagerReferenceInventoryDir());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.AGGREGATE).appendAssetInventory());

        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.AGGREGATE).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.AGGREGATE).toString());

        return processor;
    }

    /**
     * Filters duplicate asset information from the inventory using a Kotlin script.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_execute-kotlin-script.md">util_execute-kotlin-script.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for filtering asset entries.
     */
    private MavenProcessor handleAssetFilter(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(EXECUTE_KOTLIN_SCRIPT);
        processor.setStage(Stage.AGGREGATE);

        processor.setProcessorParameter(INPUT_KOTLIN_SCRIPT_FILE, context.getEnvironment().getScriptsDirNormalized() + "inventory.asset.filter.kts");
        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(PARAM_ASSET_ID, context.getAsset().getId());

        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.AGGREGATE).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.AGGREGATE).toString());

        return processor;
    }
}
