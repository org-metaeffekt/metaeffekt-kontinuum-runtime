package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ReportType;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.List;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#SUMMARIZE} stage.
 * Responsible for creating CycloneDX and SPDX documents in the summarize stage
 * if enabled in the pipeline configuration.
 */
public class SummarizeStageHandler implements AssetStageHandler {

    @Override
    public Stage getStage() {
        return Stage.SUMMARIZE;
    }

    @Override
    public void process(AssetExecutionContext context) {
        PipelineConfiguration.Options.GlobalOptions globalOptions = context.getConfiguration().getOptions().getGlobal();
        assert globalOptions != null;

        if (globalOptions.getEnableCycloneDxBom()) {
            context.addProcessor(handleInventoryToCycloneDxConversion(context));
        }

        if (globalOptions.getEnableSpdxBom()) {
            context.addProcessor(handleInventoryToSpdxConversion(context));
        }

        if (context.getConfiguration().getOverviews() != null && !context.getConfiguration().getOverviews().isEmpty()) {
            context.addSequential(handleOverviewResources(context), handleOverviewCreation(context));
        }
    }

    /**
     * Creates a CycloneDX BOM from the inventory in the summarize stage.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/convert/convert_inventory-to-cyclonedx.md">convert_inventory-to-cyclonedx.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for CycloneDX BOM conversion.
     */
    private MavenProcessor handleInventoryToCycloneDxConversion(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_CYCLONEDX);
        processor.setStage(Stage.SUMMARIZE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.SUMMARIZE).appendCycloneDxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        return processor;
    }

    /**
     * Creates an SPDX BOM from the inventory in the summarize stage.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/convert/convert_inventory-to-spdx.md">convert_inventory-to-spdx.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for SPDX BOM conversion.
     */
    private MavenProcessor handleInventoryToSpdxConversion(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_SPDX);
        processor.setStage(Stage.SUMMARIZE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.SUMMARIZE).appendSpdxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        return processor;
    }

    private MavenProcessor handleOverviewResources(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(COPY_RESOURCES);
        processor.setStage(Stage.SUMMARIZE);

        processor.setProcessorParameter(INPUT_ADVISOR_INVENTORIES_DIR, context.getCurrentInventoryDir());
        processor.setProcessorParameter(INPUT_DASHBOARDS_DIR, context.getStageDirForAsset(Stage.REPORT).appendDashboardDir());
        processor.setProcessorParameter(INPUT_INVENTORIES_DIR, context.getStageDirForAsset(Stage.PREPARE).toString());
        processor.setProcessorParameter(INPUT_REPORTS_DIR, resolveReportsDir(context));
        processor.setProcessorParameter(OUTPUT_RESOURCES_DIR, context.getStageDirForAsset(Stage.SUMMARIZE).toString() + "resources/");

        return processor;
    }

    /**
     * Reports are written into group-keyed directories ({@code 08_reported/<groupId>/}). If exactly
     * one report entry contains the asset, its group directory is used; otherwise the report root
     * is used, since the asset's reports may be spread across several groups.
     */
    private String resolveReportsDir(AssetExecutionContext context) {
        List<PipelineConfiguration.Report> reports = context.getConfiguration().getReports();
        if (reports != null) {
            List<PipelineConfiguration.Report> reportsForAsset = reports.stream()
                    .filter(report -> report != null
                            && report.getAssetIds() != null
                            && report.getAssetIds().contains(context.getAsset().getId()))
                    .toList();
            if (reportsForAsset.size() == 1) {
                return context.getWorkspace().getReportDir(reportsForAsset.get(0)).toString();
            }
        }
        return context.getWorkspace().getReportRootDir();
    }

    /**
     * Creates an overview report from the resources gathered during previous stages.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/portfolio/portfolio_create-overview.md">portfolio_create-overview.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for overview creation.
     */
    private MavenProcessor handleOverviewCreation(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(CREATE_OVERVIEW);
        processor.setStage(Stage.SUMMARIZE);

        processor.setProcessorParameter(INPUT_ADVISOR_INVENTORIES_DIR, "advisor-inventories");
        processor.setProcessorParameter(INPUT_DASHBOARDS_DIR, context.getStageDirForAsset(Stage.REPORT).appendDashboardDir());
        processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getStageDirForAsset(Stage.SUMMARIZE).toString() + "resources/");
        processor.setProcessorParameter(INPUT_INVENTORY_PATH, "source-inventories");
        processor.setProcessorParameter(INPUT_REPORTS_DIR, "vulnerability-reports");
        processor.setProcessorParameter(OUTPUT_OVERVIEW_FILE, context.getStageDirForAsset(Stage.SUMMARIZE).appendOverviewFile());
        processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE, context.getConfiguration().getOptions().getEnrichment().getSecurityPolicyFile());
        processor.setProcessorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS, context.getConfiguration().getOptions().getEnrichment().getSecurityPolicyActiveIds().toString());
        return processor;
    }


}
