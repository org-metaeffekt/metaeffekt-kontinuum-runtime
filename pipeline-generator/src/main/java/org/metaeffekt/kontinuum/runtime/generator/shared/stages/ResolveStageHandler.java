package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.RESOLVE_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#RESOLVE} stage.
 * Responsible for resolving dependencies and downloading additional artifact metadata
 * when resolve is explicitly enabled.
 */
public class ResolveStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.RESOLVE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        boolean enableResolve = context.getConfiguration().getOptions() != null
                && context.getConfiguration().getOptions().getGlobal() != null
                && Boolean.TRUE.equals(context.getConfiguration().getOptions().getGlobal().getEnableResolve());

        if (enableResolve) {
            MavenProcessor processor = handleResolve(context);

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
     * Downloads and aggregates artifact resolution information into the asset inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/resolve/resolve_resolve-inventory.md">resolve_resolve-inventory.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for resolving the inventory.
     */
    private MavenProcessor handleResolve(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(RESOLVE_INVENTORY);
        processor.setStage(Stage.RESOLVE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                context.getStageDirForAsset(Stage.RESOLVE).appendAssetInventory());
        processor.setProcessorParameter(PARAM_ARTIFACT_RESOLVER_CONFIG_FILE,
                context.getEnvironment().ARTIFACT_RESOLVER_CONFIG_FILE);
        processor.setProcessorParameter(PARAM_ARTIFACT_RESOLVER_PROXY_FILE,
                context.getEnvironment().ARTIFACT_RESOLVER_PROXY_FILE);
        processor.setProcessorParameter(ENV_MAVEN_INDEX_DIR, context.getWorkspace().MAVEN_INDEX_DIR);

        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.RESOLVE).appendAssetInventory());
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.RESOLVE).toString());

        return processor;
    }
}
