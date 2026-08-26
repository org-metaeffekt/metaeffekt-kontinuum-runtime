package org.metaeffekt.kontinuum.runtime.models.gitlab;

import org.metaeffekt.kontinuum.runtime.models.shared.EnvironmentConfiguration;

import lombok.Builder;
import lombok.experimental.SuperBuilder;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

@SuperBuilder
public class GitlabConfiguration extends EnvironmentConfiguration {
    
    @Builder.Default
    public final int GIT_DEPTH = 1;
    
    @Builder.Default
    public final String GIT_STRATEGY = "CLONE";

    public final String RUNNER_TAG;

    public final String CONTAINER_IMAGE;

    @Override
    public String getWorkspaceDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKSPACE_DIR);
    }
}

