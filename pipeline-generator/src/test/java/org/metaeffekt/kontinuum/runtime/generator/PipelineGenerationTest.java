package org.metaeffekt.kontinuum.runtime.generator;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.generator.gitlab.GitlabPipeline;
import org.metaeffekt.kontinuum.runtime.generator.local.LocalPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class PipelineGenerationTest {

    private PipelineConfiguration loadValidConfig() {
        return new PipelineConfigurationLoader().readConfig(new File("src/test/resources/valid-pipeline-config.yaml"));
    }

    @Test
    public void testLocalPipelineWithLocalMavenRepo() {
        PipelineConfiguration config = loadValidConfig();
        LocalConfiguration localConfig = LocalConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .LOCAL_MAVEN_REPO("/root/.m2/repository")
                .build();

        LocalPipeline pipeline = new LocalPipeline(config, localConfig);
        String script = pipeline.generatePipeline();

        assertTrue(script.contains("-Dmaven.repo.local=/root/.m2/repository"));
    }

    @Test
    public void testLocalPipelineWithoutLocalMavenRepo() {
        PipelineConfiguration config = loadValidConfig();
        LocalConfiguration localConfig = LocalConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .build();

        LocalPipeline pipeline = new LocalPipeline(config, localConfig);
        String script = pipeline.generatePipeline();

        assertFalse(script.contains("-Dmaven.repo.local="));
        assertFalse(script.contains("null"));
    }

    @Test
    public void testGitlabPipelineWithLocalMavenRepo() {
        PipelineConfiguration config = loadValidConfig();
        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .LOCAL_MAVEN_REPO("/root/.m2/repository")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        assertTrue(gitlabYaml.contains("-Dmaven.repo.local=/root/.m2/repository"));
    }

    @Test
    public void testGitlabPipelineWithoutLocalMavenRepo() {
        PipelineConfiguration config = loadValidConfig();
        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        assertFalse(gitlabYaml.contains("-Dmaven.repo.local="));
        assertFalse(gitlabYaml.contains("null"));
    }

    @Test
    public void testGitlabPipelineJobNamesUniquenessAndDiscriminators() {
        PipelineConfiguration config = loadValidConfig();
        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Extract top-level job names (lines starting with non-whitespace ending with ':')
        Pattern topLevelKeyPattern = Pattern.compile("^(?!stages|variables|default)([a-zA-Z0-9_.-]+):$", Pattern.MULTILINE);
        Matcher matcher = topLevelKeyPattern.matcher(gitlabYaml);

        Set<String> jobNames = new HashSet<>();
        while (matcher.find()) {
            String jobName = matcher.group(1);
            assertFalse(jobNames.contains(jobName), "Duplicate job name found in generated GitLab pipeline: " + jobName);
            jobNames.add(jobName);
        }

        assertFalse(jobNames.isEmpty(), "Expected at least one job name to be generated");

        // Verify document discriminators are present for create-document
        assertTrue(jobNames.stream().anyMatch(name -> name.contains("create-document-REPORT-VR-en")));
        assertTrue(jobNames.stream().anyMatch(name -> name.contains("create-document-REPORT-LD-en")));
        assertTrue(jobNames.stream().anyMatch(name -> name.contains("create-document-REPORT-CR-de")));
    }

    @Test
    public void testGitlabPipelineExtractStageNeedsChain() {
        PipelineConfiguration config = loadValidConfig();
        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Check that attach-metadata depends on scan-directory
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-attach-metadata-EXTRACT:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-scan-directory-EXTRACT]"));

        // Check that enrich-with-reference depends on attach-metadata
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-enrich-with-reference-EXTRACT:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-attach-metadata-EXTRACT]"));
    }

    @Test
    public void testGitlabPipelinePrepareStageWithPortfolioManager() {
        PipelineConfiguration config = loadValidConfig();
        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Portfolio upload depends on the last processor of EXTRACT (enrich-with-reference)
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-portfolio-upload-PREPARE:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-enrich-with-reference-EXTRACT]"));

        // Portfolio download depends on portfolio upload
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-portfolio-download-PREPARE:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-portfolio-upload-PREPARE]"));

        // copy-inventory should not run since portfolioManager is configured
        assertFalse(gitlabYaml.contains("copy-inventory-PREPARE"));
    }

    @Test
    public void testGitlabPipelinePrepareStageFallbackCopy() {
        PipelineConfiguration config = loadValidConfig();
        // Disable portfolio manager and bom conversions
        config.setPortfolioManager(null);
        config.getOptions().getGlobal().setEnableCycloneDxBom(false);
        config.getOptions().getGlobal().setEnableSpdxBom(false);

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // copy-inventory should run as fallback and depend on enrich-with-reference
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-copy-inventory-PREPARE:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-enrich-with-reference-EXTRACT]"));
        assertFalse(gitlabYaml.contains("portfolio-upload-PREPARE"));
        assertFalse(gitlabYaml.contains("portfolio-download-PREPARE"));
    }

    @Test
    public void testGitlabPipelinePrepareStageCycloneDxAndSpdx() {
        PipelineConfiguration config = loadValidConfig();
        config.setPortfolioManager(null);
        config.getOptions().getGlobal().setEnableCycloneDxBom(true);
        config.getOptions().getGlobal().setEnableSpdxBom(true);

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Both CycloneDX and SPDX should depend on EXTRACT enrich-with-reference
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-inventory-to-cyclonedx-PREPARE:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-inventory-to-spdx-PREPARE:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-enrich-with-reference-EXTRACT]"));

        // No copy-inventory because other processors are running
        assertFalse(gitlabYaml.contains("copy-inventory-PREPARE"));
    }

    @Test
    public void testGitlabPipelineAggregateStageWithPortfolioManager() {
        PipelineConfiguration config = loadValidConfig();
        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // enrich-with-reference in AGGREGATE depends on portfolio-download
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-enrich-with-reference-AGGREGATE:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-portfolio-download-PREPARE, alacritty-0.17.0-enrich-with-reference-EXTRACT]")
                || gitlabYaml.contains("needs: [alacritty-0.17.0-enrich-with-reference-EXTRACT, alacritty-0.17.0-portfolio-download-PREPARE]"));

        // execute-kotlin-script in AGGREGATE depends on enrich-with-reference-AGGREGATE
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-execute-kotlin-script-AGGREGATE:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-enrich-with-reference-AGGREGATE]"));
    }

    @Test
    public void testGitlabPipelineAggregateStageWithoutPortfolioManager() {
        PipelineConfiguration config = loadValidConfig();
        config.setPortfolioManager(null);

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // No processors in AGGREGATE should run when portfolio manager is not configured
        assertFalse(gitlabYaml.contains("-AGGREGATE:"));
    }

    @Test
    public void testGitlabPipelineResolveStageConditional() {
        PipelineConfiguration config = loadValidConfig();
        config.getOptions().getGlobal().setEnableResolve(true);

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        assertTrue(gitlabYaml.contains("alacritty-0.17.0-resolve-inventory-RESOLVE:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-execute-kotlin-script-AGGREGATE, alacritty-0.17.0-enrich-with-reference-EXTRACT]")
                || gitlabYaml.contains("needs: [alacritty-0.17.0-enrich-with-reference-EXTRACT, alacritty-0.17.0-execute-kotlin-script-AGGREGATE]"));

        // When disabled
        config.getOptions().getGlobal().setEnableResolve(false);
        GitlabPipeline disabledPipeline = new GitlabPipeline(config, gitlabConfig);
        assertFalse(disabledPipeline.generatePipeline().contains("-RESOLVE:"));
    }

    @Test
    public void testGitlabPipelineScanStageConditional() {
        PipelineConfiguration config = loadValidConfig();
        config.getOptions().getGlobal().setEnableScan(true);

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        assertTrue(gitlabYaml.contains("alacritty-0.17.0-scan-inventory-SCAN:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-execute-kotlin-script-AGGREGATE, alacritty-0.17.0-enrich-with-reference-EXTRACT]")
                || gitlabYaml.contains("needs: [alacritty-0.17.0-enrich-with-reference-EXTRACT, alacritty-0.17.0-execute-kotlin-script-AGGREGATE]"));

        // When disabled
        config.getOptions().getGlobal().setEnableScan(false);
        GitlabPipeline disabledPipeline = new GitlabPipeline(config, gitlabConfig);
        assertFalse(disabledPipeline.generatePipeline().contains("-SCAN:"));
    }

    @Test
    public void testGitlabPipelineAdviseStageConditional() {
        PipelineConfiguration config = loadValidConfig();
        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // In valid config, alacritty has VR report and dashboard, so ADVISE runs with dependencies
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-enrich-inventory-ADVISE:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-execute-kotlin-script-AGGREGATE"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-enrich-with-reference-EXTRACT"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-download-index-PRE"));

        // Per-asset test: remove dashboards and change alacritty report to LD only (no vuln enrichment required)
        config.setDashboards(null);
        config.getReports().get(0).setTypes(java.util.List.of("LD"));

        // Alacritty no longer requires vulnerability enrichment
        assertFalse(config.requiresVulnerabilityEnrichment("alacritty-0.17.0"));
        // But spdx still has CR which requires vulnerability enrichment
        assertTrue(config.requiresVulnerabilityEnrichment("spdx"));

        GitlabPipeline perAssetPipeline = new GitlabPipeline(config, gitlabConfig);
        String perAssetYaml = perAssetPipeline.generatePipeline();

        assertFalse(perAssetYaml.contains("alacritty-0.17.0-enrich-inventory-ADVISE:"));
        assertTrue(perAssetYaml.contains("spdx-java-library-2.0.1-enrich-inventory-ADVISE:"));
    }

    @Test
    public void testGitlabPipelineGroupStageCopyInventoryAndApplyBusinessCase() {
        PipelineConfiguration config = loadValidConfig();
        // Configure SDA report for alacritty
        config.getReports().get(0).setTypes(java.util.List.of("SDA"));

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Check copy-inventory job in GROUP stage
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-copy-inventory-GROUP:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-enrich-inventory-ADVISE, alacritty-0.17.0-enrich-with-reference-EXTRACT]"));

        // Check apply-business-case job in GROUP stage and that it depends on copy-inventory
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-apply-business-case-GROUP-en_US:"));
        assertTrue(gitlabYaml.contains("needs: [alacritty-0.17.0-copy-inventory-GROUP]"));
    }

    @Test
    public void testGitlabPipelineGroupStageMultiAssetConsolidation() {
        PipelineConfiguration config = loadValidConfig();
        // Configure a single report entry with multiple asset IDs
        PipelineConfiguration.Report multiReport = new PipelineConfiguration.Report();
        multiReport.setAssetIds(java.util.List.of("alacritty-0.17.0", "spdx"));
        multiReport.setTypes(java.util.List.of("VR"));
        multiReport.setLocales(java.util.List.of(org.metaeffekt.kontinuum.runtime.models.shared.SupportedLocale.EN_US));
        multiReport.setOrganization("metaeffekt");
        multiReport.setWatermark("Sample");
        config.setReports(java.util.List.of(multiReport));

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Both assets copy into the same consolidated grouped directory
        assertTrue(gitlabYaml.contains("07_grouped/alacritty-0.17.0-spdx/vulnerability-report/en_US/alacritty-0.17.0.xlsx"));
        assertTrue(gitlabYaml.contains("07_grouped/alacritty-0.17.0-spdx/vulnerability-report/en_US/spdx-java-library-2.0.1.xlsx"));
    }

    @Test
    public void testGitlabPipelineReportStageSoftwareDistributionAnnexChain() {
        PipelineConfiguration config = loadValidConfig();
        config.getReports().get(0).setTypes(java.util.List.of("SDA"));
        config.setDashboards(null);

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Check SDA chain: source-aggregation -> sda generation -> license aggregation -> annex archive creation
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-aggregate-sources-REPORT:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-create-document-REPORT-SDA-en:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-aggregate-licenses-REPORT:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-create-annex-archive-REPORT:"));

        // SDA document generation depends on source aggregation
        int sdaDocIndex = gitlabYaml.indexOf("alacritty-0.17.0-create-document-REPORT-SDA-en:");
        String sdaDocSnippet = gitlabYaml.substring(sdaDocIndex, gitlabYaml.indexOf("script:", sdaDocIndex));
        assertTrue(sdaDocSnippet.contains("needs: [alacritty-0.17.0-aggregate-sources-REPORT]"));

        // License aggregation depends on SDA document generation
        int licAggIndex = gitlabYaml.indexOf("alacritty-0.17.0-aggregate-licenses-REPORT:");
        String licAggSnippet = gitlabYaml.substring(licAggIndex, gitlabYaml.indexOf("script:", licAggIndex));
        assertTrue(licAggSnippet.contains("needs: [alacritty-0.17.0-create-document-REPORT-SDA-en]"));

        // Annex archive creation depends on license aggregation (and upstream)
        int annexIndex = gitlabYaml.indexOf("alacritty-0.17.0-create-annex-archive-REPORT:");
        String annexSnippet = gitlabYaml.substring(annexIndex, gitlabYaml.indexOf("script:", annexIndex));
        assertTrue(annexSnippet.contains("alacritty-0.17.0-aggregate-licenses-REPORT"));
    }

    @Test
    public void testGitlabPipelineReportStageLicenseDocumentationChain() {
        PipelineConfiguration config = loadValidConfig();
        config.getReports().get(0).setTypes(java.util.List.of("LD"));
        config.setDashboards(null);

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Check LD chain: source-aggregation -> LD generation -> license aggregation (no annex archive)
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-aggregate-sources-REPORT:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-create-document-REPORT-LD-en:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-aggregate-licenses-REPORT:"));
        assertFalse(gitlabYaml.contains("alacritty-0.17.0-create-annex-archive-REPORT:"));

        // LD document generation depends on source aggregation
        int ldDocIndex = gitlabYaml.indexOf("alacritty-0.17.0-create-document-REPORT-LD-en:");
        String ldDocSnippet = gitlabYaml.substring(ldDocIndex, gitlabYaml.indexOf("script:", ldDocIndex));
        assertTrue(ldDocSnippet.contains("needs: [alacritty-0.17.0-aggregate-sources-REPORT]"));

        // License aggregation depends on LD document generation
        int licAggIndex = gitlabYaml.indexOf("alacritty-0.17.0-aggregate-licenses-REPORT:");
        String licAggSnippet = gitlabYaml.substring(licAggIndex, gitlabYaml.indexOf("script:", licAggIndex));
        assertTrue(licAggSnippet.contains("needs: [alacritty-0.17.0-create-document-REPORT-LD-en]"));
    }

    @Test
    public void testGitlabPipelineReportStageInitialLicenseDocumentationChain() {
        PipelineConfiguration config = loadValidConfig();
        config.getReports().get(0).setTypes(java.util.List.of("ILD"));
        config.setDashboards(null);

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Check ILD chain: ILD generation -> license aggregation (no source aggregation, no annex archive)
        assertFalse(gitlabYaml.contains("alacritty-0.17.0-aggregate-sources-REPORT:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-create-document-REPORT-ILD-en:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-aggregate-licenses-REPORT:"));
        assertFalse(gitlabYaml.contains("alacritty-0.17.0-create-annex-archive-REPORT:"));

        // License aggregation depends on ILD document generation
        int licAggIndex = gitlabYaml.indexOf("alacritty-0.17.0-aggregate-licenses-REPORT:");
        String licAggSnippet = gitlabYaml.substring(licAggIndex, gitlabYaml.indexOf("script:", licAggIndex));
        assertTrue(licAggSnippet.contains("needs: [alacritty-0.17.0-create-document-REPORT-ILD-en]"));
    }

    @Test
    public void testGitlabPipelineReportStageIndependentDashboardsAndReports() {
        PipelineConfiguration config = loadValidConfig();
        // Only VR report for alacritty
        config.getReports().get(0).setTypes(java.util.List.of("VR"));

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // VR only: report generation with no other report processors
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-create-document-REPORT-VR-en:"));
        assertFalse(gitlabYaml.contains("alacritty-0.17.0-aggregate-sources-REPORT:"));
        assertFalse(gitlabYaml.contains("alacritty-0.17.0-aggregate-licenses-REPORT:"));
        assertFalse(gitlabYaml.contains("alacritty-0.17.0-create-annex-archive-REPORT:"));

        // Dashboard runs separately and depends on ADVISE stage
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-create-dashboard-REPORT:"));
        int dashIndex = gitlabYaml.indexOf("alacritty-0.17.0-create-dashboard-REPORT:");
        String dashSnippet = gitlabYaml.substring(dashIndex, gitlabYaml.indexOf("script:", dashIndex));
        assertTrue(dashSnippet.contains("needs: [alacritty-0.17.0-enrich-inventory-ADVISE]"));
    }

    @Test
    public void testGitlabPipelineSummarizeStageBoms() {
        PipelineConfiguration config = loadValidConfig();
        PipelineConfiguration.Options options = config.getOptions();
        PipelineConfiguration.Options.GlobalOptions globalOptions = new PipelineConfiguration.Options.GlobalOptions();
        globalOptions.setEnableCycloneDxBom(true);
        globalOptions.setEnableSpdxBom(true);
        options.setGlobal(globalOptions);

        GitlabConfiguration gitlabConfig = GitlabConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .RUNNER_TAG("local")
                .CONTAINER_IMAGE("metaeffekt/metaeffekt-kontinuum-runtime:2.1.0")
                .build();

        GitlabPipeline pipeline = new GitlabPipeline(config, gitlabConfig);
        String gitlabYaml = pipeline.generatePipeline();

        // Check SUMMARIZE stage jobs are generated
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-inventory-to-cyclonedx-SUMMARIZE:"));
        assertTrue(gitlabYaml.contains("alacritty-0.17.0-inventory-to-spdx-SUMMARIZE:"));

        // Verify no needs are set in summarize stage (no dependencies)
        int cdxIndex = gitlabYaml.indexOf("alacritty-0.17.0-inventory-to-cyclonedx-SUMMARIZE:");
        String cdxSnippet = gitlabYaml.substring(cdxIndex, gitlabYaml.indexOf("script:", cdxIndex));
        assertFalse(cdxSnippet.contains("needs:"));

        int spdxIndex = gitlabYaml.indexOf("alacritty-0.17.0-inventory-to-spdx-SUMMARIZE:");
        String spdxSnippet = gitlabYaml.substring(spdxIndex, gitlabYaml.indexOf("script:", spdxIndex));
        assertFalse(spdxSnippet.contains("needs:"));
    }
}
