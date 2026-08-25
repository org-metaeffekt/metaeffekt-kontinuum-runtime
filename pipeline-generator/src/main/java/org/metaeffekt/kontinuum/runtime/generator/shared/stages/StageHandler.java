package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

/**
 * Interface for stage-specific pipeline processor generation.
 * Each implementation handles a single pipeline stage and appends
 * the appropriate processors to the execution context.
 */
public interface StageHandler {

    /**
     * Returns the stage this handler is responsible for.
     */
    Stage getStage();

    /**
     * Inspects the execution context and appends processors for this stage.
     * Implementations should update the context's current artifact paths
     * when they produce output artifacts consumed by downstream stages.
     */
    void process(AssetExecutionContext context);
}
