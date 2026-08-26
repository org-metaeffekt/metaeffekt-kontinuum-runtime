package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.Objects;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.RESOLVE_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

public class ResolveStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.RESOLVE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        if (context.getConfiguration().getOptions().getGlobal().getEnableResolve()) {
            handleResolve(context);
        }
    }

    private void handleResolve(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(RESOLVE_INVENTORY);
        processor.setStage(Stage.RESOLVE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                context.getStageDirForAsset(Stage.RESOLVE).appendAssetInventory());
        processor.setProcessorParameter(PARAM_ARTIFACT_RESOLVER_CONFIG_FILE,
                context.getEnvironment().ARTIFACT_RESOLVER_CONFIG_FILE);
        processor.setProcessorParameter(PARAM_ARTIFACT_RESOLVER_PROXY_FILE,
                context.getEnvironment().ARTIFACT_RESOLVER_PROXY_FILE);
        processor.setProcessorParameter(ENV_MAVEN_INDEX_DIR, context.getWorkspace().MAVEN_INDEX_DIR);

        context.setCurrentInventoryPath(context.getStageDirForAsset(Stage.RESOLVE).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.RESOLVE).toString());
        context.addProcessor(processor);
    }
}
