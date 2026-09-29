package org.metaeffekt.kontinuum.runtime.generator.shared;

import lombok.Getter;
import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The authoritative processor execution plan and result of pipeline generation. It exposes every
 * execution context in a deterministic order: pipeline-wide, then per asset, then per report group.
 */
@Getter
public class PipelineExecution extends ProcessorExecutionPlan {

    private PipelineExecutionContext pipelineContext;
    private Map<Asset, AssetExecutionContext> assetContexts = new LinkedHashMap<>();
    private List<ReportGroupExecutionContext> reportGroupContexts = new ArrayList<>();

    /**
     * Creates an empty pipeline execution.
     */
    public PipelineExecution() {
    }

    /**
     * Creates a pipeline execution from the given contexts.
     *
     * @param pipelineContext     the pipeline-wide execution context
     * @param assetContexts       the per-asset execution contexts
     * @param reportGroupContexts the per-report-group execution contexts
     */
    public PipelineExecution(PipelineExecutionContext pipelineContext,
                             Map<Asset, AssetExecutionContext> assetContexts,
                             List<ReportGroupExecutionContext> reportGroupContexts) {
        this.pipelineContext = pipelineContext;
        this.assetContexts = new LinkedHashMap<>(assetContexts);
        this.reportGroupContexts = new ArrayList<>(reportGroupContexts);
        importContexts();
    }

    void setExecutionContexts(PipelineExecutionContext pipelineContext,
                              Map<Asset, AssetExecutionContext> assetContexts,
                              List<ReportGroupExecutionContext> reportGroupContexts) {
        this.pipelineContext = pipelineContext;
        this.assetContexts = new LinkedHashMap<>(assetContexts);
        this.reportGroupContexts = new ArrayList<>(reportGroupContexts);
    }

    /**
     * All execution contexts in generation order.
     *
     * @return the execution contexts in generation order
     */
    public List<ExecutionContext> getContexts() {
        List<ExecutionContext> contexts = new ArrayList<>();
        if (pipelineContext != null) {
            contexts.add(pipelineContext);
        }
        contexts.addAll(assetContexts.values());
        contexts.addAll(reportGroupContexts);
        return contexts;
    }

    /**
     * Convenience accessor for the per-asset processors. Useful for tests.
     *
     * @return a map from each asset to its processors
     */
    public Map<Asset, List<Processor>> getAssetProcessorsMap() {
        Map<Asset, List<Processor>> map = new LinkedHashMap<>();
        for (Map.Entry<Asset, AssetExecutionContext> entry : assetContexts.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getProcessors());
        }
        return map;
    }

    private void importContexts() {
        List<ExecutionContext> contexts = getContexts();
        for (ExecutionContext context : contexts) {
            registerContext(context);
        }
        for (ExecutionContext context : contexts) {
            for (Processor processor : context.getProcessors()) {
                addProcessor(context, processor);
            }
        }
        for (ExecutionContext context : contexts) {
            for (Processor processor : context.getProcessors()) {
                addDependency(processor, context.getDependencies(processor).toArray(new Processor[0]));
            }
            if (context instanceof ReportGroupExecutionContext reportGroup) {
                for (Processor prerequisite : reportGroup.getPrerequisites()) {
                    addPrerequisite(reportGroup, prerequisite);
                }
            }
        }
    }
}
