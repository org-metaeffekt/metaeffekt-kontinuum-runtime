package org.metaeffekt.kontinuum.runtime.generator.shared;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.ReportType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PipelineConfigurationLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void projectNameIsRequired() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.getProjectProperties().getProject().setName(" ");
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void projectVersionIsRequired() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.getProjectProperties().getProject().setVersion(null);
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void assetsListIsRequired() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.getProjectProperties().setAssets(new ArrayList<>());
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void assetIdIsRequired() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.getProjectProperties().getAssets().get(0).setId(null);
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void assetReferenceIsRequired() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.getProjectProperties().getAssets().get(0).setReference("");
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void urlResolverRequiresValidUrlOrPattern() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.getProjectProperties().getAssets().get(0).getUrlResolver().setUrl("not-a-url");
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void urlPatternRequiresPopulatedPlaceholders() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        valid.getProjectProperties().getAssets().get(0).getUrlResolver().setUrl(null);
        valid.getProjectProperties().getAssets().get(0).getUrlResolver()
            .setUrlPattern("https://example.com/${name}-${version}.zip");
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.getProjectProperties().getAssets().get(0).getUrlResolver().setUrl(null);
        invalid.getProjectProperties().getAssets().get(0).getUrlResolver()
            .setUrlPattern("https://example.com/${name}-${version}.zip");
        invalid.getProjectProperties().getAssets().get(0).setName(null);
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void reportTypeMustBeValid() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        valid.setReports(List.of(report("asset-1", List.of(ReportType.VULNERABILITY_REPORT.getKey()))));
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.setReports(List.of(report("asset-1", List.of("INVALID"))));
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void reportAssetIdMustExist() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        valid.setReports(List.of(report("asset-1", List.of(ReportType.VULNERABILITY_REPORT.getKey()))));
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.setReports(List.of(report("missing", List.of(ReportType.VULNERABILITY_REPORT.getKey()))));
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void dashboardAssetIdMustExist() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        valid.setDashboards(List.of(dashboard("asset-1")));
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.setDashboards(List.of(dashboard("missing")));
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    @Test
    void assessmentFieldsRequiredWhenReportsOrDashboardsPresent() throws IOException {
        PipelineConfiguration valid = minimalConfig();
        valid.setReports(List.of(report("asset-1", List.of(ReportType.VULNERABILITY_REPORT.getKey()))));
        assertDoesNotThrow(() -> readConfig(valid));

        PipelineConfiguration invalid = minimalConfig();
        invalid.setReports(List.of(report("asset-1", List.of(ReportType.VULNERABILITY_REPORT.getKey()))));
        invalid.getProjectProperties().getAssets().get(0).setAssessmentId(null);
        assertThrows(IllegalStateException.class, () -> readConfig(invalid));
    }

    private PipelineConfiguration minimalConfig() {
        PipelineConfiguration configuration = new PipelineConfiguration();
        PipelineConfiguration.ProjectProperties projectProperties = new PipelineConfiguration.ProjectProperties();
        PipelineConfiguration.ProjectProperties.Project project = new PipelineConfiguration.ProjectProperties.Project();
        project.setName("example-project");
        project.setVersion("1.0.0");
        project.setTenant("example-tenant");

        PipelineConfiguration.ProjectProperties.Asset asset = new PipelineConfiguration.ProjectProperties.Asset();
        asset.setId("asset-1");
        asset.setName("asset");
        asset.setVersion("1.0.0");
        asset.setAssessmentId("asset-1");
        asset.setContext("local");
        asset.setReference("inventories/reference-inventory.xlsx");

        PipelineConfiguration.ProjectProperties.Asset.UrlResolver urlResolver =
            new PipelineConfiguration.ProjectProperties.Asset.UrlResolver();
        urlResolver.setUrl("https://example.com/asset-1.zip");
        asset.setUrlResolver(urlResolver);

        projectProperties.setProject(project);
        projectProperties.setAssets(new ArrayList<>(List.of(asset)));
        configuration.setProjectProperties(projectProperties);
        configuration.setReports(new ArrayList<>());
        configuration.setDashboards(new ArrayList<>());
        configuration.setOptions(new PipelineConfiguration.Options());
        return configuration;
    }

    private PipelineConfiguration readConfig(PipelineConfiguration configuration) throws IOException {
        File file = tempDir.resolve("pipeline-config-" + System.nanoTime() + ".yaml").toFile();
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.disable(MapperFeature.AUTO_DETECT_GETTERS);
        objectMapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS);
        objectMapper.writeValue(file, configuration);
        return new PipelineConfigurationLoader().readConfig(file);
    }

    private PipelineConfiguration.Report report(String assetId, List<String> types) {
        PipelineConfiguration.Report report = new PipelineConfiguration.Report();
        report.setAssetId(assetId);
        report.setTypes(types);
        return report;
    }

    private PipelineConfiguration.Dashboard dashboard(String assetId) {
        PipelineConfiguration.Dashboard dashboard = new PipelineConfiguration.Dashboard();
        dashboard.setAssetId(assetId);
        return dashboard;
    }
}
