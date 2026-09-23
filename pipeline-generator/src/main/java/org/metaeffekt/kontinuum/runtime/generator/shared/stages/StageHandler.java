package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

/**
 * Base type for stage-specific pipeline processor generation.
 * <p>
 * Implementations must additionally implement one of {@link PipelineStageHandler},
 * {@link AssetStageHandler} or {@link ReportStageHandler}, which fixes the
 * {@link StageScope} and the context type their process method receives.
 */
public interface StageHandler {

    /**
     * Returns the stage this handler is responsible for.
     */
    Stage getStage();

    /**
     * Returns the granularity at which this handler is invoked.
     */
    StageScope getScope();
}
