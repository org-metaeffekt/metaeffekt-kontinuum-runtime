package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.INVENTORY_TO_CYCLONEDX;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.INVENTORY_TO_SPDX;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#SUMMARIZE} stage.
 * Responsible for creating CycloneDX and SPDX documents in the summarize stage
 * if enabled in the pipeline configuration.
 */
public class SummarizeStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.SUMMARIZE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        PipelineConfiguration.Options.GlobalOptions globalOptions = context.getConfiguration().getOptions().getGlobal();
        assert globalOptions != null;

        if (globalOptions.getEnableCycloneDxBom()) {
            context.addProcessor(handleInventoryToCycloneDxConversion(context));
        }

        if (globalOptions.getEnableSpdxBom()) {
            context.addProcessor(handleInventoryToSpdxConversion(context));
        }
    }

    /**
     * Creates a CycloneDX BOM from the inventory in the summarize stage.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/convert/convert_inventory-to-cyclonedx.md">convert_inventory-to-cyclonedx.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for CycloneDX BOM conversion.
     */
    private MavenProcessor handleInventoryToCycloneDxConversion(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_CYCLONEDX);
        processor.setStage(Stage.SUMMARIZE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.SUMMARIZE).appendCycloneDxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        return processor;
    }

    /**
     * Creates an SPDX BOM from the inventory in the summarize stage.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/convert/convert_inventory-to-spdx.md">convert_inventory-to-spdx.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for SPDX BOM conversion.
     */
    private MavenProcessor handleInventoryToSpdxConversion(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_SPDX);
        processor.setStage(Stage.SUMMARIZE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.SUMMARIZE).appendSpdxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        return processor;
    }
}
