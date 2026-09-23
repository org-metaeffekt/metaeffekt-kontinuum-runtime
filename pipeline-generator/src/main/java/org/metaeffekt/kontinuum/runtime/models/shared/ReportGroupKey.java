package org.metaeffekt.kontinuum.runtime.models.shared;

/**
 * Identifies a report group: a single report entry combined with one of its report types.
 * Document generation for the group's locales happens within one {@link ReportGroupExecutionContext}.
 *
 * @param reportIndex the position of the report entry in the pipeline configuration
 * @param reportType the report type to generate for the entry
 */
public record ReportGroupKey(int reportIndex, ReportType reportType) {
}
