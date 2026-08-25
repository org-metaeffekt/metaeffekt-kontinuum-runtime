package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

public class SummarizeStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.SUMMARIZE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        // Summarize stage is currently a no-op.
        // Reserved for future cross-asset summarization logic.
    }
}
