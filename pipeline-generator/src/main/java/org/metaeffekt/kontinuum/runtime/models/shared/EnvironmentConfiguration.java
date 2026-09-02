package org.metaeffekt.kontinuum.runtime.models.shared;

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
    public final String TMD_PASSWORD;
    public final String TMD_USERKEYS_FILE;
    public final String TMD_SOURCE = "ae-kosmos";
    public final String SETUP_COMMAND;
    private final String KONTINUUM_DIR;

    private final String PORTFOLIO_MANAGER_CLIENT_KEYSTORE_FILE;
    private final String PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_FILE;
    public final String PORTFOLIO_MANAGER_CLIENT_KEYSTORE_PASSWORD;
    public final String PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_PASSWORD;
    public final String PORTFOLIO_MANAGER_TOKEN;
    public final String PORTFOLIO_MANAGER_URL;
    public final String MAVEN_CLI_OPTS;

    public String getMirrorDir() {
        return VULNERABILITY_MIRROR_DIR;
    }

    public String getPortfolioManagerClientTruststoreFile() {
        return KontinuumUtils.normalizeFilePath(WORKBENCH_DIR, PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_FILE);
    }

    public String getPortfolioManagerClientKeystoreFile() {
        return KontinuumUtils.normalizeFilePath(WORKBENCH_DIR, PORTFOLIO_MANAGER_CLIENT_KEYSTORE_FILE);
    }

    public String getMirrorDatabaseDirNormalized() {
        return KontinuumUtils.normalizeDir(VULNERABILITY_MIRROR_DIR, ".database");
    }

    public String getCorrelationDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "correlations/");
    }

    public String getDescriptorsDirNormalized() { return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "descriptors/"); }


    public String getScriptsDirNormalized() { return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "scripts/"); }

    public String getConfigDirNormalized() { return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "config/"); }

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


