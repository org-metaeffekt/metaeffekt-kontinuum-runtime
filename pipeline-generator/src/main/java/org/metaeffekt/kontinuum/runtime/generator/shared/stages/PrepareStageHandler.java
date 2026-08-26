package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.Objects;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

public class PrepareStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.PREPARE;
    }

    @Override
    public void process(AssetExecutionContext context) {

        handleInventoryCopy(context);

        if (context.getConfiguration().getOptions().getGlobal().getEnableCycloneDxBom()) {
            handleInventoryToCycloneDxConversion(context);
        }

        if (context.getConfiguration().getOptions().getGlobal().getEnableSpdxBom()) {
            handleInventoryToSpdxConversion(context);
        }

        if (Objects.nonNull(context.getConfiguration().getPortfolioManager())) {
            handlePortfolioUpload(context);
            handlePortfolioDownload(context);
        }
    }

    private void handleInventoryCopy(AssetExecutionContext context) {
        ProcessorDefinitions.StandaloneProcessor standaloneProcessor = (ProcessorDefinitions.StandaloneProcessor) context.getProcessorCatalog().getProcessorById(COPY_INVENTORY);
        standaloneProcessor.setStage(Stage.PREPARE);

        standaloneProcessor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        standaloneProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getStageDirForAsset(Stage.PREPARE).appendAssetInventory());

        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.PREPARE).toString());
        context.setCurrentInventoryPath(context.getStageDirForAsset(Stage.PREPARE).appendAssetInventory());
        context.addProcessor(standaloneProcessor);
    }

    private void handleInventoryToCycloneDxConversion(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_CYCLONEDX);
        processor.setStage(Stage.PREPARE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.PREPARE).appendCycloneDxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        context.addProcessor(processor);
    }

    private void handleInventoryToSpdxConversion(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(INVENTORY_TO_SPDX);
        processor.setStage(Stage.PREPARE);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, context.getStageDirForAsset(Stage.PREPARE).appendSpdxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        context.addProcessor(processor);
    }

    private void handlePortfolioUpload(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(PORTFOLIO_UPLOAD);
        processor.setStage(Stage.PREPARE);

        processor.setProcessorParameter(INPUT_FILE, context.getCurrentInventoryPath());
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_URL, context.getEnvironment().PORTFOLIO_MANAGER_URL);
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_TOKEN, context.getEnvironment().PORTFOLIO_MANAGER_TOKEN);
        processor.setProcessorParameter(PARAM_PROJECT_NAME, context.getConfiguration().getPortfolioManager().getProject());
        processor.setProcessorParameter(PARAM_ASSET_GROUP_ID, context.getConfiguration().getPortfolioManager().getAssetGroup());
        processor.setProcessorParameter(PARAM_ASSET_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_ASSET_VERSION, asset.getVersion());
        processor.setProcessorParameter(PARAM_KEYSTORE_CONFIG_FILE, context.getEnvironment().getPortfolioManagerClientKeystoreFile());
        processor.setProcessorParameter(PARAM_TRUSTSTORE_CONFIG_FILE, context.getEnvironment().getPortfolioManagerClientTruststoreFile());
        processor.setProcessorParameter(PARAM_KEYSTORE_PASSWORD, context.getEnvironment().PORTFOLIO_MANAGER_CLIENT_KEYSTORE_PASSWORD);
        processor.setProcessorParameter(PARAM_TRUSTSTORE_PASSWORD, context.getEnvironment().PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_PASSWORD);

        context.addProcessor(processor);
    }

    private void handlePortfolioDownload(AssetExecutionContext context) {
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
        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.PREPARE).toString());
        context.setCurrentInventoryPath(context.getStageDirForAsset(Stage.PREPARE).appendAssetInventory());
        context.addProcessor(processor);
    }
}
