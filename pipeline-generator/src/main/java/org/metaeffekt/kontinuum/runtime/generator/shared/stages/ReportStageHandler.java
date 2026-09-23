package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Report;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

import java.util.List;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#REPORT} stage.
 * Generates exactly one document per report group and locale, reading the grouped inventories
 * created by the asset-scoped group stage. A report entry with three assets, three types and two
 * locales therefore produces six documents, not one per asset.
 * Based on report type:
 * <ul>
 *   <li>Software Distribution Annex (SDA): group merge &rarr; source aggregation &rarr; license aggregation &rarr; per locale SDA generation &rarr; annex archive.</li>
 *   <li>License Documentation (LD): group merge &rarr; source aggregation &rarr; license aggregation &rarr; per locale LD generation.</li>
 *   <li>Initial License Documentation (ILD): group merge &rarr; license aggregation &rarr; per locale ILD generation.</li>
 *   <li>Other report types (e.g. VR, VSR, CR, CA): per locale report generation only.</li>
 * </ul>
 */
public class ReportStageHandler implements ReportGroupStageHandler {

    @Override
    public Stage getStage() {
        return Stage.REPORT;
    }

    @Override
    public void process(ReportGroupExecutionContext context) {
        Report report = context.getReport();
        ReportType reportType = context.getReportType();
        String type = reportType.getKey();
        List<SupportedLocale> locales = report.getLocales();

        switch (reportType) {
            case SOFTWARE_DISTRIBUTION_ANNEX -> {
                MavenProcessor merge = handleGroupInventoryMerge(context, locales.get(0));
                MavenProcessor sourceAggregation = handleSourceAggregation(context);
                MavenProcessor licenseAggregation = handleLicenseAggregation(context);
                context.addSequential(merge, sourceAggregation, licenseAggregation);
                addPrerequisiteDependencies(context, merge);

                for (SupportedLocale locale : locales) {
                    MavenProcessor reportGenerationProcessor = handleReportGeneration(context, report, type, locale);
                    context.addDependency(reportGenerationProcessor, licenseAggregation);
                    context.addSequential(reportGenerationProcessor, handleAnnexArchiveCreation(context, locale));
                }
            }
            case LICENSE_DOCUMENTATION -> {
                MavenProcessor merge = handleGroupInventoryMerge(context, locales.get(0));
                MavenProcessor sourceAggregation = handleSourceAggregation(context);
                MavenProcessor licenseAggregation = handleLicenseAggregation(context);
                context.addSequential(merge, sourceAggregation, licenseAggregation);
                addPrerequisiteDependencies(context, merge);

                for (SupportedLocale locale : locales) {
                    MavenProcessor reportGenerationProcessor = handleReportGeneration(context, report, type, locale);
                    context.addDependency(reportGenerationProcessor, licenseAggregation);
                    context.addProcessor(reportGenerationProcessor);
                }
            }
            case INITIAL_LICENSE_DOCUMENTATION -> {
                MavenProcessor merge = handleGroupInventoryMerge(context, locales.get(0));
                MavenProcessor licenseAggregation = handleLicenseAggregation(context);
                context.addSequential(merge, licenseAggregation);
                addPrerequisiteDependencies(context, merge);

                for (SupportedLocale locale : locales) {
                    MavenProcessor reportGenerationProcessor = handleReportGeneration(context, report, type, locale);
                    context.addDependency(reportGenerationProcessor, licenseAggregation);
                    context.addProcessor(reportGenerationProcessor);
                }
            }
            default -> {
                for (SupportedLocale locale : locales) {
                    MavenProcessor reportGenerationProcessor = handleReportGeneration(context, report, type, locale);
                    context.addProcessor(reportGenerationProcessor);
                    addPrerequisiteDependencies(context, reportGenerationProcessor);
                }
            }
        }
    }

    /**
     * Wires a processor to depend on every inventory contribution of the group's member assets,
     * so report generation waits for the asset-scoped group stage to complete for all members.
     */
    private void addPrerequisiteDependencies(ReportGroupExecutionContext context, Processor target) {
        List<Processor> prerequisites = context.getPrerequisites();
        if (!prerequisites.isEmpty()) {
            context.addDependency(target, prerequisites.toArray(new Processor[0]));
        }
    }

    /**
     * Merges the per-asset inventories of the group into a single inventory used by source and
     * license aggregation.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_merge-inventories.md">util_merge-inventories.md</a>
     */
    private MavenProcessor handleGroupInventoryMerge(ReportGroupExecutionContext context, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(MERGE_INVENTORIES);
        processor.setStage(Stage.REPORT);

        processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getGroupedInventoryDir(locale));
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE, context.getReportDir().appendMergedInventoryFile());
        processor.setProcessorParameter(PARAM_INVENTORY_INCLUDES, "*.xlsx");

        return processor;
    }

    /**
     * Aggregates source packages and artifacts for all components of the merged group inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_aggregate-sources.md">util_aggregate-sources.md</a>
     */
    private MavenProcessor handleSourceAggregation(ReportGroupExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(AGGREGATE_SOURCES);
        processor.setStage(Stage.REPORT);

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getReportDir().appendMergedInventoryFile());
        processor.setProcessorParameter(OUTPUT_TARGET_DIR, context.getReportDir().appendSourcesDir());
        processor.setProcessorParameter(PARAM_CONFIG_FILE, context.getEnvironment().getConfigDirNormalized() + "source-aggregation/config.yaml");
        processor.setProcessorParameter(PARAM_PROTOCOL_FILE, context.getReportDir().appendSourceAggregationLog());
        processor.setProcessorParameter(PARAM_FAIL_ON_MISSING_SOURCES, "false");

        return processor;
    }

    /**
     * Aggregates licenses and component terms metadata from the database for the merged group inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/util/util_aggregate-licenses.md">util_aggregate-licenses.md</a>
     */
    private MavenProcessor handleLicenseAggregation(ReportGroupExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(AGGREGATE_LICENSES);
        processor.setStage(Stage.REPORT);

        processor.setProcessorParameter(ENV_TMD_SOURCE, context.getEnvironment().TMD_SOURCE);
        processor.setProcessorParameter(ENV_TMD_PASSWORD, context.getEnvironment().TMD_PASSWORD);
        processor.setProcessorParameter(ENV_TMD_USERKEYS_FILE, context.getEnvironment().TMD_USERKEYS_FILE);
        processor.setProcessorParameter(INPUT_INVENTORY_FILE, context.getReportDir().appendMergedInventoryFile());
        processor.setProcessorParameter(PARAM_REFERENCE_COMPONENTS_DIR, context.getEnvironment().getWorkbenchDirNormalized() + "components/");
        processor.setProcessorParameter(PARAM_REFERENCE_LICENSES_DIR, context.getEnvironment().getWorkbenchDirNormalized() + "licenses/");
        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, context.getReferenceInventoryDir());
        processor.setProcessorParameter(PARAM_TARGET_COMPONENTS_DIR, context.getReportDir().appendComponentsDir());
        processor.setProcessorParameter(PARAM_TARGET_LICENSES_DIR, context.getReportDir().appendLicensesDir());

        return processor;
    }

    /**
     * Generates a report document (PDF) for the specified report type and locale from the grouped inventory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/report/report_create-document.md">report_create-document.md</a>
     */
    private MavenProcessor handleReportGeneration(ReportGroupExecutionContext context, Report report, String type, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(CREATE_DOCUMENT);
        processor.setStage(Stage.REPORT);
        ReportType reportType = ReportType.fromKey(type);

        processor.setProcessorParameter(INPUT_INVENTORY_DIR, context.getGroupedInventoryDir(locale));

        if (reportType.equals(ReportType.CERT_REPORT)) {
            processor.setProcessorParameter(PARAM_OVERVIEW_ADVISORS, "[\"CERT_FR\"]");
        } else {
            processor.setProcessorParameter(PARAM_OVERVIEW_ADVISORS,
                    report.getOverviewAdvisors() == null || report.getOverviewAdvisors().isEmpty()
                            ? null
                            : String.join(", ", report.getOverviewAdvisors()));
        }

        if (ReportType.requiresVulnerabilityEnrichment(reportType)) {
            if (context.getConfiguration().getOptions() != null
                    && context.getConfiguration().getOptions().getEnrichment() != null) {
                processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE,
                        context.getConfiguration().getOptions().getEnrichment().getSecurityPolicyFile(context.getEnvironment().getWorkbenchDirNormalized()));
            }
        }

        processor.setProcessorParameter(OUTPUT_DOCUMENT_FILE, context.getReportDir().appendReportFile(reportType, locale));
        processor.setProcessorParameter(PARAM_COMPUTED_INVENTORY_DIR, context.getReportDir().appendComputedDir());
        processor.setProcessorParameter(PARAM_DOCUMENT_TYPE, type);
        processor.setProcessorParameter(PARAM_DOCUMENT_LANGUAGE, locale.getLanguage());

        processor.setProcessorParameter(PARAM_ASSET_ID, context.getGroupId());
        processor.setProcessorParameter(PARAM_ASSET_NAME, report.getProductName());
        processor.setProcessorParameter(PARAM_ASSET_VERSION, report.getProductVersion());

        processor.setProcessorParameter(PARAM_PRODUCT_NAME, report.getProductName());
        processor.setProcessorParameter(PARAM_PRODUCT_VERSION, report.getProductVersion());
        processor.setProcessorParameter(PARAM_PRODUCT_WATERMARK, report.getProductWatermark());
        processor.setProcessorParameter(PARAM_PROPERTY_SELECTOR_ORGANIZATION, report.getOrganization());
        processor.setProcessorParameter(PARAM_PROPERTY_SELECTOR_CLASSIFICATION, report.getClassificationRating());
        processor.setProcessorParameter(PARAM_PROPERTY_SELECTOR_CONTROL, report.getControlRating());
        processor.setProcessorParameter(PARAM_ASSET_DESCRIPTOR_FILE, KontinuumUtils.normalizeDir(context.getEnvironment().getDescriptorsDirNormalized(), reportType.getAssetDescriptorFile()));
        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, context.getReferenceInventoryDir());
        processor.setProcessorParameter(PARAM_REFERENCE_LICENSES_DIR, context.getEnvironment().getWorkbenchDirNormalized() + "licenses/");
        processor.setProcessorParameter(PARAM_REFERENCE_COMPONENTS_DIR, context.getEnvironment().getWorkbenchDirNormalized() + "components/");
        processor.setProcessorParameter(ENV_KONTINUUM_DIR,
                context.getEnvironment().getKontinuumDirNormalized());
        processor.setProcessorParameter(ENV_KONTINUUM_PROCESSORS_DIR,
                context.getEnvironment().getKontinuumProcessorsDirNormalized());
        processor.setProcessorParameter(ENV_WORKBENCH_DIR,
                context.getEnvironment().getWorkbenchDirNormalized());
        processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR,
                context.getEnvironment().getMirrorDatabaseDirNormalized());

        return processor;
    }

    /**
     * Creates the software distribution annex zip archive including document PDF, components, licenses, and sources.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/report/report_create-annex-archive.md">report_create-annex-archive.md</a>
     */
    private MavenProcessor handleAnnexArchiveCreation(ReportGroupExecutionContext context, SupportedLocale locale) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(CREATE_ANNEX_ARCHIVE);
        processor.setStage(Stage.REPORT);

        processor.setProcessorParameter(OUTPUT_ANNEX_ARCHIVE_FILE, context.getReportDir().appendAnnexArchiveFile(locale));
        processor.setProcessorParameter(INPUT_INVENTORY_COMPONENTS_DIR, context.getReportDir().appendComponentsDir());
        processor.setProcessorParameter(INPUT_INVENTORY_LICENSES_DIR, context.getReportDir().appendLicensesDir());
        processor.setProcessorParameter(INPUT_INVENTORY_SOURCES_DIR, context.getReportDir().appendSourcesDir());

        if (locale.equals(SupportedLocale.DE_DE)) {
            processor.setProcessorParameter(INPUT_DOCUMENT_DE_PDF_FILE, context.getReportDir().appendReportFile(ReportType.SOFTWARE_DISTRIBUTION_ANNEX, locale));
        } else {
            processor.setProcessorParameter(INPUT_DOCUMENT_EN_PDF_FILE, context.getReportDir().appendReportFile(ReportType.SOFTWARE_DISTRIBUTION_ANNEX, locale));
        }

        return processor;
    }
}
