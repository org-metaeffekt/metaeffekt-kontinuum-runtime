package org.metaeffekt.kontinuum.runtime;

import lombok.Setter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.kontinuum.runtime.models.shared.OverwriteOptions;
import org.metaeffekt.kontinuum.runtime.models.shared.PortfolioManagerOptions;
import org.metaeffekt.kontinuum.runtime.models.shared.TermsMetadataOptions;

import java.io.File;

/**
 * Shared base class for Maven Mojos that generate a Kontinuum execution pipeline.
 */
@Setter
public abstract class AbstractGeneratePipelineMojo extends AbstractMojo {

    @Parameter(property = "pipelineConfigPath", required = true)
    protected String pipelineConfigPath;

    @Parameter(property = "outputFile", required = true)
    protected File outputFile;

    @Parameter(property = "mavenCliOpts")
    protected String mavenCliOpts;

    @Parameter(property = "localMavenRepo", alias = "local.maven.repo")
    protected String localMavenRepo;

    @Parameter(property = "artifactResolverConfigFile")
    protected String artifactResolverConfigFile;

    @Parameter(property = "artifactResolverProxyFile")
    protected String artifactResolverProxyFile;

    @Parameter(property = "setupCommand")
    protected String setupCommand;

    @Parameter(property = "scanPropertiesFile")
    protected String scanPropertiesFile;

    @Parameter(property = "vulnerabilityMirrorDir")
    protected String vulnerabilityMirrorDir;

    @Parameter(property = "vulnerabilityMirrorUrl")
    protected String vulnerabilityMirrorUrl;

    @Parameter(property = "workbenchDir", required = true)
    protected String workbenchDir;

    @Parameter(property = "workspaceDir", required = true)
    protected String workspaceDir;

    @Parameter(property = "kontinuumDir", defaultValue = "/usr/src/metaeffekt-kontinuum/")
    protected String kontinuumDir;

    @Parameter
    protected TermsMetadataOptions termsMetadataOptions = new TermsMetadataOptions();

    @Parameter
    protected PortfolioManagerOptions portfolioManagerOptions = new PortfolioManagerOptions();

    @Parameter
    protected OverwriteOptions overwriteOptions = new OverwriteOptions();
}
