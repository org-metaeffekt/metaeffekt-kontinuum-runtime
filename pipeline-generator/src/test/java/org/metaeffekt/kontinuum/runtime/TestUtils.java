package org.metaeffekt.kontinuum.runtime;

import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey;

import java.util.List;

import static org.metaeffekt.kontinuum.runtime.TestUtilParams.*;

public class TestUtils {

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
        pipelineConfiguration.setReports(List.of());
        pipelineConfiguration.setDashboards(List.of());
        pipelineConfiguration.setOptions(new PipelineConfiguration.Options());

        return pipelineConfiguration;
    }

    public static GitlabConfiguration buildMinimalGitlabConfiguration() {
        GitlabConfiguration gitlabConfiguration = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        return gitlabConfiguration;
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

    public static boolean scriptContainsParameterValue(String script, ProcessorParameterKey key, String expectedValue) {
        return script.contains("-D" + key + "=" + expectedValue);
    }
}
