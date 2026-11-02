package org.metaeffekt.kontinuum.runtime.generator.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetType;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipelineConfigurationLoaderTest {

    private PipelineConfiguration configuration;
    private Asset parent;

    @BeforeEach
    void setUp() {
        configuration = TestUtils.buildMinimalPipelineConfiguration();
        parent = configuration.getProjectProperties().getAssets().get(0);
        parent.setContext("local");
        parent.setAssets(new ArrayList<>());
    }

    @Test
    void nestedAssetInheritsMissingValuesFromParent() {
        Asset child = scriptedChild("child");
        parent.getAssets().add(child);

        validate();

        assertEquals(parent.getVersion(), child.getVersion());
        assertEquals(parent.getReference(), child.getReference());
        assertEquals(parent.getContext(), child.getContext());
    }

    @Test
    void nestedAssetKeepsOwnValues() {
        Asset child = scriptedChild("child");
        child.setVersion("9.9.9");
        parent.getAssets().add(child);

        validate();

        assertEquals("9.9.9", child.getVersion());
    }

    @Test
    void inheritanceCascadesThroughAllLevels() {
        Asset child = scriptedChild("child");
        Asset grandchild = scriptedChild("grandchild");
        child.setAssets(new ArrayList<>(List.of(grandchild)));
        parent.getAssets().add(child);

        validate();

        assertEquals(parent.getReference(), grandchild.getReference());
    }

    @Test
    void nestedAssetWithOwnResolverIsAccepted() {
        Asset child = scriptedChild("child");
        Asset.UrlResolver urlResolver = new Asset.UrlResolver();
        urlResolver.setUrl("https://example.com/child.zip");
        child.setUrlResolver(urlResolver);
        parent.getAssets().add(child);

        assertDoesNotThrow(this::validate);
    }

    @Test
    void nestedAssetWithoutResolverAndScriptIsAccepted() {
        Asset child = scriptedChild("child");
        child.setExtractionScript(null);
        parent.getAssets().add(child);

        assertDoesNotThrow(this::validate);
    }

    @Test
    void topLevelAssetWithExtractionScriptIsAccepted() {
        Asset.ExtractionScript extractionScript = new Asset.ExtractionScript();
        extractionScript.setScript("scripts/parent.kts");
        parent.setExtractionScript(extractionScript);

        assertDoesNotThrow(this::validate);
    }

    @Test
    void topLevelAssetWithoutResolverIsRejected() {
        parent.setUrlResolver(null);

        assertThrows(IllegalStateException.class, this::validate);
    }

    @Test
    void assetWithMultipleResolversIsRejected() {
        parent.setMavenResolver(new Asset.MavenResolver());

        assertThrows(IllegalStateException.class, this::validate);
    }

    @Test
    void assetWithTypeButWithoutResolverIsRejected() {
        Asset child = scriptedChild("child");
        child.setType(AssetType.WINDOWS);
        parent.getAssets().add(child);

        assertThrows(IllegalStateException.class, this::validate);
    }

    @Test
    void extractionScriptWithoutScriptIsRejected() {
        Asset child = scriptedChild("child");
        child.getExtractionScript().setScript(null);
        parent.getAssets().add(child);

        assertThrows(IllegalStateException.class, this::validate);
    }

    @Test
    void duplicateAssetIdsAreRejected() {
        parent.getAssets().add(scriptedChild(parent.getId()));

        assertThrows(IllegalStateException.class, this::validate);
    }

    private void validate() {
        new PipelineConfigurationLoader().validatePipelineConfigFile(configuration);
    }

    private static Asset scriptedChild(String id) {
        Asset.ExtractionScript extractionScript = new Asset.ExtractionScript();
        extractionScript.setScript("scripts/child.kts");

        Asset child = new Asset();
        child.setId(id);
        child.setExtractionScript(extractionScript);
        return child;
    }
}