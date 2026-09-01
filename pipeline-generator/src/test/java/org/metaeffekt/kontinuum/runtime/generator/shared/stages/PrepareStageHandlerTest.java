package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.generator.local.LocalPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.generator.shared.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrepareStageHandlerTest {

    @Test
    public void testPortfolioManagerAssetGroupIdUsesAssetIdForRootAsset() {
        PipelineConfiguration config = TestUtils.buildMinimalPipelineConfiguration();
        PipelineConfiguration.PortfolioManager pm = new PipelineConfiguration.PortfolioManager();
        pm.setProject("test-project");
        config.setPortfolioManager(pm);

        LocalConfiguration localConfig = TestUtils.buildMinimalLocalConfiguration();
        LocalPipeline localPipeline = new LocalPipeline(config, localConfig);
        String script = localPipeline.generatePipeline();

        assertTrue(script.contains("-Dparam.asset.group.id='asset-id:1.0.0'"));
        assertTrue(script.contains("-Dparam.project.name='test-project'"));
    }

    @Test
    public void testPortfolioManagerAssetGroupIdUsesRootAssetIdForNestedAsset() {
        PipelineConfiguration config = TestUtils.buildMinimalPipelineConfiguration();
        PipelineConfiguration.PortfolioManager pm = new PipelineConfiguration.PortfolioManager();
        pm.setProject("test-project");
        config.setPortfolioManager(pm);

        Asset rootAsset = config.getProjectProperties().getAssets().get(0);
        rootAsset.setId("root-asset-id");
        rootAsset.setVersion("2.5.0");

        Asset nestedAsset = new Asset();
        nestedAsset.setId("nested-asset-id");
        nestedAsset.setName("nested-asset-name");
        nestedAsset.setVersion("1.0.0");
        nestedAsset.setReference(rootAsset.getReference());
        nestedAsset.setUrlResolver(rootAsset.getUrlResolver());

        rootAsset.setAssets(List.of(nestedAsset));

        LocalConfiguration localConfig = TestUtils.buildMinimalLocalConfiguration();
        LocalPipeline localPipeline = new LocalPipeline(config, localConfig);
        String script = localPipeline.generatePipeline();

        // Both root and nested asset uploads should use the root asset's ID:version as the asset group ID
        assertTrue(script.contains("-Dparam.asset.name='asset-name'"));
        assertTrue(script.contains("-Dparam.asset.name='nested-asset-name'"));
        // Both occurrences of param.asset.group.id should be 'root-asset-id:2.5.0'
        assertTrue(script.contains("-Dparam.asset.group.id='root-asset-id:2.5.0'"));
        assertFalse(script.contains("-Dparam.asset.group.id='nested-asset-id:1.0.0'"));
    }

    @Test
    public void testPortfolioManagerValidationWithoutAssetGroup() {
        PipelineConfigurationLoader loader = new PipelineConfigurationLoader();
        PipelineConfiguration config = loader.readConfig(new File("src/test/resources/valid-pipeline-config.yaml"));

        assertNotNull(config);
        assertNotNull(config.getPortfolioManager());
        assertEquals("metaeffekt-kontinuum", config.getPortfolioManager().getProject());
    }
}
