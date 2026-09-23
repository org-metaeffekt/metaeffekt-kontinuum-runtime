package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.StandaloneProcessor;

import java.util.List;
import java.util.Map;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#GROUP} stage.
 * Consolidates asset inventories into the group subdirectories of every report entry the asset
 * belongs to. A report entry defines a collection of assets, so only assets whose id is listed in
 * {@link PipelineConfiguration.Report#getAssetIds()} contribute to a group. If a report defines a
 * pre-report filter, it is applied to each member asset's inventory before it is copied.
 * For software distribution annex (SDA), the inventory is enriched with license notices
 * and business case information via the apply business case processor.
 * <p>
 * The copied and enriched inventories are registered as prerequisites of the corresponding
 * {@link ReportGroupExecutionContext} so that report generation waits for all members.
 */
public class GroupStageHandler implements AssetStageHandler {

    private final Map<ReportGroupKey, ReportGroupExecutionContext> reportGroupContexts;

    public GroupStageHandler(Map<ReportGroupKey, ReportGroupExecutionContext> reportGroupContexts) {
        this.reportGroupContexts = reportGroupContexts;
    }

    @Override
    public Stage getStage() {
        return Stage.GROUP;
    }

    @Override
    public void process(AssetExecutionContext context) {
        List<PipelineConfiguration.Report> reports = context.getConfiguration().getReports();
        if (reports == null || reports.isEmpty()) {
            return;
        }

        String assetId = context.getAsset().getId();

        for (int reportIndex = 0; reportIndex < reports.size(); reportIndex++) {
            PipelineConfiguration.Report report = reports.get(reportIndex);
            if (report == null || report.getAssetIds() == null || !report.getAssetIds().contains(assetId)) {
                continue;
            }

            String inventoryFile = context.getCurrentInventoryFile();

            MavenProcessor preReportFilter = null;
            if (StringUtils.isNotBlank(report.getPreReportFilterFile())) {
                preReportFilter = handlePreReportInventoryFiler(context, report);
                context.addProcessor(preReportFilter);
                inventoryFile = context.getWorkspace().getGroupedPreparedDir(report, context.getAsset()).appendAssetInventory();
            }

            for (SupportedLocale locale : report.getLocales()) {
                for (String type : report.getTypes()) {
                    ReportType reportType = ReportType.fromKey(type);

                    StandaloneProcessor copyProcessor = handleInventoryCopy(context, report, reportType, locale, inventoryFile);
                    if (preReportFilter != null) {
                        context.addDependency(copyProcessor, preReportFilter);
                    }
                    context.addProcessor(copyProcessor);
                    registerGroupPrerequisite(reportIndex, reportType, copyProcessor);

                    if (ReportType.requiresScan(reportType)) {
                        MavenProcessor businessCaseProcessor = handleApplyBusinessCase(context, report, reportType, locale);
                        context.addDependency(businessCaseProcessor, copyProcessor);
                        context.addProcessor(businessCaseProcessor);
                        registerGroupPrerequisite(reportIndex, reportType, businessCaseProcessor);
                    }
                }
            }
        }
    }

    private void registerGroupPrerequisite(int reportIndex, ReportType reportType, Processor processor) {
        ReportGroupExecutionContext groupContext = reportGroupContexts.get(new ReportGroupKey(reportIndex, reportType));
        if (groupContext != null) {
            groupContext.addPrerequisite(processor);
        }
    }

    /**
     * Filters the asset's inventory for a single report entry using the report's configured kotlin
     * script. The filtered inventory is written into the report group's prepared directory and
     * becomes the input of all subsequent inventory copies for that report.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_transform-inventories.md">util_transform-inventories.md</a>
     */
    private MavenProcessor handlePreReportInventoryFiler(AssetExecutionContext context, PipelineConfiguration.Report report) {
        Asset asset = context.getAsset();
        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(TRANSFORM_INVENTORIES);
        mavenProcessor.setStage(Stage.GROUP);

        Workspace.AssetPath preparedDir = context.getWorkspace().getGroupedPreparedDir(report, asset);
        mavenProcessor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getCurrentInventoryFile());
        mavenProcessor.setProcessorParameter(OUTPUT_INVENTORY_DIR, preparedDir.appendAssetInventory());
        mavenProcessor.setProcessorParameter(PARAM_KOTLIN_SCRIPT_FILE, context.getEnvironment().getScriptsDirNormalized() + "prepare.kts");
        mavenProcessor.setProcessorParameter(PARAM_ASSET_NAME, asset.getName());

        return mavenProcessor;
    }

    /**
     * Copies the asset inventory into the grouped report subdirectory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_copy-inventory.sh">util_copy-inventory.sh</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @param report The report configuration defining the grouped target.
     * @param reportType The report type being grouped.
     * @param locale The target locale for the grouped report.
     * @param inputInventoryFile The inventory to copy (the filtered inventory if a pre-report filter is active).
     * @return The configured {@link StandaloneProcessor} for copying the inventory.
     */
    private StandaloneProcessor handleInventoryCopy(AssetExecutionContext context, PipelineConfiguration.Report report, ReportType reportType, SupportedLocale locale, String inputInventoryFile) {
        StandaloneProcessor standaloneProcessor = (StandaloneProcessor) context.getProcessorCatalog().getProcessorById(COPY_INVENTORY);
        standaloneProcessor.setStage(Stage.GROUP);

        standaloneProcessor.setProcessorParameter(INPUT_INVENTORY_FILE, inputInventoryFile);
        standaloneProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                context.getGroupedStage(report, reportType, locale).appendAssetInventory());

        // Setting the output inventory as the new "current" inventory is omitted here on purpose.

        return standaloneProcessor;
    }

    /**
     * Applies business case specific changes to the inventory for all license reports.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_apply-business-case.md">util_apply-business-case.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @param report The report configuration defining the grouped target.
     * @param reportType The report type being grouped.
     * @param locale The target locale for the software distribution annex.
     * @return The configured {@link MavenProcessor} for applying the business case.
     */
    private MavenProcessor handleApplyBusinessCase(AssetExecutionContext context, PipelineConfiguration.Report report, ReportType reportType, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(APPLY_BUSINESS_CASE);
        processor.setStage(Stage.GROUP);

        processor.setProcessorParameter(ENV_TMD_PASSWORD, context.getEnvironment().TMD_PASSWORD);
        processor.setProcessorParameter(ENV_TMD_USERKEYS_FILE, context.getEnvironment().TMD_USERKEYS_FILE);

        // Here, the "context.getCurrent()" inventory is not utilized on purpose as it is not set by the previous processor.
        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getGroupedStage(report, reportType, locale).appendAssetInventory());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getGroupedStage(report, reportType, locale).appendAssetInventory());

        processor.setProcessorParameter(ENV_TMD_SOURCE, context.getEnvironment().TMD_SOURCE);
        processor.setProcessorParameter(PARAM_LANGUAGE_MODE, locale.getIdentifier());
        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, context.getEnvironment().getWorkbenchDirNormalized() + context.getConfiguration().getOptions().getGlobal().getDebugParam());

        return processor;
    }
}
