package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

public class PostStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.POST;
    }

    @Override
    public void process(AssetExecutionContext context) {
        // Post stage is currently a no-op.
        // Reserved for future post-processing logic.
    }
}
