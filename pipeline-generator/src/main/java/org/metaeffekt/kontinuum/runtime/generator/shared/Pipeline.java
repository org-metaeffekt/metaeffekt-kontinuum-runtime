package org.metaeffekt.kontinuum.runtime.generator.shared;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.generator.shared.stages.*;
import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Pipeline {

    private final PipelineConfiguration pipelineConfiguration;

    private final Workspace workspace;

    private final EnvironmentConfiguration environmentConfiguration;
    private final ProcessorCatalog processorCatalog = new DefaultProcessorCatalog();

    private final List<StageHandler> stageHandlers;

    public Pipeline(PipelineConfiguration pipelineConfiguration,
                    EnvironmentConfiguration environmentConfiguration) {

        new PipelineConfigurationLoader().validatePipelineConfigFile(pipelineConfiguration);

        this.environmentConfiguration = environmentConfiguration;
        this.pipelineConfiguration = pipelineConfiguration;
        this.workspace = new Workspace(pipelineConfiguration, environmentConfiguration);
        this.stageHandlers = List.of(
                new PreStageHandler(),
                new FetchStageHandler(),
                new ExtractStageHandler(),
                new PrepareStageHandler(),
                new AggregateStageHandler(),
                new ResolveStageHandler(),
                new ScanStageHandler(),
                new AdviseStageHandler(),
                new GroupStageHandler(),
                new ReportStageHandler(),
                new SummarizeStageHandler(),
                new PostStageHandler()
        );
    }

    public Map<Asset, List<Processor>> generatePipeline() {
        Map<Asset, List<Processor>> assetProcessorsMap = new LinkedHashMap<>();

        for (Asset asset : pipelineConfiguration.getProjectProperties().getAllAssets()) {
            AssetExecutionContext context = new AssetExecutionContext(
                    asset,
                    pipelineConfiguration,
                    environmentConfiguration,
                    workspace,
                    processorCatalog
            );

            for (StageHandler handler : stageHandlers) {
                handler.process(context);
            }

            appendPreScriptToProcessors(context);

            assetProcessorsMap.put(asset, context.getProcessors());
        }

        return assetProcessorsMap;
    }

    private void appendPreScriptToProcessors(AssetExecutionContext context) {
        if (StringUtils.isBlank(environmentConfiguration.SETUP_COMMAND)) {
            return;
        }

        for (Processor processor : context.getProcessors()) {
            String preScript = processor.getPreScript();

            if (StringUtils.isBlank(preScript)) {
                processor.setPreScript(environmentConfiguration.SETUP_COMMAND);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(environmentConfiguration.SETUP_COMMAND).append(System.lineSeparator()).append(preScript);
                processor.setPreScript(stringBuilder.toString());
            }
        }
    }

}
