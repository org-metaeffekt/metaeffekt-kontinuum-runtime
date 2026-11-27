package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Data;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

/**
 * Portfolio Manager connection and client authentication options.
 */
@Data
public class PortfolioManagerOptions {

    private String url;
    private String token;
    private String clientKeystoreFile;
    private String clientTruststoreFile;
    private String clientKeystorePassword;
    private String clientTruststorePassword;

    /**
     * Returns they keystore file path relative to the designated workbench directory.
     * @param workbenchDir the workbench directory containing project-specific configurations.
     * @return a normalized file path relative to the workbench directory.
     */
    public String getClientKeystoreFile(String workbenchDir) {
        return KontinuumUtils.normalizeFilePath(workbenchDir, clientKeystoreFile);
    }

    /**
     * Returns they truststore file path relative to the designated workbench directory.
     * @param workbenchDir the workbench directory containing project-specific configurations.
     * @return a normalized file path relative to the workbench directory.
     */
    public String getClientTruststoreFile(String workbenchDir) {
        return KontinuumUtils.normalizeFilePath(workbenchDir, clientTruststoreFile);
    }
}
