package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.StandaloneProcessor;

import java.util.List;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.APPLY_BUSINESS_CASE;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.COPY_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#GROUP} stage.
 * Consolidates asset inventories into relevant group stage subdirectories for each
 * report entry in the pipeline configuration upon which report generation is based.
 * If multiple asset IDs are listed for a single report entry, all those asset inventories
 * are copied to the consolidated grouped subdirectory.
 * For software distribution annex (SDA), the inventory is enriched with license notices
 * and business case information via the apply business case processor.
 */
public class GroupStageHandler implements StageHandler {

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

        for (PipelineConfiguration.Report report : reports) {
            List<String> types = report.getTypes();
            List<SupportedLocale> locales = report.getLocales();

            for (SupportedLocale locale : locales) {
                for (String type : types) {
                    ReportType reportType = ReportType.fromKey(type);

                    StandaloneProcessor copyProcessor = handleInventoryCopy(context, report, reportType, locale);
                    context.addProcessor(copyProcessor);

                    if (ReportType.requiresScan(reportType)) {
                        MavenProcessor businessCaseProcessor = handleApplyBusinessCase(context, report, reportType, locale);
                        context.addDependency(businessCaseProcessor, copyProcessor);
                        context.addProcessor(businessCaseProcessor);
                    }
                }
            }
        }
    }

    /**
     * Copies the asset inventory into the grouped report subdirectory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_copy-inventory.sh">util_copy-inventory.sh</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @param report The report configuration defining the grouped target.
     * @param reportType The report type being grouped.
     * @param locale The target locale for the grouped report.
     * @return The configured {@link StandaloneProcessor} for copying the inventory.
     */
    private StandaloneProcessor handleInventoryCopy(AssetExecutionContext context, PipelineConfiguration.Report report, ReportType reportType, SupportedLocale locale) {
        StandaloneProcessor standaloneProcessor = (StandaloneProcessor) context.getProcessorCatalog().getProcessorById(COPY_INVENTORY);
        standaloneProcessor.setStage(Stage.GROUP);

        standaloneProcessor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
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

        return processor;
    }
}
