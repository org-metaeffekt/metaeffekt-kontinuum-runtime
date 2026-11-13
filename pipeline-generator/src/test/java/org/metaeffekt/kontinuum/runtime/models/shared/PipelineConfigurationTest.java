package org.metaeffekt.kontinuum.runtime.models.shared;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PipelineConfigurationTest {

    private static final String WORKBENCH = "/opt/workbench/";

    @Test
    void referenceDirectoryIsResolvedAgainstWorkbench() {
        Asset asset = new Asset();
        asset.setReference("inventories/reference-inventory/inventory");

        assertEquals("/opt/workbench/inventories/reference-inventory/inventory/", asset.getReferenceDir(WORKBENCH));
    }

    @Test
    void referenceFileResolvesToItsParentDirectory() {
        Asset asset = new Asset();
        asset.setReference("inventories/reference-inventory/inventory/artifact-inventory.xls");

        assertEquals("/opt/workbench/inventories/reference-inventory/inventory/", asset.getReferenceDir(WORKBENCH));
    }

    @Test
    void referenceDirectoryDoesNotDependOnTheFileSystem() {
        // the directory does not exist relative to the working directory of the test; it must still be kept
        Asset asset = new Asset();
        asset.setReference("does/not/exist/inventory");

        assertEquals("/opt/workbench/does/not/exist/inventory/", asset.getReferenceDir(WORKBENCH));
    }

    @Test
    void missingReferenceIsRejected() {
        Asset asset = new Asset();

        assertThrows(IllegalStateException.class, () -> asset.getReferenceDir(WORKBENCH));
    }

    @Test
    void relativeFileUrlIsResolvedAgainstWorkbench() {
        assertEquals("file:/opt/workbench/inventories/app.xlsx", resolvedUrl("file:inventories/app.xlsx"));
        assertEquals("file:/opt/workbench/inventories/app.xlsx", resolvedUrl("FILE:inventories/app.xlsx"));
    }

    @Test
    void absoluteAndNonFileUrlsAreUnchanged() {
        assertEquals("file:/data/app.xlsx", resolvedUrl("file:/data/app.xlsx"));
        assertEquals("file:///data/app.xlsx", resolvedUrl("file:///data/app.xlsx"));
        assertEquals("file://host/data/app.xlsx", resolvedUrl("file://host/data/app.xlsx"));
        assertEquals("https://example.com/app.zip", resolvedUrl("https://example.com/app.zip"));
    }

    @Test
    void absoluteReferenceIsNotPrefixedWithWorkbench() {
        Asset asset = new Asset();
        asset.setReference("/data/reference-inventory/inventory/artifact-inventory.xls");

        assertEquals("/data/reference-inventory/inventory/", asset.getReferenceDir(WORKBENCH));
    }

    private static String resolvedUrl(String url) {
        Asset.UrlResolver urlResolver = new Asset.UrlResolver();
        urlResolver.setUrl(url);
        return urlResolver.getResolvedUrl(WORKBENCH);
    }
}
