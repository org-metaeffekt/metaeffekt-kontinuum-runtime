package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.generator.shared.Pipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineExecution;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog;
import org.metaeffekt.kontinuum.runtime.models.shared.OverwriteOptions;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveStageHandlerTest {

    private static PipelineConfiguration pipelineConfiguration;
    private static LocalConfiguration localConfiguration;

    @BeforeAll
    public static void setUp() {
        pipelineConfiguration = TestUtils.buildMinimalPipelineConfiguration();
        localConfiguration = TestUtils.buildMinimalLocalConfiguration();
    }

    @Test
    public void testLocalResolveEnabled() {
        OverwriteOptions overwriteOptions = new OverwriteOptions();
        overwriteOptions.setForceEnableLocalResolve(true);
        LocalConfiguration localConfiguration = ResolveStageHandlerTest.localConfiguration.toBuilder()
                .overwriteOptions(overwriteOptions)
                .build();
        Pipeline pipeline = new Pipeline(pipelineConfiguration, localConfiguration);
        PipelineExecution pipelineExecution = pipeline.generatePipeline();
        boolean containsResolveProcessor = pipelineExecution.getAllProcessors()
                .stream()
                .anyMatch(p -> p.getId().equals(DefaultProcessorCatalog.ProcessorIds.RESOLVE_INVENTORY.getValue()));
        assertTrue(containsResolveProcessor);
    }

    @Test
    public void testLocalResolveDisabled() {
        Pipeline pipeline = new Pipeline(pipelineConfiguration, localConfiguration);
        PipelineExecution pipelineExecution = pipeline.generatePipeline();
        boolean containsResolveProcessor = pipelineExecution.getAllProcessors()
                .stream()
                .anyMatch(p -> p.getId().equals(DefaultProcessorCatalog.ProcessorIds.RESOLVE_INVENTORY.getValue()));
        assertFalse(containsResolveProcessor);
    }
}