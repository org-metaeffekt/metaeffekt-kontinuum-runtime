package org.metaeffekt.kontinuum.runtime.models.gitlab;

import lombok.Builder;
import lombok.experimental.SuperBuilder;
import org.metaeffekt.kontinuum.runtime.models.shared.EnvironmentConfiguration;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

/**
 * Environment configuration for pipeline execution on GitLab CI.
 */
@SuperBuilder(toBuilder = true)
public class GitlabConfiguration extends EnvironmentConfiguration {

    @Builder.Default
    public int GIT_DEPTH = 1;

    @Builder.Default
    public String GIT_STRATEGY = "clone";

    public String RUNNER_TAG;

    public String CONTAINER_IMAGE;

    @Override
    public String getWorkspaceDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKSPACE_DIR);
    }
}

