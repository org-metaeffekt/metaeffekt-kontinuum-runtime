package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.*;

import java.util.List;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.APPLY_BUSINESS_CASE;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.COPY_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

public class GroupStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.GROUP;
    }

    @Override
    public void process(AssetExecutionContext context) {
        List<PipelineConfiguration.Report> reports = context.getConfiguration().getReports();
        if (reports == null || reports.isEmpty()) {
            return;
        }

        for (PipelineConfiguration.Report report : reports) {
            for (String assetId : report.getAssetIds()) {
                if (!assetId.equals(context.getAsset().getId())) {
                    continue;
                }

                List<String> types = report.getTypes();
                List<SupportedLocale> locales = report.getLocales();

                assert !locales.isEmpty();
                assert !types.isEmpty();

                for (SupportedLocale locale : locales) {
                    for (String type : types) {

                        if (type.equals(ReportType.SOFTWARE_DISTRIBUTION_ANNEX.getKey())) {
                            handleApplyBusinessCase(context, locale);
                        } else {
                            handleInventoryCopy(context, type, locale);
                        }
                    }
                }
            }
        }
    }

    private void handleInventoryCopy(AssetExecutionContext context, String type, SupportedLocale locale) {
        ProcessorDefinitions.StandaloneProcessor standaloneProcessor = (ProcessorDefinitions.StandaloneProcessor) context.getProcessorCatalog().getProcessorById(COPY_INVENTORY);
        standaloneProcessor.setStage(Stage.GROUP);

        standaloneProcessor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        standaloneProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getGroupedStageForAsset(ReportType.fromKey(type), locale).appendAssetInventory());

        context.addProcessor(standaloneProcessor);
    }

    private void handleApplyBusinessCase(AssetExecutionContext context, SupportedLocale locale) {
        ProcessorDefinitions.MavenProcessor processor = (ProcessorDefinitions.MavenProcessor) context.getProcessorCatalog().getProcessorById(APPLY_BUSINESS_CASE);
        processor.setStage(Stage.REPORT);

        processor.setProcessorParameter(ENV_TMD_PASSWORD, context.getEnvironment().TMD_PASSWORD);
        processor.setProcessorParameter(ENV_TMD_USERKEYS_FILE,context.getEnvironment().TMD_USERKEYS_FILE);
        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getGroupedStageForAsset(ReportType.SOFTWARE_DISTRIBUTION_ANNEX, locale).appendAssetInventory());
        processor.setProcessorParameter(ENV_TMD_SOURCE, context.getEnvironment().TMD_SOURCE);
        processor.setProcessorParameter(PARAM_LANGUAGE_MODE, locale.getIdentifier());

        context.addProcessor(processor);

    }
}
