package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Getter;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.List;

/**
 * Execution context for a report group: one report entry combined with one report type.
 * A single report is generated per locale of the group, reading the grouped inventories that the
 * asset-scoped group stage copied for each locale.
 */
@Getter
public class ReportGroupExecutionContext extends AbstractExecutionContext {

    private final int reportIndex;
    private final PipelineConfiguration.Report report;
    private final ReportType reportType;
    private final List<Asset> assets;
    private final PipelineConfiguration configuration;
    private final EnvironmentConfiguration environment;
    private final Workspace workspace;
    private final ProcessorCatalog processorCatalog;

    public ReportGroupExecutionContext(int reportIndex,
                                       PipelineConfiguration.Report report,
                                       ReportType reportType,
                                       List<Asset> assets,
                                       PipelineConfiguration configuration,
                                       EnvironmentConfiguration environment,
                                       Workspace workspace,
                                       ProcessorCatalog processorCatalog) {
        this(new ProcessorExecutionPlan(), reportIndex, report, reportType, assets, configuration,
                environment, workspace, processorCatalog);
    }

    public ReportGroupExecutionContext(ProcessorExecutionPlan executionPlan,
                                       int reportIndex,
                                       PipelineConfiguration.Report report,
                                       ReportType reportType,
                                       List<Asset> assets,
                                       PipelineConfiguration configuration,
                                       EnvironmentConfiguration environment,
                                       Workspace workspace,
                                       ProcessorCatalog processorCatalog) {
        super(executionPlan);
        this.reportIndex = reportIndex;
        this.report = report;
        this.reportType = reportType;
        this.assets = assets;
        this.configuration = configuration;
        this.environment = environment;
        this.workspace = workspace;
        this.processorCatalog = processorCatalog;
    }

    @Override
    public String getName() {
        return getGroupId() + "-" + reportType.getKey();
    }

    public String getGroupId() {
        return report.getGroupId();
    }

    public void addPrerequisite(Processor prerequisite) {
        getExecutionPlan().addPrerequisite(this, prerequisite);
    }

    public List<Processor> getPrerequisites() {
        return getExecutionPlan().getPrerequisites(this);
    }

    /**
     * The directory containing the grouped inventories for the given locale.
     */
    public String getGroupedInventoryDir(SupportedLocale locale) {
        return workspace.getGroupedDir(report, null, reportType, locale).toString();
    }

    /**
     * The group-keyed report output directory ({@code 08_reported/<groupId>/}).
     */
    public Workspace.GroupPath getReportDir() {
        return workspace.getReportDir(report);
    }

    /**
     * Reference inventory directory of the group. Since a reference is configured per asset,
     * the first member asset is used as the representative for the group.
     */
    public String getReferenceInventoryDir() {
        if (assets == null || assets.isEmpty()) {
            throw new IllegalStateException("Report group " + getName() + " has no member assets to derive a reference inventory from.");
        }
        return assets.get(0).getReferenceDir(environment.getWorkbenchDirNormalized());
    }
}
