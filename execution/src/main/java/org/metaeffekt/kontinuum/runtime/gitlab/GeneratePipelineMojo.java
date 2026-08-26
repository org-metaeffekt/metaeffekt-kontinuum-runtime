package org.metaeffekt.kontinuum.runtime.gitlab;

import org.apache.commons.lang3.StringUtils;
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

@Mojo(name = "generate-gitlab-pipeline", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GeneratePipelineMojo extends AbstractGeneratePipelineMojo {

    @Parameter(property = "containerImage", required = true)
    private String containerImage;

    @Parameter(property = "runnerTag")
    private String runnerTag;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating GitLab CI pipeline from: " + pipelineConfigPath);

        File pipelineConfigFile = new File(pipelineConfigPath);

        if (!pipelineConfigFile.exists()) {
            throw new MojoExecutionException("The pipeline configuration file " + pipelineConfigFile.getAbsolutePath() + " does not exist.");
        }

       GitlabConfiguration.GitlabConfigurationBuilder gitlabConfiguration = GitlabConfiguration.builder()
                .ARTIFACT_RESOLVER_CONFIG_FILE(artifactResolverConfigFile)
                .ARTIFACT_RESOLVER_PROXY_FILE(artifactResolverProxyFile)
                .RUNNER_TAG(runnerTag);


        gitlabConfiguration.CONTAINER_IMAGE(containerImage)
                .KOSMOS_PASSWORD(kosmosPassword)
                .KOSMOS_USERKEYS_FILE(userkeysFile)
                .SCAN_PROPERTIES_FILE(scanPropertiesFile)
                .VULNERABILITY_MIRROR_DIR(vulnerabilityMirrorDir)
                .VULNERABILITY_MIRROR_URL(vulnerabilityMirrorUrl)
                .WORKBENCH_DIR(workbenchDir)
                .KONTINUUM_DIR(kontinuumDir)
                .WORKSPACE_DIR(workspaceDir)
                .PORTFOLIO_MANAGER_URL(portfolioManagerUrl)
                .PORTFOLIO_MANAGER_TOKEN(portfolioManagerToken)
                .PORTFOLIO_MANAGER_CLIENT_KEYSTORE_FILE(portfolioManagerClientKeystoreFile)
                .PORTFOLIO_MANAGER_CLIENT_KEYSTORE_PASSWORD(portfolioManagerClientKeystorePassword)
                .PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_FILE(portfolioManagerClientTruststoreFile)
                .PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_PASSWORD(portfolioManagerClientTruststorePassword)
                .MAVEN_CLI_OPTS(mavenCliOpts)
                .SETUP_COMMAND(setupCommand);


        PipelineConfiguration pipelineConfiguration = new PipelineConfigurationLoader().readConfig(pipelineConfigFile);

        GitlabPipeline gitlabPipeline = new GitlabPipeline(pipelineConfiguration, gitlabConfiguration.build());

        try {
            outputFile.getParentFile().mkdirs();
            Files.writeString(outputFile.toPath(), gitlabPipeline.generatePipeline());
            getLog().info("Pipeline written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write pipeline file: " + outputFile, e);
        }
    }
}
