package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;

/**
 * Handler invoked once per asset. Implementations should update the context's current artifact
 * paths when they produce output artifacts consumed by downstream stages.
 */
public interface AssetStageHandler extends StageHandler {

    @Override
    default StageScope getScope() {
        return StageScope.ASSET;
    }

    /**
     * Inspects the asset execution context and appends processors for this stage.
     */
    void process(AssetExecutionContext context);
}
