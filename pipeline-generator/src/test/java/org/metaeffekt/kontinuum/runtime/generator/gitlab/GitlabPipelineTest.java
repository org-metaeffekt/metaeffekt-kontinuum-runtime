package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.SupportedLocale;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitlabPipelineTest {

    @Test
    void processorsOfTheSameStageAreCombinedIntoOneJob() {
        PipelineConfiguration configuration = buildPipelineConfiguration();

        GitlabConfiguration environment = GitlabConfiguration.builder()
                .KONTINUUM_DIR("kontinuum")
                .CONTAINER_IMAGE("runtime-image")
                .build();
        String pipeline = new GitlabPipeline(configuration, environment).generatePipeline();

        assertEquals(1, countJobsForStage(pipeline, "PREPARE"), pipeline);
        assertTrue(pipeline.contains("advise/advise_attach-metadata.xml"), pipeline);
        assertTrue(pipeline.contains("util/util_enrich-with-reference.xml"), pipeline);

        assertEquals(1, countJobsForStage(pipeline, "REPORT"), pipeline);
        assertTrue(pipeline.contains("report/report_create-document.xml"), pipeline);
    }

    @Test
    void generatesScriptThatCopiesContainerPatchesToTheirAbsolutePaths() {
        GitlabConfiguration environment = GitlabConfiguration.builder()
                .KONTINUUM_DIR("kontinuum")
                .CONTAINER_IMAGE("runtime-image")
                .CONTAINER_PATCH_DIRECTORY("$CI_PROJECT_DIR/container-patches/")
                .build();

        String pipeline = new GitlabPipeline(buildPipelineConfiguration(), environment).generatePipeline();

        assertTrue(pipeline.contains("      cp -r \"$CI_PROJECT_DIR/container-patches/.\" /"), pipeline);
    }

    private static PipelineConfiguration buildPipelineConfiguration() {
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
        return configuration;
    }

    private static long countJobsForStage(String pipeline, String stage) {
        return pipeline.lines()
                .filter(line -> line.equals("  stage: " + stage))
                .count();
    }
}
