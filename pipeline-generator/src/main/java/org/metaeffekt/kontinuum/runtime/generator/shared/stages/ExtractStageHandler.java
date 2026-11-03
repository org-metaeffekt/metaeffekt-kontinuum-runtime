package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

import java.util.IdentityHashMap;
import java.util.Map;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.EXECUTE_KOTLIN_SCRIPT;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.SCAN_DIRECTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#EXTRACT} stage.
 * Determines the inventory of each asset:
 * <ul>
 *     <li>Assets with a resolver extract their inventory from the fetched files, depending on their asset type
 *     (directory scan by default, or a Windows or Linux appliance extraction archive). Assets whose url resolver
 *     already references an extracted inventory skip the extraction.</li>
 *     <li>Assets without a resolver use the inventory of their parent asset.</li>
 * </ul>
 * If an extraction script is configured, it is applied to the inventory afterward.
 */
public class ExtractStageHandler implements AssetStageHandler {

    /**
     * Remembers the inventory and the producing processor of every processed asset, so that nested assets without
     * a resolver can use the inventory of their parent. Assets are processed parent-first (see
     * {@link org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties#getAllAssets()}).
     */
    private final Map<Asset, ExtractionResult> extractionResults = new IdentityHashMap<>();

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private record ExtractionResult(String inventoryFile, String inventoryDir, Processor producer) {
    }

    @Override
    public Stage getStage() {
        return Stage.EXTRACT;
    }

    @Override
    public void process(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        Processor producer;

        if (asset.hasResolver()) {
            producer = handleExtraction(context);
        } else {
            producer = useParentInventory(context);
        }

        if (asset.isScriptedAsset()) {
            MavenProcessor script = handleExtractionScript(context);
            context.addProcessor(script);
            if (producer != null) {
                context.addDependency(script, producer);
            }
            producer = script;
        }

        extractionResults.put(asset, new ExtractionResult(
                context.getCurrentInventoryFile(), context.getCurrentInventoryDir(), producer));
    }

    /**
     * Extracts the inventory of an asset with a resolver, depending on its asset type.
     *
     * @param context The asset execution context containing pipeline and asset information.
     * @return the extraction processor, or null if the fetched file already is an inventory
     */
    private Processor handleExtraction(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        if (asset.isPreExtractedInventory()) {
            return null;
        }

        MavenProcessor extraction = switch (asset.getType()) {
            case WINDOWS -> handleApplianceExtraction(context, ProcessorIds.EXTRACT_WINDOWS_INVENTORY);
            case LINUX -> handleApplianceExtraction(context, ProcessorIds.EXTRACT_LINUX_ARCH_INVENTORY);
            default -> handleInventoryExtraction(context);
        };
        context.addProcessor(extraction);
        return extraction;
    }

    /**
     * Uses the inventory of the parent asset as the inventory of the current asset.
     *
     * @param context The asset execution context containing pipeline and asset information.
     * @return the processor that produced the parent inventory, or null if it was produced in an earlier stage
     */
    private Processor useParentInventory(AssetExecutionContext context) {
        Asset parent = context.getParentAsset();
        ExtractionResult parentResult = extractionResults.get(parent);
        if (parentResult == null) {
            throw new IllegalStateException(String.format(
                    "Asset %s has no resolver, but no inventory of its parent asset %s is available.",
                    context.getAsset(), parent));
        }

        context.setCurrentInventoryFile(parentResult.inventoryFile());
        context.setCurrentInventoryDir(parentResult.inventoryDir());
        return parentResult.producer();
    }

    /**
     * Applies the extraction script of the asset to its current inventory.
     *
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for executing the kotlin script.
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_execute-kotlin-script.md">util_execute-kotlin-script.md</a>
     */
    private MavenProcessor handleExtractionScript(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        String scriptedInventoryDir = context.getStageDirForAsset(Stage.EXTRACT) + "scripted/";
        String scriptedInventoryFile = scriptedInventoryDir + asset.getExtractionScriptInventoryFileName();

        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(
                EXECUTE_KOTLIN_SCRIPT);
        processor.setStage(Stage.EXTRACT);
        processor.setProcessorParameter(INPUT_KOTLIN_SCRIPT_FILE,
                KontinuumUtils.normalizeFilePath(context.getEnvironment().getWorkbenchDirNormalized(),
                        asset.getExtractionScript().getScript()));
        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getCurrentInventoryFile());
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE, scriptedInventoryFile);
        processor.setProcessorParameter(PARAM_ASSET_ID, asset.getId());

        Map<String, String> scriptParams = asset.getExtractionScript().getParams();
        if (scriptParams != null && !scriptParams.isEmpty()) {
            processor.setProcessorParameter(PARAM_SCRIPT_PARAMS, toJson(scriptParams, asset));
        }

        context.setCurrentInventoryFile(scriptedInventoryFile);
        context.setCurrentInventoryDir(scriptedInventoryDir);

        return processor;
    }

    /**
     * Extracts an inventory from a Windows or Linux appliance extraction archive.
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

        context.setCurrentInventoryDir(extractDir);
        context.setCurrentInventoryFile(context.getStageDirForAsset(Stage.EXTRACT).appendAssetInventory());

        return mavenProcessor;
    }

    private static String toJson(Map<String, String> scriptParams, Asset asset) {
        try {
            return JSON_MAPPER.writeValueAsString(scriptParams);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to convert 'extractionScript.params' of asset " + asset + " to json.", e);
        }
    }
}
