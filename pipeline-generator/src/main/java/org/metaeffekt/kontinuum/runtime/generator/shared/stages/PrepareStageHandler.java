package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.StandaloneProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.Objects;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#PREPARE} stage.
 * Responsible for preparing SBOMs (CycloneDX, SPDX) and synchronizing data with Portfolio Manager.
 */
public class PrepareStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.PREPARE;
    }

    @Override
    public void process(AssetExecutionContext context) {

        PipelineConfiguration.Options.GlobalOptions globalOptions = context.getConfiguration().getOptions().getGlobal();
        assert globalOptions != null;

        boolean cycloneDxEnabled = globalOptions.getEnableCycloneDxBom();
        boolean spdxEnabled = globalOptions.getEnableSpdxBom();

        boolean hasPortfolioManager = Objects.nonNull(context.getConfiguration().getPortfolioManager());

        StandaloneProcessor inventoryCopyProcessor = handleInventoryCopy(context);
        context.addProcessor(inventoryCopyProcessor);

        if (cycloneDxEnabled) {
            MavenProcessor cycloneDxProcessor = handleInventoryToCycloneDxConversion(context);
            context.addProcessor(cycloneDxProcessor);
            context.addDependency(cycloneDxProcessor, inventoryCopyProcessor);
        }

        if (spdxEnabled) {
            MavenProcessor spdxProcessor = handleInventoryToSpdxConversion(context);
            context.addProcessor(spdxProcessor);
            context.addDependency(spdxProcessor, inventoryCopyProcessor);        }

        if (hasPortfolioManager) {
            MavenProcessor uploadProcessor = handlePortfolioUpload(context);
            MavenProcessor downloadProcessor = handlePortfolioDownload(context);

            context.addDependency(uploadProcessor, inventoryCopyProcessor);
            context.addSequential(uploadProcessor, downloadProcessor);
        }
    }

    /**
     * Copies an inventory file from the extract stage into the prepare stage workspace directory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_copy-inventory.sh">util_copy-inventory.sh</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link StandaloneProcessor} for copying the inventory.
     */
    private StandaloneProcessor handleInventoryCopy(AssetExecutionContext context) {
        StandaloneProcessor standaloneProcessor = (StandaloneProcessor) context.getProcessorCatalog().getProcessorById(COPY_INVENTORY);
        standaloneProcessor.setStage(Stage.PREPARE);

        standaloneProcessor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        standaloneProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.PREPARE).appendAssetInventory());

        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.PREPARE).toString());
        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.PREPARE).appendAssetInventory());

        return standaloneProcessor;
    }

    /**
     * Creates a CycloneDX BOM from the inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/convert/convert_inventory-to-cyclonedx.md">convert_inventory-to-cyclonedx.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for CycloneDX BOM conversion.
     */
    private MavenProcessor handleInventoryToCycloneDxConversion(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_CYCLONEDX);
        processor.setStage(Stage.PREPARE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.PREPARE).appendCycloneDxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        return processor;
    }

    /**
     * Creates an SPDX BOM from the inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/convert/convert_inventory-to-spdx.md">convert_inventory-to-spdx.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for SPDX BOM conversion.
     */
    private MavenProcessor handleInventoryToSpdxConversion(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_SPDX);
        processor.setStage(Stage.PREPARE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.PREPARE).appendSpdxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        return processor;
    }

    /**
     * Uploads the inventory to a running Portfolio Manager service.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/prepare/prepare_portfolio-upload.md">prepare_portfolio-upload.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for portfolio manager upload.
     */
    private MavenProcessor handlePortfolioUpload(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(PORTFOLIO_UPLOAD);
        processor.setStage(Stage.PREPARE);

        Asset rootAsset = context.getRootAsset();
        Asset target = (rootAsset != null) ? rootAsset : asset;
        String assetGroupId = target.getId() + ":" + target.getVersion();

        processor.setProcessorParameter(INPUT_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_URL, context.getEnvironment().PORTFOLIO_MANAGER_URL);
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_TOKEN, context.getEnvironment().PORTFOLIO_MANAGER_TOKEN);
        processor.setProcessorParameter(PARAM_PROJECT_NAME, context.getConfiguration().getPortfolioManager().getProject());
        processor.setProcessorParameter(PARAM_ASSET_GROUP_ID, assetGroupId);
        processor.setProcessorParameter(PARAM_ASSET_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_ASSET_VERSION, asset.getVersion());
        processor.setProcessorParameter(PARAM_KEYSTORE_CONFIG_FILE, context.getEnvironment().getPortfolioManagerClientKeystoreFile());
        processor.setProcessorParameter(PARAM_TRUSTSTORE_CONFIG_FILE, context.getEnvironment().getPortfolioManagerClientTruststoreFile());
        processor.setProcessorParameter(PARAM_KEYSTORE_PASSWORD, context.getEnvironment().PORTFOLIO_MANAGER_CLIENT_KEYSTORE_PASSWORD);
        processor.setProcessorParameter(PARAM_TRUSTSTORE_PASSWORD, context.getEnvironment().PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_PASSWORD);

        return processor;
    }

    /**
     * Downloads report artifacts from Portfolio Manager into the prepare stage reference directory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/aggregate/aggregate_portfolio-download.md">aggregate_portfolio-download.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for portfolio manager download.
     */
    private MavenProcessor handlePortfolioDownload(AssetExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(PORTFOLIO_DOWNLOAD);
        processor.setStage(Stage.PREPARE);

        processor.setProcessorParameter(OUTPUT_INVENTORY_DIR, context.getStageDirForAsset(Stage.PREPARE).appendPortfolioManagerReferenceDir());
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_URL, context.getEnvironment().PORTFOLIO_MANAGER_URL);
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_TOKEN, context.getEnvironment().PORTFOLIO_MANAGER_TOKEN);
        processor.setProcessorParameter(PARAM_PROJECT_NAME, context.getConfiguration().getPortfolioManager().getProject());
        processor.setProcessorParameter(PARAM_ASSET_GROUP_ID, "Reports:SNAPSHOT");
        processor.setProcessorParameter(PARAM_ASSET_ID, context.getConfiguration().getPortfolioManager().getProject());
        processor.setProcessorParameter(PARAM_KEYSTORE_CONFIG_FILE, context.getEnvironment().getPortfolioManagerClientKeystoreFile());
        processor.setProcessorParameter(PARAM_TRUSTSTORE_CONFIG_FILE, context.getEnvironment().getPortfolioManagerClientTruststoreFile());
        processor.setProcessorParameter(PARAM_KEYSTORE_PASSWORD, context.getEnvironment().PORTFOLIO_MANAGER_CLIENT_KEYSTORE_PASSWORD);
        processor.setProcessorParameter(PARAM_TRUSTSTORE_PASSWORD, context.getEnvironment().PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_PASSWORD);
        processor.setProcessorParameter(PARAM_INVENTORY_MODIFIER, "report");

        String targetInventoryPath = context.getStageDirForAsset(Stage.PREPARE).appendPortfolioManagerReferenceInventory();

        StringBuilder postScript = new StringBuilder();
        postScript.append("find ").append(context.getStageDirForAsset(Stage.PREPARE).appendPortfolioManagerReferenceDir()).append(" -type f -name \"*.zip\" -print0 | while IFS= read -r -d '' zip_file; do").append(System.lineSeparator());
        postScript.append("    zip_dir=$(dirname \"$zip_file\")").append(System.lineSeparator());
        postScript.append("    unzip -q -j \"$zip_file\" \"*_report.xlsx\" \"*_report.xls\" -d \"$zip_dir\" || true").append(System.lineSeparator());
        postScript.append("    extracted_file=$(find \"$zip_dir\" -maxdepth 1 -type f \\( -name \"*_report.xlsx\" -o -name \"*_report.xls\" \\) | head -n 1)").append(System.lineSeparator());
        postScript.append("    if [ -n \"$extracted_file\" ]; then").append(System.lineSeparator());
        postScript.append("        mv \"$extracted_file\" \"").append(targetInventoryPath).append("\"").append(System.lineSeparator());
        postScript.append("    fi").append(System.lineSeparator());
        postScript.append("done").append(System.lineSeparator());

        processor.setPostScript(postScript.toString());

        context.setPortfolioManagerReferenceInventoryDir(context.getStageDirForAsset(Stage.PREPARE).appendPortfolioManagerReferenceDir());
        return processor;
    }
}
