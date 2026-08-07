package org.metaeffekt.kontinuum.runtime.generator.shared;

import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.util.List;

public class TestUtils {


    public static PipelineConfiguration buildMinimalPipelineConfiguration() {
        PipelineConfiguration pipelineConfiguration = new PipelineConfiguration();

        PipelineConfiguration.ProjectProperties projectProperties = new PipelineConfiguration.ProjectProperties();

        PipelineConfiguration.ProjectProperties.Project project = new PipelineConfiguration.ProjectProperties.Project();
        project.setName("project-name");
        project.setVersion("1.0.0");

        PipelineConfiguration.ProjectProperties.Asset asset = new PipelineConfiguration.ProjectProperties.Asset();
        asset.setId("asset-id");
        asset.setName("asset-name");
        asset.setVersion("1.0.0");
        asset.setReference("src/test/resources/reference-inventory.xls");

        PipelineConfiguration.ProjectProperties.Asset.UrlResolver urlResolver = new PipelineConfiguration.ProjectProperties.Asset.UrlResolver();
        urlResolver.setUrl("https://test-url.com");

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
