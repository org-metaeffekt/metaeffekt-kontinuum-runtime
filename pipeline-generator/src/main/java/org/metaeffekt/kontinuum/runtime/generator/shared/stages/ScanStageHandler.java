package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.SCAN_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

public class ScanStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.SCAN;
    }

    @Override
    public void process(AssetExecutionContext context) {
        if (context.getConfiguration().requiresLicenseScan()) {
            handleLicenseScan(context);
        }

        context.setCurrentInventoryPath(context.getStageDirForAsset(Stage.SCAN).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.SCAN).toString());
    }

    public void handleLicenseScan(AssetExecutionContext context) {
        MavenProcessor processor = context.getProcessorCatalog().getProcessorById(SCAN_INVENTORY);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                context.getStageDirForAsset(Stage.SCAN).appendAssetInventory());
        processor.setProcessorParameter(PARAM_PROPERTIES_FILE,
                context.getEnvironment().SCAN_PROPERTIES_FILE);
        processor.setProcessorParameter(ENV_KOSMOS_PASSWORD,
                context.getEnvironment().KOSMOS_PASSWORD);
        processor.setProcessorParameter(ENV_KOSMOS_USERKEYS_FILE,
                context.getEnvironment().KOSMOS_USERKEYS_FILE);

        try {
            processor.setProcessorParameter(INPUT_OUTPUT_ANALYSIS_BASE_DIR,
                    context.getStageDirForAsset(Stage.SCAN).appendLicenseAnalysisDir());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        context.addProcessor(processor);
    }
}
