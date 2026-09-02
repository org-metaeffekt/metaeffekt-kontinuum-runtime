package org.metaeffekt.kontinuum.runtime.local;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.kontinuum.runtime.AbstractGeneratePipelineMojo;
import org.metaeffekt.kontinuum.runtime.generator.local.LocalPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Mojo(name = "generate-local-pipeline", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateLocalPipelineMojo extends AbstractGeneratePipelineMojo {

    @Parameter(property = "executionEnvironment", defaultValue = "UNIX")
    private LocalConfiguration.ExecutionEnvironment executionEnvironment;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating local execution pipeline from: " + pipelineConfigPath);

        File pipelineConfigFile = new File(pipelineConfigPath);

        if (!pipelineConfigFile.exists()) {
            throw new MojoExecutionException("The pipeline configuration file " + pipelineConfigFile.getAbsolutePath() + " does not exist.");
        }

        LocalConfiguration localConfiguration = LocalConfiguration.builder()
                .executionEnvironment(executionEnvironment)
                .TMD_PASSWORD(tmdPassword)
                .TMD_USERKEYS_FILE(tmdUserkeysFile)
                .TMD_SOURCE(tmdSource)
                .ARTIFACT_RESOLVER_CONFIG_FILE(artifactResolverConfigFile)
                .ARTIFACT_RESOLVER_PROXY_FILE(artifactResolverProxyFile)
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
                .build();

        PipelineConfiguration pipelineConfiguration = new PipelineConfigurationLoader().readConfig(pipelineConfigFile);

        LocalPipeline localPipeline = new LocalPipeline(pipelineConfiguration, localConfiguration);

        try {
            outputFile.getParentFile().mkdirs();
            Files.writeString(outputFile.toPath(), localPipeline.generatePipeline());
            getLog().info("Local pipeline written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write local pipeline file: " + outputFile, e);
        }
    }
}