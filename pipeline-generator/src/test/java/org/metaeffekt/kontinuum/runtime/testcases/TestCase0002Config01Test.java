package org.metaeffekt.kontinuum.runtime.testcases;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case 0002: the period of cert reports is passed to the report processor. The {@code create-document}
 * invocation of each report is checked in the local and the GitLab pipeline: an explicit period is passed as
 * configured, a missing period falls back to the default, and other report types get no period.
 */
class TestCase0002Config01Test extends AbstractTestCase {

    private static final String CREATE_DOCUMENT_POM = "report/report_create-document.xml";
    private static final String PERIOD_PARAMETER_PREFIX = "-Dparam.report.period.";

    @Test
    void localPipeline_passesCertReportPeriod() throws IOException {
        assertPeriodParameters(generateLocalPipeline());
    }

    @Test
    void gitlabPipeline_passesCertReportPeriod() throws IOException {
        assertPeriodParameters(generateGitlabPipeline());
    }

    private static void assertPeriodParameters(GeneratedPipeline pipeline) {
        List<String> explicitPeriod = createDocumentParameters(pipeline, "cert-explicit-period");
        assertTrue(explicitPeriod.contains("-Dparam.report.period.since='4 weeks'"), pipeline.toString());
        assertTrue(explicitPeriod.contains("-Dparam.report.period.until='1 week'"), pipeline.toString());

        List<String> defaultPeriod = createDocumentParameters(pipeline, "cert-default-period");
        assertTrue(defaultPeriod.contains("-Dparam.report.period.since='1 week'"), pipeline.toString());
        assertTrue(defaultPeriod.contains("-Dparam.report.period.until='now'"), pipeline.toString());

        List<String> vulnerabilityReport = createDocumentParameters(pipeline, "vulnerability-report");
        assertTrue(vulnerabilityReport.stream().noneMatch(p -> p.startsWith(PERIOD_PARAMETER_PREFIX)), pipeline.toString());
    }

    /**
     * Returns the parameters of the single {@code create-document} invocation of a report, identified by the asset
     * id the report processor receives.
     */
    private static List<String> createDocumentParameters(GeneratedPipeline pipeline, String reportId) {
        String assetIdParameter = "-Dparam.asset.id='" + reportId + "'";
        List<List<String>> invocations = pipeline.mavenInvocations().stream()
                .filter(lines -> lines.get(0).contains(CREATE_DOCUMENT_POM))
                .filter(lines -> lines.contains(assetIdParameter))
                .toList();

        assertEquals(1, invocations.size(),
                "Expected exactly one create-document invocation for report " + reportId + ":\n" + pipeline);
        return invocations.get(0);
    }
}
