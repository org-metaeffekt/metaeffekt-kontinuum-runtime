package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetType;
import org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.EXTRACT_LINUX_ARCH_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.EXTRACT_WINDOWS_INVENTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.SCAN_DIRECTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#EXTRACT} stage.
 * Responsible for extracting an inventory from fetched assets. Depending on the asset type of the
 * url resolver, the inventory is extracted by scanning the fetched directory (default) or from a
 * Windows or Linux appliance extraction archive.
 * Assets whose url resolver already references an extracted inventory (a local {@code .xls}/{@code .xlsx}
 * file) skip this stage, since the fetched file already is an inventory.
 */
public class ExtractStageHandler implements AssetStageHandler {

    @Override
    public Stage getStage() {
        return Stage.EXTRACT;
    }

    @Override
    public void process(AssetExecutionContext context) {
        if (context.getAsset().isPreExtractedInventory()) {
            return;
        }

        MavenProcessor inventoryExtraction = switch (getAssetType(context.getAsset())) {
            case WINDOWS -> handleApplianceExtraction(context, EXTRACT_WINDOWS_INVENTORY);
            case LINUX -> handleApplianceExtraction(context, EXTRACT_LINUX_ARCH_INVENTORY);
            case DEFAULT -> handleInventoryExtraction(context);
        };
        context.addProcessor(inventoryExtraction);
    }

    /**
     * Determines the asset type of the url resolver. Assets without url resolver or without
     * an explicit type use the default extraction.
     */
    private static AssetType getAssetType(Asset asset) {
        if (asset.getUrlResolver() == null || asset.getUrlResolver().getAssetType() == null) {
            return AssetType.DEFAULT;
        }
        return asset.getUrlResolver().getAssetType();
    }

    /**
     * Extracts an inventory from the fetched artifact directory.
     *
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for scanning and extracting the inventory.
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/prepare/extract_scan-directory.md">extract_scan-directory.md</a>
     */
    private MavenProcessor handleInventoryExtraction(AssetExecutionContext context) {
        Asset asset = context.getAsset();

        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(SCAN_DIRECTORY);
        mavenProcessor.setStage(Stage.EXTRACT);
        mavenProcessor.setProcessorParameter(INPUT_EXTRACT_DIR, context.getStageDirForAsset(Stage.FETCH).toString());
        mavenProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                                             context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());
        mavenProcessor.setProcessorParameter(OUTPUT_SCAN_DIR, context.getStageDirForAsset(Stage.EXTRACT) + "scan/");
        mavenProcessor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR,
                                             asset.getReferenceDir(
                                                     context.getEnvironment().getWorkbenchDirNormalized()));

        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.EXTRACT).toString());
        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());

        return mavenProcessor;
    }

    /**
     * Extracts an inventory from a Windows or Linux appliance extraction archive. Optionally applies
     * a kotlin script to the extracted inventory.
     *
     * @param context     The asset execution context containing pipeline and asset information.
     * @param processorId The extraction processor to use (windows or linux).
     * @return The configured {@link MavenProcessor} for the appliance extraction.
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/extract/extract_windows-inventory.md">extract_windows-inventory.md</a>
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/extract/extract_linux-arch-inventory.md">extract_linux-arch-inventory.md</a>
     */
    private MavenProcessor handleApplianceExtraction(AssetExecutionContext context, ProcessorIds processorId) {
        Asset asset = context.getAsset();
        String workbenchDir = context.getEnvironment().getWorkbenchDirNormalized();
        String extractDir = context.getStageDirForAsset(Stage.EXTRACT).toString();

        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(processorId);
        mavenProcessor.setStage(Stage.EXTRACT);
        mavenProcessor.setProcessorParameter(INPUT_ARCHIVE_FILE,
                context.getStageDirForAsset(Stage.FETCH) + asset.getUrlResolverFileName());
        mavenProcessor.setProcessorParameter(PARAM_ANALYSIS_DIR, extractDir + "analysis/");
        mavenProcessor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());
        mavenProcessor.setProcessorParameter(PARAM_EXCLUDE_PATTERN_FILE,
                KontinuumUtils.normalizeFilePath(workbenchDir, asset.getUrlResolver().getExcludePatternFile()));

        String kotlinScriptFile = asset.getUrlResolver().getKotlinScriptFile();
        if (StringUtils.isNotBlank(kotlinScriptFile)) {
            mavenProcessor.setProcessorParameter(PARAM_KOTLIN_SCRIPT_FILE,
                    KontinuumUtils.normalizeFilePath(workbenchDir, kotlinScriptFile));
            mavenProcessor.setProcessorParameter(OUTPUT_SCRIPTED_INVENTORY_DIR, extractDir + "scripted/");
        }

        context.setCurrentInventoryDir(extractDir);
        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());

        return mavenProcessor;
    }
}
