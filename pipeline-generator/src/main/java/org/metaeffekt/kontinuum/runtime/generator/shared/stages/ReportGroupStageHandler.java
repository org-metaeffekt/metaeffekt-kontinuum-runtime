package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.ReportGroupExecutionContext;

/**
 * Handler invoked once per report group. Report documents are generated per locale of the group,
 * reading the grouped inventories produced by the asset-scoped group stage.
 */
public interface ReportGroupStageHandler extends StageHandler {

    @Override
    default StageScope getScope() {
        return StageScope.REPORT_GROUP;
    }

    /**
     * Inspects the report group context and appends processors for this stage.
     */
    void process(ReportGroupExecutionContext context);
}
