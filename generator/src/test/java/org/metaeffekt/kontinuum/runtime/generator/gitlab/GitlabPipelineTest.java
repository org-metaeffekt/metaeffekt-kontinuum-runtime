package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitlabPipelineTest {

    private static final File PIPELINE_CONFIG_FILE = new File("src/test/resources/valid-pipeline-config.yaml");

    @Test
    public void testValidPipelineGeneration() throws IOException {
        GitlabConfiguration gitlabConfiguration = GitlabConfiguration.builder()
        .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
        .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
        .KOSMOS_PASSWORD(KontinuumUtils.getLocalProperties().getProperty("KOSMOS_PASSWORD"))
        .RUNNER_TAG("gpu")
        .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.3.2_0.156.x")
        .build();
        
        PipelineConfiguration pipelineConfiguration = new PipelineConfigurationLoader().readConfig(PIPELINE_CONFIG_FILE);
        GitlabPipeline gitlabPipeline = new GitlabPipeline(pipelineConfiguration, gitlabConfiguration);

        Path outputPath = Path.of("target/generator/valid-gitlab-pipeline.yml");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, gitlabPipeline.generatePipeline());
    }
}
