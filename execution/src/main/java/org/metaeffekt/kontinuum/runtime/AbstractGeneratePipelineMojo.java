package org.metaeffekt.kontinuum.runtime;

import lombok.Setter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

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

    @Parameter(property = "tmdPassword")
    protected String tmdPassword;

    @Parameter(property = "tmdUserkeysFile")
    protected String tmdUserkeysFile;

    @Parameter(property = "tmdSource")
    protected String tmdSource;

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

    @Parameter(property = "portfolioManagerClientKeystoreFile")
    protected String portfolioManagerClientKeystoreFile;

    @Parameter(property = "portfolioManagerClientTruststoreFile")
    protected String portfolioManagerClientTruststoreFile;

    @Parameter(property = "portfolioManagerClientKeystorePassword")
    protected String portfolioManagerClientKeystorePassword;

    @Parameter(property = "portfolioManagerClientTruststorePassword")
    protected String portfolioManagerClientTruststorePassword;

    @Parameter(property = "portfolioManagerToken")
    protected String portfolioManagerToken;

    @Parameter(property = "portfolioManagerUrl")
    protected String portfolioManagerUrl;

}
