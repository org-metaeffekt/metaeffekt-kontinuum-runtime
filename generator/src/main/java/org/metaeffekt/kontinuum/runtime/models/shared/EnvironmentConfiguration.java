package org.metaeffekt.kontinuum.runtime.models.shared;

import java.io.File;

import lombok.Builder;
import lombok.experimental.SuperBuilder;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

@SuperBuilder
public abstract class EnvironmentConfiguration {
    
    @Builder.Default
    public final String WORKBENCH_DIR = "./workbench/";

    @Builder.Default
    public final String WORKSPACE_DIR = "./workspace/";

    @Builder.Default
    private final String VULNERABILITY_MIRROR_DIR = "./mirror/";

    public final String VULNERABILITY_MIRROR_URL;
    public final String ARTIFACT_RESOLVER_CONFIG_FILE;
    public final String ARTIFACT_RESOLVER_PROXY_FILE;
    public final String SCAN_PROPERTIES_FILE;
    public final String KOSMOS_PASSWORD;
    public final String KOSMOS_USERKEYS_FILE;
    public final File SETUP_COMMAND;
    private final String KONTINUUM_DIR;

    private final String PORTFOLIO_MANAGER_CLIENT_KEYSTORE_FILE;
    private final String PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_FILE;
    public final String PORTFOLIO_MANAGER_CLIENT_KEYSTORE_PASSWORD;
    public final String PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_PASSWORD;
    public final String PORTFOLIO_MANAGER_TOKEN;
    public final String PORTFOLIO_MANAGER_URL;

    public String getMirrorDir() {
        return VULNERABILITY_MIRROR_DIR;
    }

    public String getPortfolioManagerClientTruststoreFile() {
        return KontinuumUtils.normalizeFilePath(WORKBENCH_DIR, PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_FILE);
    }

    public String getPortfolioManagerClientKeystoreFile() {
        return KontinuumUtils.normalizeFilePath(WORKBENCH_DIR, PORTFOLIO_MANAGER_CLIENT_KEYSTORE_FILE);
    }

    public String getMirrorDatabaseDir() {
        return KontinuumUtils.normalizeDir(VULNERABILITY_MIRROR_DIR, ".database");
    }

    public String getCorrelationDir() {
        return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "correlations/");
    }

    public String getDescriptorsDirNormalized() { return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "descriptors/"); }

    public String getAssessmentsDir() {
        return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "assessments/");
    }

    public String getWorkbenchDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKBENCH_DIR);
    }

    public String getKontinuumDirNormalized() {
        return KontinuumUtils.normalizeDir(KONTINUUM_DIR);
    }

    public String getKontinuumProcessorsDirNormalized() {
        return KontinuumUtils.normalizeDir(KONTINUUM_DIR, "processors");
    }

    public abstract String getWorkspaceDirNormalized();
}


