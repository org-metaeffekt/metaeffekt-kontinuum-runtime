package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

/**
 * Base configuration for a Kontinuum environment, exposing directories, credentials and processor
 * settings resolved from the harness properties.
 */
@SuperBuilder(toBuilder = true)
public abstract class EnvironmentConfiguration {

    @Builder.Default
    public final String WORKBENCH_DIR = "./workbench/";

    @Builder.Default
    public final String WORKSPACE_DIR = "./workspace/";

    @Builder.Default
    private final String VULNERABILITY_MIRROR_DIR = "./mirror/";

    private final String KONTINUUM_DIR;

    public final String VULNERABILITY_MIRROR_URL;
    public final String ARTIFACT_RESOLVER_CONFIG_FILE;
    public final String ARTIFACT_RESOLVER_PROXY_FILE;
    public final String SCAN_PROPERTIES_FILE;
    public final String SETUP_COMMAND;
    public final String MAVEN_CLI_OPTS;
    public final String LOCAL_MAVEN_REPO;
    public final String CONTAINER_PATCH_DIRECTORY;


    @Builder.Default
    public final TermsMetadataOptions termsMetadataOptions = new TermsMetadataOptions();

    @Builder.Default
    public final PortfolioManagerOptions portfolioManagerOptions = new PortfolioManagerOptions();

    @Getter
    @Builder.Default
    public final OverwriteOptions overwriteOptions = new OverwriteOptions();

    public String getMirrorDir() {
        return VULNERABILITY_MIRROR_DIR;
    }

    public String getMirrorDatabaseDirNormalized() {
        return KontinuumUtils.normalizeDir(VULNERABILITY_MIRROR_DIR, ".database");
    }

    public String getCorrelationDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "correlations/");
    }

    public String getDescriptorsDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "descriptors/");
    }


    public String getScriptsDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "scripts/");
    }

    public String getConfigDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKBENCH_DIR, "config/");
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

    /**
     * Returns the normalized workspace directory.
     *
     * @return the normalized workspace directory
     */
    public abstract String getWorkspaceDirNormalized();
}


