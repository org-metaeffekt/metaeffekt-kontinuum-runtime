package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetType;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset.UrlResolver;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.ProcessorParameter;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey;

import static org.junit.jupiter.api.Assertions.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

class ExtractStageHandlerTest {

    private static final String ARCHIVE_URL = "https://test-url.com/sample-extract.tar";

    private AssetExecutionContext context;
    private UrlResolver urlResolver;
    private final ExtractStageHandler handler = new ExtractStageHandler();

    @BeforeEach
    void setUp() {
        context = TestUtils.buildMinimalAssetExecutionContext();
        urlResolver = context.getAsset().getUrlResolver();
        urlResolver.setUrl(ARCHIVE_URL);
        urlResolver.setExcludePatternFile("config/excludes.yaml");
    }

    @Test
    void defaultAssetTypeUsesScanDirectory() {
        handler.process(context);

        assertEquals("scan-directory", extraction().getId());
    }

    @Test
    void windowsAssetTypeUsesWindowsExtraction() {
        urlResolver.setAssetType(AssetType.WINDOWS);

        handler.process(context);

        assertEquals("extract-windows-inventory", extraction().getId());
        assertEquals("extract/extract_windows-inventory.xml", extraction().getPomLocation());
    }

    @Test
    void linuxAssetTypeUsesLinuxExtraction() {
        urlResolver.setAssetType(AssetType.LINUX);

        handler.process(context);

        assertEquals("extract-linux-arch-inventory", extraction().getId());
        assertEquals("extract/extract_linux-arch-inventory.xml", extraction().getPomLocation());
    }

    @Test
    void applianceExtractionSetsRequiredParameters() {
        urlResolver.setAssetType(AssetType.WINDOWS);

        handler.process(context);

        assertTrue(param(INPUT_ARCHIVE_FILE).endsWith("/sample-extract.tar"));
        assertTrue(param(PARAM_ANALYSIS_DIR).endsWith("/analysis/"));
        assertTrue(param(OUTPUT_INVENTORY_FILE).endsWith(".xlsx"));
        assertEquals("workbench/config/excludes.yaml", param(PARAM_EXCLUDE_PATTERN_FILE));
    }

    @Test
    void applianceExtractionWithoutKotlinScriptLeavesScriptParametersEmpty() {
        urlResolver.setAssetType(AssetType.WINDOWS);

        handler.process(context);

        assertNull(param(PARAM_KOTLIN_SCRIPT_FILE));
        assertNull(param(OUTPUT_SCRIPTED_INVENTORY_DIR));
    }

    @Test
    void applianceExtractionWithKotlinScriptSetsScriptParameters() {
        urlResolver.setAssetType(AssetType.WINDOWS);
        urlResolver.setKotlinScriptFile("scripts/transform.kts");

        handler.process(context);

        assertEquals("workbench/scripts/transform.kts", param(PARAM_KOTLIN_SCRIPT_FILE));
        assertTrue(param(OUTPUT_SCRIPTED_INVENTORY_DIR).endsWith("/scripted/"));
    }

    @Test
    void applianceExtractionIsTheOnlyProcessorAndSetsCurrentInventory() {
        urlResolver.setAssetType(AssetType.WINDOWS);

        handler.process(context);

        assertEquals(1, context.getProcessors().size());
        assertEquals(param(OUTPUT_INVENTORY_FILE), context.getCurrentInventoryFile());
    }

    @Test
    void preExtractedInventorySkipsExtractionForAllAssetTypes() {
        urlResolver.setAssetType(AssetType.WINDOWS);
        urlResolver.setUrl("file:///tmp/inventory.xlsx");

        handler.process(context);

        assertTrue(context.getProcessors().isEmpty());
    }


    private MavenProcessor extraction() {
        return (MavenProcessor) context.getProcessors().get(0);
    }

    private String param(ProcessorParameterKey key) {
        return param(extraction(), key);
    }

    private static String param(Processor processor, ProcessorParameterKey key) {
        return processor.getParameters().stream()
                .filter(p -> p.getKey() == key)
                .findFirst()
                .map(ProcessorParameter::getValue)
                .orElse(null);
    }
}