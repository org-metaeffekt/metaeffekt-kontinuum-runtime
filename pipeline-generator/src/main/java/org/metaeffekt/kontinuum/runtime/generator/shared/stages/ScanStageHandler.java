package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.SCAN_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#SCAN} stage.
 * Responsible for scanning licenses and copyright information
 * when license scanning is explicitly enabled.
 */
public class ScanStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.SCAN;
    }

    @Override
    public void process(AssetExecutionContext context) {
        boolean enableScan = context.getConfiguration().getOptions() != null
                && context.getConfiguration().getOptions().getGlobal() != null
                && Boolean.TRUE.equals(context.getConfiguration().getOptions().getGlobal().getEnableScan());

        if (enableScan) {
            MavenProcessor processor = handleLicenseScan(context);

            Processor previousProcessor = context.getLastProcessor();
            if (previousProcessor != null) {
                context.addDependency(processor, previousProcessor);
            }
            Processor extractProcessor = context.getLastProcessor(Stage.EXTRACT);
            if (extractProcessor != null) {
                context.addDependency(processor, extractProcessor);
            }

            context.addProcessor(processor);
        }
    }

    /**
     * Enriches the asset inventory with licensing and copyright information.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/scan/scan_scan-inventory.md">scan_scan-inventory.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for scanning the inventory.
     */
    private MavenProcessor handleLicenseScan(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(SCAN_INVENTORY);
        processor.setStage(Stage.SCAN);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                context.getStageDirForAsset(Stage.SCAN).appendAssetInventory());
        processor.setProcessorParameter(PARAM_PROPERTIES_FILE,
                context.getEnvironment().SCAN_PROPERTIES_FILE);
        processor.setProcessorParameter(ENV_KOSMOS_PASSWORD,
                context.getEnvironment().TMD_PASSWORD);
        processor.setProcessorParameter(ENV_KOSMOS_USERKEYS_FILE,
                context.getEnvironment().TMD_USERKEYS_FILE);

        try {
            processor.setProcessorParameter(INPUT_OUTPUT_ANALYSIS_BASE_DIR,
                    context.getStageDirForAsset(Stage.SCAN).appendLicenseAnalysisDir());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.SCAN).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.SCAN).toString());

        return processor;
    }
}