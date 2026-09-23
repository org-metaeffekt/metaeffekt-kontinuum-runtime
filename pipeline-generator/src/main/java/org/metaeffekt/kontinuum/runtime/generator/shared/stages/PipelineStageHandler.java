package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.PipelineExecutionContext;

/**
 * Handler invoked once per generated pipeline, independent of assets.
 */
public interface PipelineStageHandler extends StageHandler {

    @Override
    default StageScope getScope() {
        return StageScope.PIPELINE;
    }

    /**
     * Inspects the pipeline context and appends pipeline-wide processors for this stage.
     */
    void process(PipelineExecutionContext context);
}
