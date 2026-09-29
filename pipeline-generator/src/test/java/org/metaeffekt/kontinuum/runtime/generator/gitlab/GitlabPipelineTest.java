package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.SupportedLocale;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GitlabPipelineTest {

    @Test
    void reportJobNeedsIncludeProcessorsFromMemberAssetContexts() {
        PipelineConfiguration configuration = TestUtils.buildMinimalPipelineConfiguration();
        PipelineConfiguration.ProjectProperties.Asset asset = configuration.getProjectProperties().getAssets().get(0);
        asset.setAssessmentId("assessment");
        asset.setContext("project-context");

        PipelineConfiguration.Report report = new PipelineConfiguration.Report();
        report.setId("release");
        report.setAssetIds(List.of(asset.getId()));
        report.setTypes(List.of("VR"));
        report.setLocales(List.of(SupportedLocale.EN_US));
        report.setProductName("product");
        report.setProductVersion("1.0.0");
        report.setProductWatermark("watermark");
        report.setOrganization("metaeffekt");
        report.setClassificationRating("DEFAULT");
        report.setControlRating("DEFAULT");
        configuration.setReports(List.of(report));
        configuration.getOptions().getEnrichment().setSecurityPolicyFile("policies/security-policy.json");

        GitlabConfiguration environment = GitlabConfiguration.builder()
                .KONTINUUM_DIR("kontinuum")
                .CONTAINER_IMAGE("runtime-image")
                .build();
        String pipeline = new GitlabPipeline(configuration, environment).generatePipeline();

        String reportJob = pipeline.lines()
                .filter(line -> line.startsWith("release-VR-create-document-REPORT"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Generated report job was not found."));
        int reportJobStart = pipeline.indexOf(reportJob);
        int reportJobEnd = pipeline.indexOf("\n\n", reportJobStart);
        String reportJobBlock = pipeline.substring(reportJobStart,
                                                   reportJobEnd < 0 ? pipeline.length() : reportJobEnd);

        assertTrue(reportJobBlock.contains("needs: [" + asset.getId() + "-copy-inventory-GROUP"), reportJobBlock);
    }
}
