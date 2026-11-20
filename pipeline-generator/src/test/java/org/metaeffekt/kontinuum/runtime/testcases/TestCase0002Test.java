package org.metaeffekt.kontinuum.runtime.testcases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metaeffekt.kontinuum.runtime.generator.gitlab.GitlabPipeline;
import org.metaeffekt.kontinuum.runtime.generator.local.LocalPipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case 0002: the period of cert reports is passed to the report processor. The configuration template is
 * resolved and loaded, the local and the GitLab pipeline are generated, and the parameters of the
 * {@code create-document} invocation of each report are checked. The pipelines are not executed, so no Kontinuum
 * checkout is required. See {@code test-cases/README.md} for the layout and the placeholders of the templates.
 */
class TestCase0002Test {

    private static final String TEST_CASE = "tc-0002";

    private static final String CONFIG_FILE_NAME = "tc-0002-01-config.yaml";

    private static final String CREATE_DOCUMENT_POM = "report/report_create-document.xml";

    private static final String PERIOD_PARAMETER_PREFIX = "-Dparam.report.period.";

    @TempDir
    Path runtimeWorkspaceDir;

    @Test
    void localPipelinePassesThePeriodOfCertReports() throws IOException {
        LocalConfiguration localConfiguration = LocalConfiguration.builder()
                .WORKBENCH_DIR(AbstractTestCase.testCaseWorkbenchDir(TEST_CASE).toString())
                .WORKSPACE_DIR(runtimeWorkspaceDir.toString())
                .KONTINUUM_DIR("kontinuum")
                .build();

        String pipeline = new LocalPipeline(loadConfiguration(), localConfiguration).generatePipeline();

        assertPeriodParameters(pipeline);
    }

    @Test
    void gitlabPipelinePassesThePeriodOfCertReports() throws IOException {
        GitlabConfiguration gitlabConfiguration = GitlabConfiguration.builder()
                .WORKBENCH_DIR(AbstractTestCase.testCaseWorkbenchDir(TEST_CASE).toString())
                .WORKSPACE_DIR(runtimeWorkspaceDir.toString())
                .KONTINUUM_DIR("kontinuum")
                .CONTAINER_IMAGE("runtime-image")
                .build();

        String pipeline = new GitlabPipeline(loadConfiguration(), gitlabConfiguration).generatePipeline();

        assertPeriodParameters(pipeline);
    }

    private static PipelineConfiguration loadConfiguration() throws IOException {
        Path configFile = AbstractTestCase.resolveConfigTemplate(TEST_CASE, CONFIG_FILE_NAME);
        return new PipelineConfigurationLoader().readConfig(configFile.toFile());
    }

    private static void assertPeriodParameters(String pipeline) {
        List<String> explicitPeriod = createDocumentParameters(pipeline, "cert-explicit-period");
        assertTrue(explicitPeriod.contains("-Dparam.report.period.since='4 weeks'"), pipeline);
        assertTrue(explicitPeriod.contains("-Dparam.report.period.until='1 week'"), pipeline);

        List<String> defaultPeriod = createDocumentParameters(pipeline, "cert-default-period");
        assertTrue(defaultPeriod.contains("-Dparam.report.period.since='1 week'"), pipeline);
        assertTrue(defaultPeriod.contains("-Dparam.report.period.until='now'"), pipeline);

        List<String> vulnerabilityReport = createDocumentParameters(pipeline, "vulnerability-report");
        assertTrue(vulnerabilityReport.stream().noneMatch(p -> p.startsWith(PERIOD_PARAMETER_PREFIX)), pipeline);
    }

    /**
     * Returns the parameters of the single {@code create-document} invocation of a report, identified by the asset
     * id the report processor receives.
     */
    private static List<String> createDocumentParameters(String pipeline, String reportId) {
        String assetIdParameter = "-Dparam.asset.id='" + reportId + "'";
        List<List<String>> invocations = mavenInvocations(pipeline).stream()
                .filter(lines -> lines.get(0).contains(CREATE_DOCUMENT_POM))
                .filter(lines -> lines.contains(assetIdParameter))
                .toList();

        assertEquals(1, invocations.size(),
                "Expected exactly one create-document invocation for report " + reportId + ":\n" + pipeline);
        return invocations.get(0);
    }

    /**
     * Splits a generated pipeline into its maven invocations. Each invocation starts with a line beginning with
     * {@code mvn} and continues as long as the previous line ends with a line continuation. The lines are trimmed
     * and stripped of the line continuation, so local and GitLab pipelines yield the same parameter lines.
     */
    private static List<List<String>> mavenInvocations(String pipeline) {
        List<List<String>> invocations = new ArrayList<>();
        List<String> current = null;
        boolean continued = false;

        for (String line : pipeline.lines().toList()) {
            String trimmed = line.trim();
            if (continued && current != null) {
                current.add(stripContinuation(trimmed));
            } else if (trimmed.startsWith("mvn ")) {
                current = new ArrayList<>();
                current.add(stripContinuation(trimmed));
                invocations.add(current);
            } else {
                current = null;
            }
            continued = current != null && trimmed.endsWith("\\");
        }
        return invocations;
    }

    private static String stripContinuation(String line) {
        return line.endsWith("\\") ? line.substring(0, line.length() - 1).trim() : line;
    }
}
