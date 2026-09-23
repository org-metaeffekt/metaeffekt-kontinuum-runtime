package org.metaeffekt.kontinuum.runtime.generator.shared;

import lombok.Getter;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ReportGroupExecutionContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of pipeline generation. It exposes every execution context in a deterministic order:
 * pipeline-wide, then per asset, then per report group.
 */
@Getter
public class PipelineExecution {

    private final PipelineExecutionContext pipelineContext;
    private final Map<Asset, AssetExecutionContext> assetContexts;
    private final List<ReportGroupExecutionContext> reportGroupContexts;

    public PipelineExecution(PipelineExecutionContext pipelineContext,
                             Map<Asset, AssetExecutionContext> assetContexts,
                             List<ReportGroupExecutionContext> reportGroupContexts) {
        this.pipelineContext = pipelineContext;
        this.assetContexts = assetContexts;
        this.reportGroupContexts = reportGroupContexts;
    }

    /**
     * All execution contexts in generation order.
     */
    public List<ExecutionContext> getContexts() {
        List<ExecutionContext> contexts = new ArrayList<>();
        contexts.add(pipelineContext);
        contexts.addAll(assetContexts.values());
        contexts.addAll(reportGroupContexts);
        return contexts;
    }

    /**
     * Convenience accessor for the per-asset processors. Useful for tests.
     */
    public Map<Asset, List<Processor>> getAssetProcessorsMap() {
        Map<Asset, List<Processor>> map = new LinkedHashMap<>();
        for (Map.Entry<Asset, AssetExecutionContext> entry : assetContexts.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getProcessors());
        }
        return map;
    }
}
