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
}
