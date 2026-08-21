package org.metaeffekt.kontinuum.runtime.generator.shared;

import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.util.List;

import static org.metaeffekt.kontinuum.runtime.generator.shared.TestUtils.TestUtilParams.*;

public class TestUtils {


    enum TestUtilParams{

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


    public static PipelineConfiguration buildMinimalPipelineConfiguration() {
        PipelineConfiguration pipelineConfiguration = new PipelineConfiguration();

        PipelineConfiguration.ProjectProperties projectProperties = new PipelineConfiguration.ProjectProperties();

        PipelineConfiguration.ProjectProperties.Project project = new PipelineConfiguration.ProjectProperties.Project();
        project.setId(PROJECT_ID.value);
        project.setName(PROJECT_NAME.value);
        project.setVersion(PROJECT_VERSION.value);

        PipelineConfiguration.ProjectProperties.Asset asset = new PipelineConfiguration.ProjectProperties.Asset();
        asset.setId(ASSET_ID.value);
        asset.setName(ASSET_NAME.value);
        asset.setVersion(ASSET_VERSION.value);
        asset.setReference(ASSET_REFERENCE_INVENTORY.value);

        PipelineConfiguration.ProjectProperties.Asset.UrlResolver urlResolver = new PipelineConfiguration.ProjectProperties.Asset.UrlResolver();
        urlResolver.setUrl(URL_RESOLVER_URL.value);

        asset.setUrlResolver(urlResolver);
        projectProperties.setAssets(List.of(asset));
        projectProperties.setProject(project);
        pipelineConfiguration.setProjectProperties(projectProperties);

        return  pipelineConfiguration;
    }

    public static LocalConfiguration buildMinimalLocalConfiguration() {
        LocalConfiguration localConfiguration = LocalConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .build();

        return localConfiguration;
    }
}
