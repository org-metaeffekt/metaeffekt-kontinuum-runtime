package org.metaeffekt.kontinuum.runtime.models.shared;

import java.util.*;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Java-native implementation of @link ProcessorCatalog providing all Kontinuum processor definitions.
 */
public class DefaultProcessorCatalog implements ProcessorCatalog {

    private final List<ProcessorDefinitions.Processor> catalog;
    private final Map<String, ProcessorDefinitions.Processor> catalogById;

    /**
     * Creates the catalog and initializes all built-in Kontinuum processor definitions.
     */
    public DefaultProcessorCatalog() {
        List<ProcessorDefinitions.Processor> list = initCatalog();
        list.sort(Comparator.comparing(ProcessorDefinitions.Processor::getId));
        this.catalog = Collections.unmodifiableList(list);

        Map<String, ProcessorDefinitions.Processor> map = new HashMap<>();
        for (ProcessorDefinitions.Processor processor : this.catalog) {
            map.put(processor.getId(), processor);
        }
        // Aliases for backwards compatibility
        if (map.containsKey(ENRICH_WITH_REFERENCE.getValue())) {
            map.put(ENRICH_INVENTORY_WITH_REFERENCE.getValue(), map.get(ENRICH_WITH_REFERENCE.getValue()));
        }
        this.catalogById = Collections.unmodifiableMap(map);
    }

    /**
     * Identifiers of the built-in Kontinuum processors.
     */
    public enum ProcessorIds {
        AGGREGATE_LICENSES("aggregate-licenses"),
        AGGREGATE_REFERENCE_LICENSES("aggregate-reference-licenses"),
        AGGREGATE_SOURCES("aggregate-sources"),
        APPLY_BUSINESS_CASE("apply-business-case"),
        ATTACH_METADATA("attach-metadata"),
        CONVERT_ASSESSMENTS("convert-assessments"),
        COPY_INVENTORIES("copy-inventories"),
        COPY_INVENTORY("copy-inventory"),
        COPY_POM_DEPENDENCIES("copy-pom-dependencies"),
        COPY_RESOURCES("copy-resources"),
        CREATE_ANNEX_ARCHIVE("create-annex-archive"),
        CREATE_DASHBOARD("create-dashboard"),
        CREATE_DIFF("create-diff"),
        CREATE_DOCUMENT("create-document"),
        CREATE_OVERVIEW("create-overview"),
        CYCLONEDX_TO_INVENTORY("cyclonedx-to-inventory"),
        DOWNLOAD_ASSET("download-asset"),
        DOWNLOAD_DATA_SOURCES("download-data-sources"),
        DOWNLOAD_INDEX("download-index"),
        DOWNLOAD_MAVEN_ARTIFACT("download-maven-artifact"),
        ENRICH_ADVISORS("enrich-advisors"),
        ENRICH_INVENTORY("enrich-inventory"),
        ENRICH_WITH_REFERENCE("enrich-with-reference"),
        ENRICH_INVENTORY_WITH_REFERENCE("enrich-inventory-with-reference"),
        EXECUTE_KOTLIN_SCRIPT("execute-kotlin-script"),
        GENERATE_REPORT_SVG("generate-report-svg"),
        INVENTORY_TO_CYCLONEDX("inventory-to-cyclonedx"),
        INVENTORY_TO_SPDX("inventory-to-spdx"),
        MERGE_ADVISORS("merge-advisors"),
        MERGE_ASSESSMENTS("merge-assessments"),
        MERGE_INVENTORIES("merge-inventories"),
        PORTFOLIO_DOWNLOAD("portfolio-download"),
        PORTFOLIO_DOWNLOAD_JARS("portfolio-download-jars"),
        PORTFOLIO_UPLOAD("portfolio-upload"),
        RESOLVE_INVENTORY("resolve-inventory"),
        SAVE_INSPECT_IMAGE("save-inspect-image"),
        SCAN_DIRECTORY("scan-directory"),
        SCAN_INVENTORY("scan-inventory"),
        TRANSFORM_INVENTORIES("transform-inventories"),
        UPDATE_INDEX("update-index"),
        UPDATE_INDEX_EXTERNAL("update-index_external"),
        VALIDATE_REFERENCE_INVENTORY("validate-reference-inventory");

        final String value;

        ProcessorIds(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Override
    public List<ProcessorDefinitions.Processor> getProcessors() {
        List<ProcessorDefinitions.Processor> copies = new ArrayList<>(catalog.size());
        for (ProcessorDefinitions.Processor processor : catalog) {
            copies.add(processor != null ? processor.copy() : null);
        }
        return copies;
    }

    @Override
    public ProcessorDefinitions.Processor getProcessorById(String processorId) {
        if (processorId == null) {
            return null;
        }
        ProcessorDefinitions.Processor processor = catalogById.get(processorId);
        return processor != null ? processor.copy() : null;
    }

    @Override
    public ProcessorDefinitions.Processor getProcessorById(ProcessorIds processorId) {
        if (processorId == null) {
            return null;
        }
        return getProcessorById(processorId.getValue());
    }

    private static List<ProcessorDefinitions.Processor> initCatalog() {
        List<ProcessorDefinitions.Processor> list = new ArrayList<>();
        list.add(aggregateLicenses());
        list.add(aggregateReferenceLicenses());
        list.add(aggregateSources());
        list.add(applyBusinessCase());
        list.add(attachMetadata());
        list.add(convertAssessments());
        list.add(copyInventories());
        list.add(copyInventory());
        list.add(copyPomDependencies());
        list.add(copyResources());
        list.add(createAnnexArchive());
        list.add(createDashboard());
        list.add(createDiff());
        list.add(createDocument());
        list.add(createOverview());
        list.add(cyclonedxToInventory());
        list.add(downloadAsset());
        list.add(downloadDataSources());
        list.add(downloadIndex());
        list.add(downloadMavenArtifact());
        list.add(enrichAdvisors());
        list.add(enrichInventory());
        list.add(enrichWithReference());
        list.add(executeKotlinScript());
        list.add(generateReportSvg());
        list.add(inventoryToCyclonedx());
        list.add(inventoryToSpdx());
        list.add(mergeAdvisors());
        list.add(mergeAssessments());
        list.add(mergeInventories());
        list.add(portfolioDownload());
        list.add(portfolioDownloadJars());
        list.add(portfolioUpload());
        list.add(resolveInventory());
        list.add(saveInspectImage());
        list.add(scanDirectory());
        list.add(scanInventory());
        list.add(transformInventories());
        list.add(updateIndex());
        list.add(updateIndexExternal());
        list.add(validateReferenceInventory());
        return list;
    }


    private static ProcessorDefinitions.MavenProcessor aggregateLicenses() {
        return mavenProcessor(AGGREGATE_LICENSES, "Aggregate Licenses", "util/util_aggregate-licenses.xml",
                              processorParameter(ENV_TMD_PASSWORD, true),
                              processorParameter(ENV_TMD_USERKEYS_FILE, true),
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_REFERENCE_COMPONENTS_DIR, true),
                              processorParameter(PARAM_REFERENCE_LICENSES_DIR, true),
                              processorParameter(ENV_TMD_SOURCE, false),
                              processorParameter(PARAM_FAIL_ON_MISSING_COMPONENT_FILES, false),
                              processorParameter(PARAM_FAIL_ON_MISSING_LICENSE_FILE, false),
                              processorParameter(PARAM_REFERENCE_INVENTORY_DIR, false),
                              processorParameter(PARAM_REFERENCE_INVENTORY_INCLUDES, false),
                              processorParameter(PARAM_TARGET_COMPONENTS_DIR, false),
                              processorParameter(PARAM_TARGET_LICENSES_DIR, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor aggregateReferenceLicenses() {
        return mavenProcessor(AGGREGATE_REFERENCE_LICENSES, "Aggregate Reference Licenses",
                              "util/util_aggregate-reference-licenses.xml",
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_REFERENCE_INVENTORY_DIR, true),
                              processorParameter(PARAM_FAIL_ON_MISSING_LICENSE_FILE, false),
                              processorParameter(PARAM_REFERENCE_COMPONENTS_DIR, false),
                              processorParameter(PARAM_REFERENCE_INVENTORY_INCLUDES, false),
                              processorParameter(PARAM_REFERENCE_LICENSES_DIR, false),
                              processorParameter(PARAM_TARGET_COMPONENTS_DIR, false),
                              processorParameter(PARAM_TARGET_LICENSES_DIR, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor aggregateSources() {
        return mavenProcessor(AGGREGATE_SOURCES, "Aggregate Sources", "util/util_aggregate-sources.xml",
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_TARGET_DIR, true),
                              processorParameter(PARAM_CONFIG_FILE, true),
                              processorParameter(PARAM_PROTOCOL_FILE, false),
                              processorParameter(PARAM_FAIL_ON_MISSING_SOURCES, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor applyBusinessCase() {
        return mavenProcessor(APPLY_BUSINESS_CASE, "Apply Business Case", "util/util_apply-business-case.xml",
                              processorParameter(ENV_TMD_PASSWORD, true),
                              processorParameter(ENV_TMD_USERKEYS_FILE, true),
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(ENV_TMD_SOURCE, false),
                              processorParameter(PARAM_LANGUAGE_MODE, false),
                              processorParameter(PARAM_NOTICE_MODE_OVERWRITE, false),
                              processorParameter(PARAM_REFERENCE_INVENTORY_DIR, false),
                              processorParameter(PARAM_REFERENCE_INVENTORY_INCLUDES, false),
                              processorParameter(PARAM_SOURCE_MODE, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor attachMetadata() {
        return mavenProcessor(ATTACH_METADATA, "Attach Metadata", "advise/advise_attach-metadata.xml",
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_METADATA_ASSET_ID, true),
                              processorParameter(PARAM_METADATA_ASSET_NAME, false),
                              processorParameter(PARAM_METADATA_ASSET_PATH, false),
                              processorParameter(PARAM_METADATA_ASSET_TYPE, false),
                              processorParameter(PARAM_METADATA_ASSET_VERSION, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor convertAssessments() {
        return mavenProcessor(CONVERT_ASSESSMENTS, "Convert Assessments", "util/util_convert-assessments.xml",
                              processorParameter(INPUT_ASSESSMENT_DIR, true),
                              processorParameter(OUTPUT_ASSESSMENT_DIR, true),
                              processorParameter(PARAM_OUTPUT_FORMAT, true),
                              processorParameter(PARAM_OUTPUT_MODE, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor copyInventories() {
        return mavenProcessor(COPY_INVENTORIES, "Copy Inventories", "util/util_copy-inventories.xml",
                              processorParameter(OUTPUT_INVENTORIES_DIR, true),
                              processorParameter(PARAM_INVENTORIES_LIST, true),
                              processorParameter(INPUT_BASE_DIR, true)
        );
    }

    private static ProcessorDefinitions.StandaloneProcessor copyInventory() {
        return standaloneProcessor(COPY_INVENTORY, "Copy Inventory", "util/util_copy-inventory.sh",
                                   processorParameter(INPUT_INVENTORY_FILE, true),
                                   processorParameter(OUTPUT_INVENTORY_FILE, true));
    }

    private static ProcessorDefinitions.MavenProcessor copyPomDependencies() {
        return mavenProcessor(COPY_POM_DEPENDENCIES, "Copy Pom Dependencies",
                              "extract/extract_copy-pom-dependencies.xml",
                              processorParameter(OUTPUT_DEPENDENCIES_DIR, true),
                              processorParameter(PARAM_ARTIFACT_ID, true),
                              processorParameter(PARAM_EXCLUDE_TRANSITIVE_ENABLED, true),
                              processorParameter(PARAM_GROUP_ID, true),
                              processorParameter(PARAM_VERSION, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor copyResources() {
        return mavenProcessor(COPY_RESOURCES, "Copy Resources", "portfolio/portfolio_copy-resources.xml",
                              processorParameter(INPUT_ADVISOR_INVENTORIES_DIR, true),
                              processorParameter(INPUT_DASHBOARDS_DIR, true),
                              processorParameter(INPUT_INVENTORIES_DIR, true),
                              processorParameter(INPUT_REPORTS_DIR, true),
                              processorParameter(OUTPUT_RESOURCES_DIR, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor createAnnexArchive() {
        return mavenProcessor(CREATE_ANNEX_ARCHIVE, "Create Annex Archive", "report/report_create-annex-archive.xml",
                              processorParameter(OUTPUT_ANNEX_ARCHIVE_FILE, true),
                              processorParameter(INPUT_DOCUMENT_DE_PDF_FILE, false),
                              processorParameter(INPUT_DOCUMENT_EN_PDF_FILE, false),
                              processorParameter(INPUT_INVENTORY_COMPONENTS_DIR, false),
                              processorParameter(INPUT_INVENTORY_LICENSES_DIR, false),
                              processorParameter(INPUT_INVENTORY_SOURCES_DIR, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor createDashboard() {
        return mavenProcessor(CREATE_DASHBOARD, "Create Dashboard", "advise/advise_create-dashboard.xml",
                              processorParameter(ENV_VULNERABILITY_MIRROR_DIR, true),
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_DASHBOARD_FILE, true),
                              processorParameter(PARAM_ASSESSMENT_CONTEXT, true),
                              processorParameter(PARAM_ASSET_ID, true),
                              processorParameter(PARAM_SECURITY_POLICY_FILE, true),
                              processorParameter(PARAM_TENANT_ID, true),
                              processorParameter(ENV_VULNERABILITY_ASSESSMENT_API, false),
                              processorParameter(PARAM_EVENTS_SINCE_TIMESTAMP_FOR_DASHBOARD, false),
                              processorParameter(PARAM_PUT_EVENT_FOR_DASHBOARD, false),
                              processorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS, false),
                              processorParameter(PARAM_TIMELINE_CONF_ENABLED, false),
                              processorParameter(PARAM_TIMELINE_MAX_THREADS, false),
                              processorParameter(PARAM_TIMELINE_TIME_SPENT_MAX, false),
                              processorParameter(PARAM_TIMELINE_VULN_PROVIDERS_LIST, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor createDiff() {
        return mavenProcessor(CREATE_DIFF, "Create Diff", "util/util_create-diff.xml",
                              processorParameter(INPUT_INVENTORY_COMPARE_FILE, true),
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_INVENTORY_DIR, true),
                              processorParameter(PARAM_INVENTORY_COMPARE_VERSION, true),
                              processorParameter(PARAM_INVENTORY_VERSION, true),
                              processorParameter(PARAM_SECURITY_POLICY_FILE, true),
                              processorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor createDocument() {
        return mavenProcessor(CREATE_DOCUMENT, "Create Document", "report/report_create-document.xml",
                              processorParameter(ENV_KONTINUUM_DIR, true),
                              processorParameter(ENV_WORKBENCH_DIR, true),
                              processorParameter(INPUT_INVENTORY_DIR, true),
                              processorParameter(OUTPUT_DOCUMENT_FILE, true),
                              processorParameter(PARAM_ASSET_DESCRIPTOR_FILE, true),
                              processorParameter(PARAM_ASSET_ID, true),
                              processorParameter(PARAM_ASSET_NAME, true),
                              processorParameter(PARAM_ASSET_VERSION, true),
                              processorParameter(PARAM_ASSET_BUILD, true),
                              processorParameter(PARAM_DOCUMENT_TYPE, true),
                              processorParameter(PARAM_PRODUCT_NAME, true),
                              processorParameter(PARAM_PRODUCT_VERSION, true),
                              processorParameter(PARAM_PRODUCT_WATERMARK, true),
                              processorParameter(PARAM_PROPERTY_SELECTOR_ORGANIZATION, true),
                              processorParameter(ENV_KONTINUUM_PROCESSORS_DIR, false),
                              processorParameter(ENV_VULNERABILITY_MIRROR_DIR, false),
                              processorParameter(ENV_WORKBENCH_PROCESSORS_DIR, false),
                              processorParameter(PARAM_COMPUTED_INVENTORY_DIR, false),
                              processorParameter(PARAM_DOCUMENT_LANGUAGE, false),
                              processorParameter(PARAM_OVERVIEW_ADVISORS, false),
                              processorParameter(PARAM_PROPERTY_SELECTOR_CLASSIFICATION, false),
                              processorParameter(PARAM_PROPERTY_SELECTOR_CONTROL, false),
                              processorParameter(PARAM_REFERENCE_COMPONENTS_DIR, false),
                              processorParameter(PARAM_REFERENCE_INVENTORY_DIR, false),
                              processorParameter(PARAM_REFERENCE_LICENSES_DIR, false),
                              processorParameter(PARAM_SECURITY_POLICY_FILE, false),
                              processorParameter(PARAM_TEMPLATE_DIR, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor createOverview() {
        return mavenProcessor(CREATE_OVERVIEW, "Create Overview", "portfolio/portfolio_create-overview.xml",
                              processorParameter(INPUT_ADVISOR_INVENTORIES_DIR, true),
                              processorParameter(INPUT_DASHBOARDS_DIR, true),
                              processorParameter(INPUT_INVENTORY_DIR, true),
                              processorParameter(INPUT_INVENTORY_PATH, true),
                              processorParameter(INPUT_REPORTS_DIR, true),
                              processorParameter(OUTPUT_OVERVIEW_FILE, true),
                              processorParameter(PARAM_SECURITY_POLICY_FILE, true),
                              processorParameter(OUTPUT_NOTIFICATION_FILE, false),
                              processorParameter(PARAM_NOTIFICATION_CONFIG_FILE, false),
                              processorParameter(PARAM_NOTIFICATION_RULE_FILE, false),
                              processorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor cyclonedxToInventory() {
        return mavenProcessor(CYCLONEDX_TO_INVENTORY, "Cyclonedx To Inventory",
                              "convert/convert_cyclonedx-to-inventory.xml",
                              processorParameter(INPUT_BOM_FILE, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_DERIVE_ATTRIBUTES_FROM_PURL_ENABLED, false),
                              processorParameter(PARAM_INCLUDE_ASSETS_ENABLED, false),
                              processorParameter(PARAM_INCLUDE_LICENSES_ENABLED, false),
                              processorParameter(PARAM_INCLUDE_METADATA_COMPONENT_ENABLED, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor downloadAsset() {
        return mavenProcessor(DOWNLOAD_ASSET, "Download Asset", "fetch/fetch_download-asset.xml",
                              processorParameter(OUTPUT_ASSET_DIR, true),
                              processorParameter(PARAM_ASSET_URL, true),
                              processorParameter(PARAM_USERNAME, false),
                              processorParameter(PARAM_PASSWORD, false),
                              processorParameter(PARAM_TOKEN, false),
                              processorParameter(PARAM_HEADER_NAME, false),
                              processorParameter(PARAM_HEADER_VALUE, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor downloadDataSources() {
        return mavenProcessor(DOWNLOAD_DATA_SOURCES, "Download Data Sources", "mirror/mirror_download-data-sources.xml",
                              processorParameter(ENV_MIRROR_DIR, true),
                              processorParameter(ENV_NVD_APIKEY, true),
                              processorParameter(PARAM_FAIL_ON_ERROR, false),
                              processorParameter(PARAM_FAIL_ON_ISSUE, false),
                              processorParameter(PARAM_PROXY_HOST, false),
                              processorParameter(PARAM_PROXY_PASS, false),
                              processorParameter(PARAM_PROXY_PORT, false),
                              processorParameter(PARAM_PROXY_SCHEME, false),
                              processorParameter(PARAM_PROXY_USER, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor downloadIndex() {
        return mavenProcessor(DOWNLOAD_INDEX, "Download Index", "mirror/mirror_download-index.xml",
                              processorParameter(ENV_VULNERABILITY_MIRROR_DIR, true),
                              processorParameter(PARAM_MIRROR_ARCHIVE_URL, true),
                              processorParameter(PARAM_MIRROR_ARCHIVE_PASSWORD, false),
                              processorParameter(PARAM_MIRROR_ARCHIVE_USERNAME, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor downloadMavenArtifact() {
        return mavenProcessor(DOWNLOAD_MAVEN_ARTIFACT, "Download Maven Artifact",
                              "fetch/fetch_download-maven-artifact.xml",
                              processorParameter(OUTPUT_ASSET_DIR, true),
                              processorParameter(PARAM_ARTIFACT_ID, true),
                              processorParameter(PARAM_GROUP_ID, true),
                              processorParameter(PARAM_VERSION, true),
                              processorParameter(PARAM_CLASSIFIER, false),
                              processorParameter(PARAM_REPO_URL, false),
                              processorParameter(PARAM_TYPE, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor enrichAdvisors() {
        return mavenProcessor(ENRICH_ADVISORS, "Enrich Advisors", "util/util_enrich-advisors.xml",
                              processorParameter(ENV_VULNERABILITY_MIRROR_DIR, true),
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_SECURITY_POLICY_FILE, true),
                              processorParameter(PARAM_REPORT_PERIOD_SINCE, false),
                              processorParameter(PARAM_REPORT_PERIOD_UNTIL, false),
                              processorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor enrichInventory() {
        return mavenProcessor(ENRICH_INVENTORY, "Enrich Inventory", "advise/advise_enrich-inventory.xml",
                              processorParameter(ENV_VULNERABILITY_MIRROR_DIR, true),
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_TMP_DIR, true),
                              processorParameter(PARAM_ASSESSMENT_DIRS, true),
                              processorParameter(PARAM_CONTEXT_DIRS, true),
                              processorParameter(PARAM_CORRELATION_DIR, true),
                              processorParameter(PARAM_SECURITY_POLICY_FILE, true),
                              processorParameter(PARAM_ACTIVATE_CAPEC, false),
                              processorParameter(PARAM_ACTIVATE_CERTEU, false),
                              processorParameter(PARAM_ACTIVATE_CERTFR, false),
                              processorParameter(PARAM_ACTIVATE_CERTSEI, false),
                              processorParameter(PARAM_ACTIVATE_CORRELATION, false),
                              processorParameter(PARAM_ACTIVATE_CSAF, false),
                              processorParameter(PARAM_ACTIVATE_CWE, false),
                              processorParameter(PARAM_ACTIVATE_EOL, false),
                              processorParameter(PARAM_ACTIVATE_EPSS, false),
                              processorParameter(PARAM_ACTIVATE_KEV, false),
                              processorParameter(PARAM_ACTIVATE_KEYWORDS, false),
                              processorParameter(PARAM_ACTIVATE_MITRE_ATLAS, false),
                              processorParameter(PARAM_ACTIVATE_MITRE_ATTACK, false),
                              processorParameter(PARAM_ACTIVATE_MSRC, false),
                              processorParameter(PARAM_ACTIVATE_NVD, false),
                              processorParameter(PARAM_ACTIVATE_OSV, false),
                              processorParameter(PARAM_ACTIVATE_OSV_PROVIDERS, false),
                              processorParameter(PARAM_ACTIVATE_PURL_DERIVATION, false),
                              processorParameter(PARAM_ACTIVATE_STATUS, false),
                              processorParameter(PARAM_ACTIVATE_THREAT, false),
                              processorParameter(PARAM_ACTIVATE_VALIDATION, false),
                              processorParameter(PARAM_ACTIVATE_VULNERABILITIES_CUSTOM, false),
                              processorParameter(PARAM_ASSESSMENT_LABELS, false),
                              processorParameter(PARAM_DASHBOARD_FOOTER, false),
                              processorParameter(PARAM_DASHBOARD_SUBTITLE, false),
                              processorParameter(PARAM_DASHBOARD_TITLE, false),
                              processorParameter(PARAM_EXCLUDE_NVD_EQUIVALENT_MSRC, false),
                              processorParameter(PARAM_EXCLUDE_NVD_EQUIVALENT_OSV, false),
                              processorParameter(PARAM_REMOVE_GHSA_UNREVIEWED, false),
                              processorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS, false),
                              processorParameter(PARAM_THREAT_CATALOG_FILE, false),
                              processorParameter(PARAM_VULNERABILITIES_CUSTOM_DIR, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor enrichWithReference() {
        return mavenProcessor(ENRICH_WITH_REFERENCE, "Enrich With Reference", "util/util_enrich-with-reference.xml",
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_REFERENCE_INVENTORY_DIR, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor executeKotlinScript() {
        return mavenProcessor(EXECUTE_KOTLIN_SCRIPT, "Execute Kotlin Script", "util/util_execute-kotlin-script.xml",
                              processorParameter(INPUT_KOTLIN_SCRIPT_FILE, true),
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_ASSET_ID, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor generateReportSvg() {
        return mavenProcessor(GENERATE_REPORT_SVG, "Generate Report Svg", "util/util_generate-report-svg.xml",
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_SVG_DIR, true),
                              processorParameter(PARAM_SECURITY_POLICY_FILE, true),
                              processorParameter(PARAM_CVSS_ACTIVE, false),
                              processorParameter(PARAM_CVSS_VULNERABILITY_COUNT_LIMIT, false),
                              processorParameter(PARAM_OVERVIEW_ACTIVE, false),
                              processorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor inventoryToCyclonedx() {
        return mavenProcessor(INVENTORY_TO_CYCLONEDX, "Inventory To Cyclonedx",
                              "convert/convert_inventory-to-cyclonedx.xml",
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_BOM_FILE, true),
                              processorParameter(PARAM_DOCUMENT_NAME, true),
                              processorParameter(PARAM_DOCUMENT_ORGANIZATION, true),
                              processorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, true),
                              processorParameter(PARAM_CUSTOM_LICENSE_MAPPINGS, false),
                              processorParameter(PARAM_DERIVE_ATTRIBUTES_FROM_PURL_ENABLED, false),
                              processorParameter(PARAM_DOCUMENT_COMMENT, false),
                              processorParameter(PARAM_DOCUMENT_DESCRIPTION, false),
                              processorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, false),
                              processorParameter(PARAM_DOCUMENT_PERSON, false),
                              processorParameter(PARAM_DOCUMENT_VERSION, false),
                              processorParameter(PARAM_INCLUDE_ASSETS_ENABLED, false),
                              processorParameter(PARAM_INCLUDE_LICENSE_TEXTS_ENABLED, false),
                              processorParameter(PARAM_LICENSE_EXPRESSIONS_ENABLED, false),
                              processorParameter(PARAM_MAP_RELATIONSHIPS_ENABLED, false),
                              processorParameter(PARAM_TECHNICAL_PROPERTIES_ENABLED, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor inventoryToSpdx() {
        return mavenProcessor(INVENTORY_TO_SPDX, "Inventory To Spdx", "convert/convert_inventory-to-spdx.xml",
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_BOM_FILE, true),
                              processorParameter(PARAM_DOCUMENT_NAME, true),
                              processorParameter(PARAM_DOCUMENT_ORGANIZATION, true),
                              processorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, true),
                              processorParameter(PARAM_DERIVE_ATTRIBUTES_FROM_PURL_ENABLED, false),
                              processorParameter(PARAM_DOCUMENT_COMMENT, false),
                              processorParameter(PARAM_DOCUMENT_DESCRIPTION, false),
                              processorParameter(PARAM_DOCUMENT_ID_PREFIX, false),
                              processorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, false),
                              processorParameter(PARAM_DOCUMENT_PERSON, false),
                              processorParameter(PARAM_DOCUMENT_VERSION, false),
                              processorParameter(PARAM_INCLUDE_ASSETS_ENABLED, false),
                              processorParameter(PARAM_INCLUDE_LICENSE_TEXTS_ENABLED, false),
                              processorParameter(PARAM_LICENSE_EXPRESSIONS_ENABLED, false),
                              processorParameter(PARAM_MAP_RELATIONSHIPS_ENABLED, false),
                              processorParameter(PARAM_TECHNICAL_PROPERTIES_ENABLED, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor mergeAdvisors() {
        return mavenProcessor(MERGE_ADVISORS, "Merge Advisors", "util/util_merge-advisors.xml",
                              processorParameter(INPUT_INVENTORY_DIR, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_SECURITY_POLICY_FILE, true),
                              processorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor mergeAssessments() {
        return mavenProcessor(MERGE_ASSESSMENTS, "Merge Assessments", "util/util_merge-assessments.xml",
                              processorParameter(INPUT_INVENTORY_DIR, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor mergeInventories() {
        return mavenProcessor(MERGE_INVENTORIES, "Merge Inventories", "util/util_merge-inventories.xml",
                              processorParameter(INPUT_INVENTORY_DIR, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_INVENTORY_INCLUDES, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor portfolioDownload() {
        return mavenProcessor(PORTFOLIO_DOWNLOAD, "Portfolio Download", "aggregate/aggregate_portfolio-download.xml",
                              processorParameter(OUTPUT_INVENTORY_DIR, true),
                              processorParameter(PARAM_ASSET_GROUP_ID, true),
                              processorParameter(PARAM_ASSET_ID, true),
                              processorParameter(PARAM_INVENTORY_MODIFIER, true),
                              processorParameter(PARAM_KEYSTORE_CONFIG_FILE, true),
                              processorParameter(PARAM_KEYSTORE_PASSWORD, true),
                              processorParameter(PARAM_PORTFOLIO_MANAGER_TOKEN, true),
                              processorParameter(PARAM_PORTFOLIO_MANAGER_URL, true),
                              processorParameter(PARAM_PROJECT_NAME, true),
                              processorParameter(PARAM_TRUSTSTORE_CONFIG_FILE, true),
                              processorParameter(PARAM_TRUSTSTORE_PASSWORD, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor portfolioDownloadJars() {
        return mavenProcessor(PORTFOLIO_DOWNLOAD_JARS, "Portfolio Download Jars",
                              "util/util_portfolio-download-jars.xml",
                              processorParameter(INPUT_CLI_DIR, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor portfolioUpload() {
        return mavenProcessor(PORTFOLIO_UPLOAD, "Portfolio Upload", "prepare/prepare_portfolio-upload.xml",
                              processorParameter(INPUT_FILE, true),
                              processorParameter(PARAM_ASSET_GROUP_ID, true),
                              processorParameter(PARAM_ASSET_NAME, true),
                              processorParameter(PARAM_ASSET_VERSION, true),
                              processorParameter(PARAM_KEYSTORE_CONFIG_FILE, true),
                              processorParameter(PARAM_KEYSTORE_PASSWORD, true),
                              processorParameter(PARAM_PORTFOLIO_MANAGER_TOKEN, true),
                              processorParameter(PARAM_PORTFOLIO_MANAGER_URL, true),
                              processorParameter(PARAM_PROJECT_NAME, true),
                              processorParameter(PARAM_TRUSTSTORE_CONFIG_FILE, true),
                              processorParameter(PARAM_TRUSTSTORE_PASSWORD, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor resolveInventory() {
        return mavenProcessor(RESOLVE_INVENTORY, "Resolve Inventory", "resolve/resolve_resolve-inventory.xml",
                              processorParameter(ENV_MAVEN_INDEX_DIR, true),
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_ARTIFACT_RESOLVER_CONFIG_FILE, true),
                              processorParameter(PARAM_ARTIFACT_RESOLVER_PROXY_FILE, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor saveInspectImage() {
        return mavenProcessor(SAVE_INSPECT_IMAGE, "Save Inspect Image", "fetch/fetch_save-image.xml",
                              processorParameter(OUTPUT_DIR, true),
                              processorParameter(PARAM_IMAGE_ID, true),
                              processorParameter(PARAM_IMAGE_VERSION, true),
                              processorParameter(PARAM_REPO_URL, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor scanDirectory() {
        return mavenProcessor(SCAN_DIRECTORY, "Scan Directory", "prepare/prepare_scan-directory.xml",
                              processorParameter(INPUT_EXTRACT_DIR, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(OUTPUT_SCAN_DIR, true),
                              processorParameter(PARAM_REFERENCE_INVENTORY_DIR, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor scanInventory() {
        return mavenProcessor(SCAN_INVENTORY, "Scan Inventory", "scan/scan_scan-inventory.xml",
                              processorParameter(ENV_KOSMOS_PASSWORD, true),
                              processorParameter(ENV_KOSMOS_USERKEYS_FILE, true),
                              processorParameter(INPUT_INVENTORY_FILE, true),
                              processorParameter(INPUT_OUTPUT_ANALYSIS_BASE_DIR, true),
                              processorParameter(OUTPUT_INVENTORY_FILE, true),
                              processorParameter(PARAM_PROPERTIES_FILE, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor transformInventories() {
        return mavenProcessor(TRANSFORM_INVENTORIES, "Transform Inventories", "util/util_transform-inventories.xml",
                              processorParameter(INPUT_INVENTORY_DIR, true),
                              processorParameter(OUTPUT_INVENTORY_DIR, true),
                              processorParameter(PARAM_KOTLIN_SCRIPT_FILE, true),
                              processorParameter(PARAM_ASSET_NAME, false),
                              processorParameter(PARAM_FILTER_PRESET, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor updateIndex() {
        return mavenProcessor(UPDATE_INDEX, "Update Index", "mirror/mirror_update-index.xml",
                              processorParameter(ENV_MIRROR_DIR, true),
                              processorParameter(PARAM_FAIL_ON_ERROR, false),
                              processorParameter(PARAM_FAIL_ON_ISSUE, false),
                              processorParameter(PARAM_PROXY_HOST, false),
                              processorParameter(PARAM_PROXY_PASS, false),
                              processorParameter(PARAM_PROXY_PORT, false),
                              processorParameter(PARAM_PROXY_SCHEME, false),
                              processorParameter(PARAM_PROXY_USER, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor updateIndexExternal() {
        return mavenProcessor(UPDATE_INDEX_EXTERNAL, "Update Index_external", "mirror/mirror_update-index_external.xml",
                              processorParameter(ENV_MIRROR_DIR, true),
                              processorParameter(PARAM_FAIL_ON_ERROR, false),
                              processorParameter(PARAM_FAIL_ON_ISSUE, false),
                              processorParameter(PARAM_PROXY_HOST, false),
                              processorParameter(PARAM_PROXY_PASS, false),
                              processorParameter(PARAM_PROXY_PORT, false),
                              processorParameter(PARAM_PROXY_SCHEME, false),
                              processorParameter(PARAM_PROXY_USER, false)
        );
    }

    private static ProcessorDefinitions.MavenProcessor validateReferenceInventory() {
        return mavenProcessor(VALIDATE_REFERENCE_INVENTORY, "Validate Reference Inventory",
                              "util/util_validate-reference-inventory.xml",
                              processorParameter(INPUT_INVENTORY_DIR, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor mavenProcessor(
            ProcessorIds id, String name, String pomLocation,
            ProcessorDefinitions.ProcessorParameter... parameters) {
        ProcessorDefinitions.MavenProcessor processor = new ProcessorDefinitions.MavenProcessor(pomLocation);
        processor.setParameters(Arrays.asList(parameters));
        processor.setId(id.getValue());
        processor.setName(name);
        return processor;
    }

    private static ProcessorDefinitions.StandaloneProcessor standaloneProcessor(
            ProcessorIds id, String name, String scriptLocation,
            ProcessorDefinitions.ProcessorParameter... parameters) {

        ProcessorDefinitions.StandaloneProcessor standaloneProcessor = new ProcessorDefinitions.StandaloneProcessor(
                scriptLocation);
        standaloneProcessor.setParameters(Arrays.asList(parameters));
        standaloneProcessor.setId(id.getValue());
        standaloneProcessor.setName(name);

        return standaloneProcessor;
    }

    private static ProcessorDefinitions.ProcessorParameter processorParameter(
            ProcessorParameterKey key, boolean required) {
        return new ProcessorDefinitions.ProcessorParameter(key, required, null);
    }
}
