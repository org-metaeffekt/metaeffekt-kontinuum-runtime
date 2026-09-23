package org.metaeffekt.kontinuum.runtime.generator.shared;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.ProcessorParameter;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey;
import org.metaeffekt.kontinuum.runtime.models.shared.ReportGroupExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.SupportedLocale;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportGroupingTest {

    private static PipelineConfiguration.ProjectProperties.Asset asset(String id) {
        PipelineConfiguration.ProjectProperties.Asset asset = new PipelineConfiguration.ProjectProperties.Asset();
        asset.setId(id);
        asset.setName(id);
        asset.setVersion("1.0.0");
        asset.setAssessmentId(id);
        asset.setContext("local");
        asset.setReference("inventories/reference");
        PipelineConfiguration.ProjectProperties.Asset.UrlResolver resolver = new PipelineConfiguration.ProjectProperties.Asset.UrlResolver();
        resolver.setUrl("https://example.com/" + id + ".zip");
        asset.setUrlResolver(resolver);
        return asset;
    }

    private static PipelineConfiguration.Report report(String id, List<String> assetIds, List<String> types, List<SupportedLocale> locales) {
        PipelineConfiguration.Report report = new PipelineConfiguration.Report();
        report.setId(id);
        report.setAssetIds(assetIds);
        report.setTypes(types);
        report.setLocales(locales);
        report.setProductName("product");
        report.setProductVersion("1.0.0");
        report.setProductWatermark("watermark");
        report.setOrganization("metaeffekt");
        report.setClassificationRating("DEFAULT");
        report.setControlRating("DEFAULT");
        return report;
    }

    private static PipelineExecution generate(List<PipelineConfiguration.Report> reports) {
        PipelineConfiguration configuration = TestUtils.buildMinimalPipelineConfiguration();
        configuration.getProjectProperties().setAssets(List.of(asset("A"), asset("B"), asset("C")));
        configuration.setReports(reports);

        PipelineConfiguration.Options options = new PipelineConfiguration.Options();
        PipelineConfiguration.Options.EnrichmentOptions enrichment = new PipelineConfiguration.Options.EnrichmentOptions();
        enrichment.setSecurityPolicyFile("policies/security-policy.json");
        options.setEnrichment(enrichment);
        configuration.setOptions(options);

        LocalConfiguration environment = TestUtils.buildMinimalLocalConfiguration();
        return new Pipeline(configuration, environment).generatePipeline();
    }

    private static String parameter(Processor processor, ProcessorParameterKey key) {
        if (processor.getParameters() == null) {
            return null;
        }
        return processor.getParameters().stream()
                .filter(p -> p.getKey() == key)
                .map(ProcessorParameter::getValue)
                .findFirst()
                .orElse(null);
    }

    private static long count(ExecutionContext context, String processorId) {
        return context.getProcessors().stream().filter(p -> processorId.equals(p.getId())).count();
    }

    @Test
    public void generatesOneDocumentPerTypeAndLocaleNotPerAsset() {
        PipelineExecution execution = generate(List.of(
                report("r1", List.of("A", "B", "C"), List.of("SDA", "CR", "VR"), List.of(SupportedLocale.EN_US, SupportedLocale.DE_DE))));

        long documents = execution.getContexts().stream().mapToLong(ctx -> count(ctx, "create-document")).sum();

        // 3 types x 2 locales = 6, independent of the 3 assets.
        assertEquals(6, documents);
        assertEquals(3, execution.getReportGroupContexts().size());
    }

    @Test
    public void groupStageOnlyCopiesAssetsBelongingToTheReport() {
        PipelineExecution execution = generate(List.of(
                report("r1", List.of("A", "B"), List.of("VR"), List.of(SupportedLocale.EN_US)),
                report("r2", List.of("B"), List.of("VR"), List.of(SupportedLocale.EN_US))));

        for (AssetExecutionContext context : execution.getAssetContexts().values()) {
            String assetId = context.getAsset().getId();
            for (Processor processor : context.getProcessors()) {
                if (!"copy-inventory".equals(processor.getId())) {
                    continue;
                }
                String target = parameter(processor, ProcessorParameterKey.OUTPUT_INVENTORY_FILE);
                if (target.contains("/r2/")) {
                    assertEquals("B", assetId, "only asset B may contribute to report group r2");
                }
            }
        }
    }

    @Test
    public void reportOutputsAreGroupKeyedAndUseReportIdentity() {
        PipelineExecution execution = generate(List.of(
                report("r1", List.of("A", "B"), List.of("VR"), List.of(SupportedLocale.EN_US))));

        ReportGroupExecutionContext group = execution.getReportGroupContexts().get(0);
        assertEquals("./workspace/project-id/08_reported/r1/", group.getReportDir().toString());

        Processor document = group.getProcessors().stream()
                .filter(p -> "create-document".equals(p.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("./workspace/project-id/08_reported/r1/r1-VR-en_US.pdf",
                parameter(document, ProcessorParameterKey.OUTPUT_DOCUMENT_FILE));
        assertEquals("r1", parameter(document, ProcessorParameterKey.PARAM_ASSET_ID));
        assertEquals("product", parameter(document, ProcessorParameterKey.PARAM_ASSET_NAME));
        assertEquals("1.0.0", parameter(document, ProcessorParameterKey.PARAM_ASSET_VERSION));
    }

    @Test
    public void reportGenerationDependsOnAllMemberAssetContributions() {
        PipelineExecution execution = generate(List.of(
                report("r1", List.of("A", "B"), List.of("VR"), List.of(SupportedLocale.EN_US))));

        ReportGroupExecutionContext group = execution.getReportGroupContexts().get(0);
        Processor document = group.getProcessors().stream()
                .filter(p -> "create-document".equals(p.getId()))
                .findFirst()
                .orElseThrow();

        // One copy-inventory contribution per member asset.
        assertEquals(2, group.getDependencies(document).size());
    }

    @Test
    public void preReportFilterRunsOnEachMemberAssetInventory() {
        PipelineConfiguration.Report report = report("r1", List.of("A", "B"), List.of("VR"), List.of(SupportedLocale.EN_US));
        report.setPreReportFilterFile("scripts/prepare.kts");

        PipelineExecution execution = generate(List.of(report));

        // One filter run per member asset (A and B), not for unrelated asset C.
        long filters = execution.getAssetContexts().values().stream()
                .mapToLong(context -> count(context, "transform-inventories"))
                .sum();
        assertEquals(2, filters);

        // The copies must consume the filtered inventory.
        for (AssetExecutionContext context : execution.getAssetContexts().values()) {
            if (!List.of("A", "B").contains(context.getAsset().getId())) {
                continue;
            }
            Processor copy = context.getProcessors().stream()
                    .filter(p -> "copy-inventory".equals(p.getId()))
                    .findFirst()
                    .orElseThrow();
            assertTrue(parameter(copy, ProcessorParameterKey.INPUT_INVENTORY_FILE).contains("/prepared/"),
                    "copy must read the pre-report-filtered inventory");
        }
    }

    @Test
    public void pipelineWideStageRunsOnce() {
        PipelineExecution execution = generate(List.of(
                report("r1", List.of("A", "B", "C"), List.of("VR"), List.of(SupportedLocale.EN_US))));

        long downloadIndex = execution.getContexts().stream().mapToLong(ctx -> count(ctx, "download-index")).sum();

        assertEquals(1, downloadIndex);
    }
}
