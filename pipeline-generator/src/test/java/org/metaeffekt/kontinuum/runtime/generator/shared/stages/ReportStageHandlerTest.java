package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.generator.local.LocalPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.generator.shared.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportStageHandlerTest {

    @Test
    public void testMultipleLanguagesInReport() {
        PipelineConfiguration config = TestUtils.buildMinimalPipelineConfiguration();
        config.getProjectProperties().getProject().setTenant("metaeffekt");
        config.getProjectProperties().getAssets().get(0).setAssessmentId("metaeffekt");
        config.getProjectProperties().getAssets().get(0).setContext("local");
        config.getOptions().getEnrichment().setSecurityPolicyFile("policies/security-policy.json");

        PipelineConfiguration.Report report = new PipelineConfiguration.Report();
        report.setAssetIds(List.of("asset-id"));
        report.setTypes(List.of("VR", "SDA"));
        report.setLanguages(List.of("en", "de"));
        report.setOrganization("metaeffekt");
        config.setReports(List.of(report));

        LocalConfiguration localConfig = TestUtils.buildMinimalLocalConfiguration();
        LocalPipeline localPipeline = new LocalPipeline(config, localConfig);
        String script = localPipeline.generatePipeline();

        assertTrue(script.contains("-Dparam.document.language='en'"));
        assertTrue(script.contains("-Dparam.document.language='de'"));
        assertTrue(script.contains("-Dinput.document.en.pdf.file="));
        assertTrue(script.contains("-Dinput.document.de.pdf.file="));
    }

    @Test
    public void testSingleGermanLanguageInReport() {
        PipelineConfiguration config = TestUtils.buildMinimalPipelineConfiguration();
        PipelineConfiguration.Report report = new PipelineConfiguration.Report();
        report.setAssetIds(List.of("asset-id"));
        report.setTypes(List.of("SDA"));
        report.setLanguages(List.of("de"));
        config.setReports(List.of(report));

        LocalConfiguration localConfig = TestUtils.buildMinimalLocalConfiguration();
        LocalPipeline localPipeline = new LocalPipeline(config, localConfig);
        String script = localPipeline.generatePipeline();

        assertTrue(script.contains("-Dparam.document.language='de'"));
        assertTrue(script.contains("-Dinput.document.de.pdf.file="));
        assertFalse(script.contains("-Dinput.document.en.pdf.file="));
    }

    @Test
    public void testValidationWithLanguages() {
        PipelineConfigurationLoader loader = new PipelineConfigurationLoader();
        PipelineConfiguration config = loader.readConfig(new File("src/test/resources/valid-pipeline-config.yaml"));

        assertNotNull(config);
        assertNotNull(config.getReports());
        assertEquals(2, config.getReports().size());
        assertEquals(List.of("en"), config.getReports().get(0).getLanguages());
        assertEquals(List.of("de"), config.getReports().get(1).getLanguages());
    }
}
