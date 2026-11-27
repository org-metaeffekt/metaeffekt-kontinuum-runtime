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
import org.metaeffekt.kontinuum.runtime.models.shared.PortfolioManagerOptions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrepareStageHandlerTest {

    private static PipelineConfiguration pipelineConfigurationWithPortfolioManager;
    private static PipelineConfiguration pipelineConfigurationWithoutPortfolioManager;
    private static LocalConfiguration localConfigurationWithPortfolioManager;

    @BeforeAll
    public static void setUp() {
        pipelineConfigurationWithPortfolioManager = TestUtils.buildMinimalPipelineConfiguration();
        pipelineConfigurationWithoutPortfolioManager = TestUtils.buildMinimalPipelineConfiguration();

        PipelineConfiguration.PortfolioManager portfolioManager = new PipelineConfiguration.PortfolioManager();
        portfolioManager.setProject("test-project");
        pipelineConfigurationWithPortfolioManager.setPortfolioManager(portfolioManager);

        localConfigurationWithPortfolioManager = LocalConfiguration.builder()
                .KONTINUUM_DIR(
                        "~/Projects/metaeffekt/metaeffekt-kontinuum")
                .SCAN_PROPERTIES_FILE(
                        "config/scan/scan-control.properties")
                .WORKBENCH_DIR("workbench/")
                .WORKSPACE_DIR("./workspace")
                .portfolioManagerOptions(new PortfolioManagerOptions())
                .build();
    }

    @Test
    public void testPortfolioManagerForceDisabled() {
        OverwriteOptions overwriteOptions = new OverwriteOptions();
        overwriteOptions.setForceDisablePortfolioManager(true);
        LocalConfiguration localConfiguration = localConfigurationWithPortfolioManager.toBuilder()
                .overwriteOptions(overwriteOptions)
                .build();
        Pipeline pipeline = new Pipeline(pipelineConfigurationWithPortfolioManager, localConfiguration);
        PipelineExecution pipelineExecution = pipeline.generatePipeline();

        boolean containsPortfolioUploadProcessor = pipelineExecution.getAllProcessors()
                .stream()
                .anyMatch(p -> p.getId().equals(DefaultProcessorCatalog.ProcessorIds.PORTFOLIO_UPLOAD.getValue()));
        assertFalse(containsPortfolioUploadProcessor);

        boolean containsPortfolioDownloadProcessor = pipelineExecution.getAllProcessors()
                .stream()
                .anyMatch(p -> p.getId().equals(DefaultProcessorCatalog.ProcessorIds.PORTFOLIO_DOWNLOAD.getValue()));
        assertFalse(containsPortfolioDownloadProcessor);

    }

    @Test
    public void testPortfolioManagerEnabled() {
        Pipeline pipeline = new Pipeline(pipelineConfigurationWithPortfolioManager, localConfigurationWithPortfolioManager);
        PipelineExecution pipelineExecution = pipeline.generatePipeline();

        boolean containsPortfolioUploadProcessor = pipelineExecution.getAllProcessors()
                .stream()
                .anyMatch(p -> p.getId().equals(DefaultProcessorCatalog.ProcessorIds.PORTFOLIO_UPLOAD.getValue()));
        assertTrue(containsPortfolioUploadProcessor);

        boolean containsPortfolioDownloadProcessor = pipelineExecution.getAllProcessors()
                .stream()
                .anyMatch(p -> p.getId().equals(DefaultProcessorCatalog.ProcessorIds.PORTFOLIO_DOWNLOAD.getValue()));
        assertTrue(containsPortfolioDownloadProcessor);
    }

    @Test
    public void testPortfolioManagerDisabled() {
        Pipeline pipeline = new Pipeline(pipelineConfigurationWithoutPortfolioManager, localConfigurationWithPortfolioManager);
        PipelineExecution pipelineExecution = pipeline.generatePipeline();

        boolean containsPortfolioUploadProcessor = pipelineExecution.getAllProcessors()
                .stream()
                .anyMatch(p -> p.getId().equals(DefaultProcessorCatalog.ProcessorIds.PORTFOLIO_UPLOAD.getValue()));
        assertFalse(containsPortfolioUploadProcessor);

        boolean containsPortfolioDownloadProcessor = pipelineExecution.getAllProcessors()
                .stream()
                .anyMatch(p -> p.getId().equals(DefaultProcessorCatalog.ProcessorIds.PORTFOLIO_DOWNLOAD.getValue()));
        assertFalse(containsPortfolioDownloadProcessor);
    }
}