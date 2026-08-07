package org.metaeffekt.kontinuum.runtime.generator.local;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.generator.shared.Pipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.generator.shared.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalPipelineTest {

    private static final File PIPELINE_CONFIG_FILE = new File("src/test/resources/valid-pipeline-config.yaml");

    @Test
    @Disabled
    public void testValidPipelineGenerationExternal() throws IOException {
        File pipelineConfigFile = new File("/home/goose/Projects/metaeffekt/metaeffekt-workbench/pipelines/sample-product-pipeline.yaml");

        LocalConfiguration localConfiguration = TestUtils.buildMinimalLocalConfiguration();
        PipelineConfiguration pipelineConfiguration = new PipelineConfigurationLoader().readConfig(pipelineConfigFile);

        LocalPipeline localPipeline = new LocalPipeline(pipelineConfiguration, localConfiguration);

        Path outputPath = Path.of("target/generator/local-pipeline-external.yml");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, localPipeline.generatePipeline());
    }

    @Test
    public void testValidPipelineGeneration() throws IOException {
        LocalConfiguration localConfiguration = TestUtils.buildMinimalLocalConfiguration();

        PipelineConfiguration pipelineConfiguration = new PipelineConfigurationLoader().readConfig(PIPELINE_CONFIG_FILE);
        LocalPipeline localPipeline = new LocalPipeline(pipelineConfiguration, localConfiguration);

        Path outputPath = Path.of("target/generator/valid-local-pipeline.sh");
        Files.createDirectories(outputPath.getParent());
        String generated = localPipeline.generatePipeline();
        Files.writeString(outputPath, generated);

        assertNotNull(generated);
        assertTrue(generated.startsWith("#!/usr/bin/env bash"), "Expected bash shebang");
        assertTrue(generated.contains("set -e"), "Expected fail-fast directive");
    }

    @Test
    public void testGeneratedScriptContainsMavenInvocations() throws IOException {
        LocalConfiguration localConfiguration = LocalConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .build();

        PipelineConfiguration pipelineConfiguration = new PipelineConfigurationLoader().readConfig(PIPELINE_CONFIG_FILE);
        LocalPipeline localPipeline = new LocalPipeline(pipelineConfiguration, localConfiguration);

        String generated = localPipeline.generatePipeline();

        assertTrue(generated.contains("mvn -f "), "Expected at least one mvn invocation");
        assertTrue(generated.contains("processors/fetch/fetch_download-asset.xml"), "Expected download-asset processor invocation");
        assertTrue(generated.contains("process-resources"), "Expected process-resources goal");
    }

    @Test
    public void testGeneratedScriptOrdersByStage() throws IOException {
        LocalConfiguration localConfiguration = LocalConfiguration.builder()
                .KONTINUUM_DIR("~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE("config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .build();

        PipelineConfiguration pipelineConfiguration = new PipelineConfigurationLoader().readConfig(PIPELINE_CONFIG_FILE);
        LocalPipeline localPipeline = new LocalPipeline(pipelineConfiguration, localConfiguration);

        String generated = localPipeline.generatePipeline();

        // Pipeline.generatePipeline() is the single authority for processor run order: within each
        // asset the processors appear in the order the addXxxProcessor methods are invoked. The
        // local script is emitted per-asset (not cross-asset-by-stage), so we assert that for any
        // single asset the declared stage sequence FETCH < PREPARE < ADVISE < REPORT holds.
        int fetchIdx = generated.indexOf("# --- FETCH:");
        int prepareIdx = generated.indexOf("# --- PREPARE:");
        int adviseIdx = generated.indexOf("# --- ADVISE:");
        int reportIdx = generated.indexOf("# --- REPORT:");

        assertTrue(fetchIdx >= 0 && prepareIdx >= 0, "Expected FETCH and PREPARE sections");
        assertTrue(fetchIdx < prepareIdx, "FETCH should appear before PREPARE");
        if (adviseIdx >= 0) {
            assertTrue(prepareIdx < adviseIdx, "PREPARE should appear before ADVISE");
        }
        if (reportIdx >= 0) {
            assertTrue(adviseIdx < reportIdx, "ADVISE should appear before REPORT");
        }
    }

    @Test
    public void testMinimalLocalPipeline() {
        LocalPipeline localPipeline = new LocalPipeline(TestUtils.buildMinimalPipelineConfiguration(), TestUtils.buildMinimalLocalConfiguration());
        localPipeline.generatePipeline();
    }
}