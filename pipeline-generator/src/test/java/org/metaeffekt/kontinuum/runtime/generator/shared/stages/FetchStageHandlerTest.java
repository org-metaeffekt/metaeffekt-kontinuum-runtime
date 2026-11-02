package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;

import static org.junit.jupiter.api.Assertions.*;

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
}