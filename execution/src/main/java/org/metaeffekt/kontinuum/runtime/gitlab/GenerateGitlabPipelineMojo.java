package org.metaeffekt.kontinuum.runtime.gitlab;

import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.kontinuum.runtime.AbstractGeneratePipelineMojo;
import org.metaeffekt.kontinuum.runtime.generator.gitlab.GitlabPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Maven Mojo that generates a GitLab CI pipeline from the configured pipeline definition.
 */
@Mojo(name = "generate-gitlab-pipeline", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
@Setter
public class GenerateGitlabPipelineMojo extends AbstractGeneratePipelineMojo {

    @Parameter(property = "containerImage", required = true)
    private String containerImage;

    @Parameter(property = "runnerTag")
    private String runnerTag;

    @Parameter(property = "containerPatchDirectory")
    private String containerPatchDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating GitLab CI pipeline from: " + pipelineConfigPath);

        File pipelineConfigFile = new File(pipelineConfigPath);

        if (!pipelineConfigFile.exists()) {
            throw new MojoExecutionException(
                    "The pipeline configuration file " + pipelineConfigFile.getAbsolutePath() + " does not exist.");
        }

        GitlabConfiguration gitlabConfiguration = GitlabConfiguration.builder()
                .ARTIFACT_RESOLVER_CONFIG_FILE(
                        artifactResolverConfigFile)
                .ARTIFACT_RESOLVER_PROXY_FILE(
                        artifactResolverProxyFile)
                .RUNNER_TAG(runnerTag)
                .CONTAINER_IMAGE(containerImage)
                .termsMetadataOptions(termsMetadataOptions)
                .SCAN_PROPERTIES_FILE(scanPropertiesFile)
                .VULNERABILITY_MIRROR_DIR(vulnerabilityMirrorDir)
                .VULNERABILITY_MIRROR_URL(vulnerabilityMirrorUrl)
                .WORKBENCH_DIR(workbenchDir)
                .KONTINUUM_DIR(kontinuumDir)
                .WORKSPACE_DIR(workspaceDir)
                .portfolioManagerOptions(portfolioManagerOptions)
                .MAVEN_CLI_OPTS(mavenCliOpts)
                .LOCAL_MAVEN_REPO(localMavenRepo)
                .SETUP_COMMAND(setupCommand)
                .CONTAINER_PATCH_DIRECTORY(containerPatchDirectory)
                .overwriteOptions(overwriteOptions)
                .build();


        PipelineConfiguration pipelineConfiguration = new PipelineConfigurationLoader().readConfig(pipelineConfigFile);

        GitlabPipeline gitlabPipeline = new GitlabPipeline(pipelineConfiguration, gitlabConfiguration);

        try {
            outputFile.getParentFile().mkdirs();
            Files.writeString(outputFile.toPath(), gitlabPipeline.generatePipeline());
            getLog().info("Pipeline written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write pipeline file: " + outputFile, e);
        }
    }
}
