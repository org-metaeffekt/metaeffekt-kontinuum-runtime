package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.INVENTORY_TO_CYCLONEDX;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.INVENTORY_TO_SPDX;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.PARAM_DOCUMENT_ORGANIZATION_URL;

public class SummarizeStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.SUMMARIZE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        if (context.getConfiguration().getOptions().getGlobal().getEnableCycloneDxBom()) {
            handleInventoryToCycloneDxConversion(context);
        }

        if (context.getConfiguration().getOptions().getGlobal().getEnableSpdxBom()) {
            handleInventoryToSpdxConversion(context);
        }
    }

    private void handleInventoryToCycloneDxConversion(AssetExecutionContext context) {
        PipelineConfiguration.ProjectProperties.Asset asset = context.getAsset();
        ProcessorDefinitions.MavenProcessor processor = (ProcessorDefinitions.MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_CYCLONEDX);
        processor.setStage(Stage.SUMMARIZE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.SUMMARIZE).appendCycloneDxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        context.addProcessor(processor);
    }

    private void handleInventoryToSpdxConversion(AssetExecutionContext context) {
        PipelineConfiguration.ProjectProperties.Asset asset = context.getAsset();
        ProcessorDefinitions.MavenProcessor processor = (ProcessorDefinitions.MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_SPDX);
        processor.setStage(Stage.SUMMARIZE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.SUMMARIZE).appendSpdxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        context.addProcessor(processor);
    }
}
