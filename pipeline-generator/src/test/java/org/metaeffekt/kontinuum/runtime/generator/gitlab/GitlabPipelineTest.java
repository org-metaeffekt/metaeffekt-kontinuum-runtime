package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.ProcessorParameter;
import org.metaeffekt.kontinuum.runtime.models.shared.SupportedLocale;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class GitlabPipelineTest {

    private static final Pattern JOB_ID = Pattern.compile(
            "(?m)^(.+-)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(?=:)");

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
        assertFalse(pipeline.contains("      cp -r \"$CI_PROJECT_DIR/container-patches/.\" /\n\n"), pipeline);
    }

    @Test
    void generatesFreshDocumentOnEveryInvocation() {
        GitlabPipeline generator = new GitlabPipeline(buildPipelineConfiguration(), minimalGitlabConfiguration());

        String first = generator.generatePipeline();
        String second = generator.generatePipeline();

        assertEquals(normalizeJobIds(first), normalizeJobIds(second));
    }

    @Test
    void omitsContinuationWhenMavenProcessorHasNoParameters() {
        GitlabPipeline generator = new GitlabPipeline(buildPipelineConfiguration(), minimalGitlabConfiguration());
        allProcessors(generator).filter(MavenProcessor.class::isInstance)
                .map(MavenProcessor.class::cast)
                .forEach(processor -> processor.setParameters(List.of()));

        String pipeline = generator.generatePipeline();

        assertFalse(pipeline.contains("process-resources \\"), pipeline);
        assertTrue(pipeline.contains("process-resources\n"), pipeline);
    }

    @Test
    void quotesMavenPathsAndValuesAsLiteralShellArguments() {
        GitlabPipeline generator = new GitlabPipeline(buildPipelineConfiguration(), minimalGitlabConfiguration());
        MavenProcessor processor = allProcessors(generator)
                .filter(MavenProcessor.class::isInstance)
                .map(MavenProcessor.class::cast)
                .findFirst()
                .orElseThrow();
        processor.setPomLocation("pom with 'quote'.xml");
        ProcessorParameter parameter = processor.getParameters().stream()
                .filter(p -> p.getValue() != null)
                .findFirst()
                .orElseThrow();
        parameter.setValue("value with 'quote' and $HOME");

        String pipeline = generator.generatePipeline();

        assertTrue(pipeline.contains(
                "-f 'kontinuum/processors/pom with '\"'\"'quote'\"'\"'.xml' process-resources"), pipeline);
        assertTrue(pipeline.contains(
                "-D" + parameter.getKey() + "='value with '\"'\"'quote'\"'\"' and $HOME'"), pipeline);
    }

    @Test
    void rejectsLineBreaksInLiteralShellArguments() {
        GitlabPipeline generator = new GitlabPipeline(buildPipelineConfiguration(), minimalGitlabConfiguration());
        MavenProcessor processor = allProcessors(generator)
                .filter(MavenProcessor.class::isInstance)
                .map(MavenProcessor.class::cast)
                .findFirst()
                .orElseThrow();
        processor.getParameters().stream()
                .filter(p -> p.getValue() != null)
                .findFirst()
                .orElseThrow()
                .setValue("first line\nsecond line");

        assertThrows(IllegalArgumentException.class, generator::generatePipeline);
    }

    @Test
    void emitsMultilineScriptsWithEmitterOwnedIndentation() {
        GitlabPipeline generator = new GitlabPipeline(buildPipelineConfiguration(), minimalGitlabConfiguration());
        MavenProcessor processor = allProcessors(generator)
                .filter(MavenProcessor.class::isInstance)
                .map(MavenProcessor.class::cast)
                .findFirst()
                .orElseThrow();
        processor.setPreScript("echo pre\r\n\r\n  echo indented  \r");
        processor.setPostScript(" \n\t");

        String pipeline = generator.generatePipeline();

        assertTrue(pipeline.contains("      echo pre\n\n        echo indented  \n"), pipeline);
        assertFalse(pipeline.contains("       \n"), pipeline);
    }

    @Test
    void reproducesCleanedLog4jExample() throws IOException {
        PipelineConfiguration configuration = new PipelineConfigurationLoader().readConfig(
                Path.of("src/test/resources/gitlab-pipeline-log4j.yaml").toFile());
        String expected = Files.readString(Path.of("src/test/resources/gitlab-pipeline-log4j-expected.yml"));

        String pipeline = new GitlabPipeline(configuration, exampleGitlabConfiguration()).generatePipeline();

        assertEquals(normalizeJobIds(expected), normalizeJobIds(pipeline));
        assertDoesNotThrow(() -> new ObjectMapper(new YAMLFactory()).readTree(pipeline));
        assertTrue(pipeline.endsWith("\n"), pipeline);
        assertFalse(pipeline.endsWith("\n\n"), pipeline);
    }

    private static java.util.stream.Stream<Processor> allProcessors(GitlabPipeline generator) {
        return generator.execution.getContexts().stream().flatMap(context -> context.getProcessors().stream());
    }

    private static GitlabConfiguration minimalGitlabConfiguration() {
        return GitlabConfiguration.builder()
                .KONTINUUM_DIR("kontinuum")
                .CONTAINER_IMAGE("runtime-image")
                .build();
    }

    private static GitlabConfiguration exampleGitlabConfiguration() {
        return GitlabConfiguration.builder()
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.4.0")
                .ARTIFACT_RESOLVER_CONFIG_FILE("workbench/config/resolver/artifact-resolver-config.yaml")
                .ARTIFACT_RESOLVER_PROXY_FILE("workbench/config/resolver/artifact-resolver-proxy.yaml")
                .SCAN_PROPERTIES_FILE("workbench/config/scanner/scan-control.properties")
                .VULNERABILITY_MIRROR_DIR("/metaeffekt-gitlab-examples/mirror/")
                .VULNERABILITY_MIRROR_URL("http://ae-scanner/mirror/index/ae-mirror-index_gen7.tar.gz")
                .WORKSPACE_DIR("/metaeffekt-gitlab-examples/workspace")
                .WORKBENCH_DIR("workbench")
                .KONTINUUM_DIR("/usr/src/metaeffekt-kontinuum/")
                .PORTFOLIO_MANAGER_CLIENT_KEYSTORE_FILE("config/portfolio/pm-client-TEST-keystore.p12")
                .PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_FILE("config/portfolio/pm-client-TEST-truststore.p12")
                .PORTFOLIO_MANAGER_CLIENT_KEYSTORE_PASSWORD("client-TEST-ks-pw")
                .PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_PASSWORD("client-TEST-ts-pw")
                .PORTFOLIO_MANAGER_TOKEN("3e6c4faa-4dfc-4bae-b711-22225768274f")
                .PORTFOLIO_MANAGER_URL("https://192.168.178.100:8444")
                .MAVEN_CLI_OPTS("-X")
                .SETUP_COMMAND("echo \"Setup Command valid.\"")
                .RUNNER_TAG("examples")
                .build();
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

    private static String normalizeJobIds(String pipeline) {
        return JOB_ID.matcher(pipeline).replaceAll("$1<job-id>");
    }
}
