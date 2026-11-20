package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.ProcessorParameter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.PARAM_ASSET_URL;

class FetchStageHandlerTest {

    private final FetchStageHandler handler = new FetchStageHandler();

    private AssetExecutionContext context;
    private Asset asset;

    @BeforeEach
    void setUp() {
        context = TestUtils.buildMinimalAssetExecutionContext();
        asset = context.getAsset();
    }

    @Test
    void assetWithResolverIsFetched() {
        handler.process(context);

        assertEquals(1, context.getProcessors().size());
    }

    @Test
    void assetWithoutResolverIsNotFetched() {
        asset.setUrlResolver(null);

        handler.process(context);

        assertTrue(context.getProcessors().isEmpty());
    }

    @Test
    void assetWithResolverAndScriptIsFetched() {
        asset.setExtractionScript(new Asset.ExtractionScript());

        handler.process(context);

        assertEquals(1, context.getProcessors().size());
    }

    @Test
    void relativeFileUrlIsResolvedAgainstWorkbench() {
        asset.getUrlResolver().setUrl("file:inventories/app/app-inventory.xlsx");

        handler.process(context);

        assertEquals("file:workbench/inventories/app/app-inventory.xlsx", assetUrl());
        assertEquals("app-inventory.xlsx", asset.getUrlResolverFileName());
        assertTrue(asset.isPreExtractedInventory());
    }

    @Test
    void absoluteFileUrlsAreUnchanged() {
        for (String url : List.of("file:///data/extract.tar", "file:/data/extract.tar", "file://host/extract.tar")) {
            context = TestUtils.buildMinimalAssetExecutionContext();
            asset = context.getAsset();
            asset.getUrlResolver().setUrl(url);

            handler.process(context);

            assertEquals(url, assetUrl());
        }
    }

    @Test
    void nonFileUrlIsUnchanged() {
        asset.getUrlResolver().setUrl("https://example.com/archive.zip");

        handler.process(context);

        assertEquals("https://example.com/archive.zip", assetUrl());
    }

    private String assetUrl() {
        return context.getProcessors().get(0).getParameters().stream()
                .filter(p -> p.getKey() == PARAM_ASSET_URL)
                .findFirst()
                .map(ProcessorParameter::getValue)
                .orElse(null);
    }
}