package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

public class GroupStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.GROUP;
    }

    @Override
    public void process(AssetExecutionContext context) {
        // Group stage is currently a no-op.
        // Reserved for future grouping/aggregation logic.
    }
}
