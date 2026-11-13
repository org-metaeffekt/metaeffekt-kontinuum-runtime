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
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog;
import org.metaeffekt.kontinuum.runtime.models.shared.EnvironmentConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorExecutionPlan;
import org.metaeffekt.kontinuum.runtime.models.shared.Workspace;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

class ExtractStageHandlerTest {

    private static final String ARCHIVE_URL = "https://test-url.com/sample-extract.tar";

    private AssetExecutionContext context;
    private UrlResolver urlResolver;
    private final ExtractStageHandler handler = new ExtractStageHandler();
    private Asset asset;

    @BeforeEach
    void setUp() {
        context = TestUtils.buildMinimalAssetExecutionContext();
        asset = context.getAsset();
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
        asset.setType(AssetType.WINDOWS);

        handler.process(context);

        assertEquals("extract-windows-inventory", extraction().getId());
        assertEquals("extract/extract_windows-inventory.xml", extraction().getPomLocation());
    }

    @Test
    void linuxAssetTypeUsesLinuxExtraction() {
        asset.setType(AssetType.LINUX);

        handler.process(context);

        assertEquals("extract-linux-arch-inventory", extraction().getId());
        assertEquals("extract/extract_linux-arch-inventory.xml", extraction().getPomLocation());
    }

    @Test
    void applianceExtractionSetsRequiredParameters() {
        asset.setType(AssetType.WINDOWS);

        handler.process(context);

        assertTrue(param(INPUT_ARCHIVE_FILE).endsWith("/sample-extract.tar"));
        assertTrue(param(PARAM_ANALYSIS_DIR).endsWith("/analysis/"));
        assertTrue(param(OUTPUT_INVENTORY_FILE).endsWith(".xlsx"));
        assertEquals("workbench/config/excludes.yaml", param(PARAM_EXCLUDE_PATTERN_FILE));
    }

    @Test
    void applianceExtractionIsTheOnlyProcessorAndSetsCurrentInventory() {
        asset.setType(AssetType.WINDOWS);

        handler.process(context);

        assertEquals(1, context.getProcessors().size());
        assertEquals(param(OUTPUT_INVENTORY_FILE), context.getCurrentInventoryFile());
    }

    @Test
    void preExtractedInventorySkipsExtractionForAllAssetTypes() {
        asset.setType(AssetType.WINDOWS);
        urlResolver.setUrl("file:///tmp/inventory.xlsx");

        handler.process(context);

        assertTrue(context.getProcessors().isEmpty());
    }

    @Test
    void extractionScriptIsAppliedAfterExtraction() {
        asset.setExtractionScript(extractionScript("scripts/transform.kts"));

        handler.process(context);

        assertEquals(2, context.getProcessors().size());
        Processor extraction = context.getProcessors().get(0);
        Processor script = context.getProcessors().get(1);

        assertEquals("execute-kotlin-script", script.getId());
        assertEquals("workbench/scripts/transform.kts", param(script, INPUT_KOTLIN_SCRIPT_FILE));
        assertEquals(param(extraction, OUTPUT_INVENTORY_FILE), param(script, INPUT_INVENTORY_FILE));
        assertTrue(context.getDependencies(script).contains(extraction));
        assertTrue(context.getCurrentInventoryFile().endsWith("/scripted/" + asset.getId() + ".xlsx"));
    }

    @Test
    void extractionScriptUsesConfiguredInventoryName() {
        Asset.ExtractionScript extractionScript = extractionScript("scripts/transform.kts");
        extractionScript.setInventory("custom-inventory");
        asset.setExtractionScript(extractionScript);

        handler.process(context);

        assertTrue(context.getCurrentInventoryFile().endsWith("/scripted/custom-inventory.xlsx"));
    }

    @Test
    void extractionScriptPassesParamsAsJson() {
        Asset.ExtractionScript extractionScript = extractionScript("scripts/transform.kts");
        extractionScript.getParams().put("filter", "os");
        extractionScript.getParams().put("mode", "strict");
        asset.setExtractionScript(extractionScript);

        handler.process(context);

        Processor script = context.getProcessors().get(1);
        assertEquals("{\"filter\":\"os\",\"mode\":\"strict\"}", param(script, PARAM_SCRIPT_PARAMS));
    }

    @Test
    void absoluteScriptPathIsNotPrefixedWithWorkbench() {
        asset.setExtractionScript(extractionScript("/opt/scripts/transform.kts"));

        handler.process(context);

        Processor script = context.getProcessors().get(1);
        assertEquals("/opt/scripts/transform.kts", param(script, INPUT_KOTLIN_SCRIPT_FILE));
    }

    @Test
    void extractionScriptWithoutParamsOmitsParameter() {
        asset.setExtractionScript(extractionScript("scripts/transform.kts"));

        handler.process(context);

        Processor script = context.getProcessors().get(1);
        assertNull(param(script, PARAM_SCRIPT_PARAMS));
    }

    @Test
    void childWithoutResolverUsesParentInventory() {
        Asset child = addChild("child");
        ProcessorExecutionPlan plan = new ProcessorExecutionPlan();
        AssetExecutionContext parentContext = contextFor(asset, plan);
        AssetExecutionContext childContext = contextFor(child, plan);

        handler.process(parentContext);
        handler.process(childContext);

        assertTrue(childContext.getProcessors().isEmpty());
        assertEquals(parentContext.getCurrentInventoryFile(), childContext.getCurrentInventoryFile());
    }

    @Test
    void childScriptUsesParentInventoryAndDependsOnParent() {
        Asset child = addChild("child");
        child.setExtractionScript(extractionScript("scripts/child.kts"));
        ProcessorExecutionPlan plan = new ProcessorExecutionPlan();
        AssetExecutionContext parentContext = contextFor(asset, plan);
        AssetExecutionContext childContext = contextFor(child, plan);

        handler.process(parentContext);
        handler.process(childContext);

        Processor parentExtraction = parentContext.getProcessors().get(0);
        Processor childScript = childContext.getProcessors().get(0);

        assertEquals(parentContext.getCurrentInventoryFile(), param(childScript, INPUT_INVENTORY_FILE));
        assertTrue(childContext.getDependencies(childScript).contains(parentExtraction));
        assertTrue(childContext.getCurrentInventoryFile().endsWith("/child/scripted/child.xlsx"));
    }

    @Test
    void childWithoutProcessedParentFails() {
        Asset child = addChild("child");

        assertThrows(IllegalStateException.class, () -> handler.process(contextFor(child, new ProcessorExecutionPlan())));
    }

    private Asset addChild(String id) {
        Asset child = new Asset();
        child.setId(id);
        asset.setAssets(new ArrayList<>(List.of(child)));
        return child;
    }

    private AssetExecutionContext contextFor(Asset target, ProcessorExecutionPlan plan) {
        EnvironmentConfiguration environment = context.getEnvironment();
        return new AssetExecutionContext(plan, target, context.getConfiguration(), environment,
                new Workspace(context.getConfiguration(), environment),
                new DefaultProcessorCatalog());
    }

    private static Asset.ExtractionScript extractionScript(String script) {
        Asset.ExtractionScript extractionScript = new Asset.ExtractionScript();
        extractionScript.setScript(script);
        return extractionScript;
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