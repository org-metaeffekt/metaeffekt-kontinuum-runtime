package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enumerates the report kinds a Kontinuum pipeline can generate.
 * Each constant carries the key, workspace folder and asset descriptor file used during generation.
 */
public enum ReportType {
    CERT_REPORT("CR", "cert-report", "asset-descriptor_GENERIC-cert-report.yaml"),
    CUSTOM_ANNEX("CA", "custom-annex", "asset-descriptor_GENERIC-custom-annex.yaml"),
    INITIAL_LICENSE_DOCUMENTATION("ILD", "initial-license-documentation",
                                  "asset-descriptor_GENERIC-initial-license-documentation.yaml"),
    LICENSE_DOCUMENTATION("LD", "license-documentation", "asset-descriptor_GENERIC-license-documentation.yaml"),
    SOFTWARE_DISTRIBUTION_ANNEX("SDA", "software-distribution-annex",
                                "asset-descriptor_GENERIC-software-distribution-annex.yaml"),
    VULNERABILITY_REPORT("VR", "vulnerability-report", "asset-descriptor_GENERIC-vulnerability-report.yaml"),
    VULNERABILITY_SUMMARY_REPORT("VSR", "vulnerability-summary-report",
                                 "asset-descriptor_GENERIC-vulnerability-summary-report.yaml");

    @Getter
    private final String key;

    @Getter
    private final String workspaceFolder;

    @Getter
    private final String assetDescriptorFile;

    ReportType(String key, String workspaceFolder, String assetDescriptorFile) {
        this.key = key;
        this.workspaceFolder = workspaceFolder;
        this.assetDescriptorFile = assetDescriptorFile;
    }

    /**
     * Resolves a report type from its short key.
     *
     * @param key the report type key, e.g. {@code VR}
     * @return the matching report type
     */
    public static ReportType fromKey(String key) {
        return Arrays.stream(values())
                .filter(rt -> rt.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No ReportType found for key: " + key));
    }

    /**
     * Returns all known report type keys.
     *
     * @return the set of all report type keys
     */
    public static Set<String> allKeys() {
        return Arrays.stream(values()).map(ReportType::getKey).collect(Collectors.toSet());
    }

    /**
     * Determines whether any of the given report types require a scan stage.
     *
     * @param reportTypes the report types to inspect
     * @return {@code true} if a scan is required, otherwise {@code false}
     */
    public static boolean requiresScan(ReportType... reportTypes) {
        for (ReportType reportType : reportTypes) {
            if (reportType.equals(INITIAL_LICENSE_DOCUMENTATION) || reportType.equals(
                    LICENSE_DOCUMENTATION) || reportType.equals(SOFTWARE_DISTRIBUTION_ANNEX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether any of the given report types require vulnerability enrichment.
     *
     * @param reportTypes the report types to inspect
     * @return {@code true} if vulnerability enrichment is required, otherwise {@code false}
     */
    public static boolean requiresVulnerabilityEnrichment(ReportType... reportTypes) {
        for (ReportType reportType : reportTypes) {
            if (reportType.equals(CERT_REPORT) || reportType.equals(VULNERABILITY_REPORT) || reportType.equals(
                    VULNERABILITY_SUMMARY_REPORT)) {
                return true;
            }
        }
        return false;
    }
}
