package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.generator.gitlab.GitlabPipeline;
import org.metaeffekt.kontinuum.runtime.generator.local.LocalPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FetchStageHandlerTest {

    @Test
    public void testUrlResolverWithoutAuth() {
        PipelineConfiguration config = TestUtils.buildMinimalPipelineConfiguration();
        LocalConfiguration localConfig = TestUtils.buildMinimalLocalConfiguration();

        LocalPipeline localPipeline = new LocalPipeline(config, localConfig);
        String script = localPipeline.generatePipeline();

        assertTrue(script.contains("-Dparam.asset.url="));
        assertFalse(script.contains("-Dparam.asset.username="));
        assertFalse(script.contains("-Dparam.asset.password="));
        assertFalse(script.contains("-Dparam.asset.token="));
        assertFalse(script.contains("-Dparam.asset.header.name="));
        assertFalse(script.contains("-Dparam.asset.header.value="));
    }

    @Test
    public void testUrlResolverWithUsernameAndPassword() {
        PipelineConfiguration config = TestUtils.buildMinimalPipelineConfiguration();
        PipelineConfiguration.ProjectProperties.Asset.UrlResolver resolver =
                config.getProjectProperties().getAssets().get(0).getUrlResolver();
        resolver.setUsername("ci-user");
        resolver.setPassword("secret-pass");

        LocalConfiguration localConfig = TestUtils.buildMinimalLocalConfiguration();
        LocalPipeline localPipeline = new LocalPipeline(config, localConfig);
        String script = localPipeline.generatePipeline();

        assertTrue(script.contains("-Dparam.asset.url='https://test-url.com'"));
        assertTrue(script.contains("-Dparam.asset.username='ci-user'"));
        assertTrue(script.contains("-Dparam.asset.password='secret-pass'"));
        assertFalse(script.contains("-Dparam.asset.token="));
    }

    @Test
    public void testUrlResolverWithTokenAndCustomHeader() {
        PipelineConfiguration config = TestUtils.buildMinimalPipelineConfiguration();
        PipelineConfiguration.ProjectProperties.Asset.UrlResolver resolver =
                config.getProjectProperties().getAssets().get(0).getUrlResolver();
        resolver.setToken("glpat-xxxxxx");
        resolver.setHeaderName("PRIVATE-TOKEN");

        LocalConfiguration localConfig = TestUtils.buildMinimalLocalConfiguration();
        LocalPipeline localPipeline = new LocalPipeline(config, localConfig);
        String script = localPipeline.generatePipeline();

        assertTrue(script.contains("-Dparam.asset.token='glpat-xxxxxx'"));
        assertTrue(script.contains("-Dparam.asset.header.name='PRIVATE-TOKEN'"));
        assertFalse(script.contains("-Dparam.asset.username="));
        assertFalse(script.contains("-Dparam.asset.password="));
    }

    @Test
    public void testUrlResolverGitlabPipelineGenerationWithAuth() {
        PipelineConfiguration config = TestUtils.buildMinimalPipelineConfiguration();
        PipelineConfiguration.ProjectProperties.Asset.UrlResolver resolver =
                config.getProjectProperties().getAssets().get(0).getUrlResolver();
        resolver.setToken("my-bearer-token");

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:latest")
                .KONTINUUM_DIR("/usr/src/metaeffekt-kontinuum/")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("/workspace")
                .build();

        GitlabPipeline gitlabPipeline = new GitlabPipeline(config, gitlabConfig);
        String pipelineYaml = gitlabPipeline.generatePipeline();

        assertTrue(pipelineYaml.contains("-Dparam.asset.token='my-bearer-token'"));
        assertFalse(pipelineYaml.contains("-Dparam.asset.username="));
        assertFalse(pipelineYaml.contains("-Dparam.asset.password="));
    }
}
