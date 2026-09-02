package org.metaeffekt.kontinuum.runtime;

public enum TestUtilParams{

    PROJECT_ID("project-id"),
    PROJECT_NAME("project-name"),
    PROJECT_VERSION("project-version"),
    ASSET_ID("asset-id"),
    ASSET_NAME("asset-name"),
    ASSET_VERSION("1.0.0"),
    ASSET_REFERENCE_INVENTORY("src/test/resources/reference-inventory.xls"),
    URL_RESOLVER_URL("https://test-url.com");

    final String value;

    TestUtilParams(String value) {
        this.value = value;
    }
}
