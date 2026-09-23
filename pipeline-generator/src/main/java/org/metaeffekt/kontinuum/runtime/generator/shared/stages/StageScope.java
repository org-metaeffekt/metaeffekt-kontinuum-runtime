package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

/**
 * The granularity at which a stage handler is invoked.
 */
public enum StageScope {

    /**
     * Invoked exactly once per pipeline, independent of assets and reports.
     */
    PIPELINE,

    /**
     * Invoked once per asset in the project.
     */
    ASSET,

    /**
     * Invoked once per report group (a report entry combined with one of its report types).
     */
    REPORT_GROUP
}
