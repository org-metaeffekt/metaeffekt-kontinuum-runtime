package org.metaeffekt.kontinuum.runtime.models.local;

import lombok.Builder;
import lombok.experimental.SuperBuilder;
import org.metaeffekt.kontinuum.runtime.models.shared.EnvironmentConfiguration;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

@SuperBuilder
public class LocalConfiguration extends EnvironmentConfiguration {

    @Builder.Default
    ExecutionEnvironment executionEnvironment = ExecutionEnvironment.UNIX;

    @Override
    public String getWorkspaceDirNormalized() {
        return KontinuumUtils.normalizeDir(WORKSPACE_DIR);
    }

    public enum ExecutionEnvironment {
        UNIX,
        WINDOWS_NT
    }
}

