package org.metaeffekt.kontinuum.runtime.models.local;

import lombok.Builder;
import lombok.experimental.SuperBuilder;
import org.metaeffekt.kontinuum.runtime.models.shared.EnvironmentConfiguration;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

/**
 * Environment configuration for pipeline execution on a local machine.
 */
@SuperBuilder
public class LocalConfiguration extends EnvironmentConfiguration {

    @Builder.Default
    ExecutionEnvironment executionEnvironment = ExecutionEnvironment.UNIX;

    @Override
    public String getWorkspaceDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKSPACE_DIR);
    }

    /**
     * Supported operating-system environments for local execution.
     */
    public enum ExecutionEnvironment {
        UNIX,
        WINDOWS_NT
    }
}

