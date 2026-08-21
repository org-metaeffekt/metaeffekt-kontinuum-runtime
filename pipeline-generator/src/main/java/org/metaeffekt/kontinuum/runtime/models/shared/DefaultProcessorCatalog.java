package org.metaeffekt.kontinuum.runtime.models.shared;

import java.util.*;

import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;

/**
 * Java-native implementation of @link ProcessorCatalog providing all Kontinuum processor definitions.
 */
public class DefaultProcessorCatalog implements ProcessorCatalog {

    private final List<ProcessorDefinitions.MavenProcessor> catalog;
    private final Map<String, ProcessorDefinitions.MavenProcessor> catalogById;

    public DefaultProcessorCatalog() {
        List<ProcessorDefinitions.MavenProcessor> list = initCatalog();
        list.sort(Comparator.comparing(ProcessorDefinitions.MavenProcessor::getId));
        this.catalog = Collections.unmodifiableList(list);

        Map<String, ProcessorDefinitions.MavenProcessor> map = new HashMap<>();
        for (ProcessorDefinitions.MavenProcessor processor : this.catalog) {
            map.put(processor.getId(), processor);
        }
        // Aliases for backwards compatibility
        if (map.containsKey(ENRICH_WITH_REFERENCE.getValue())) {
            map.put(ENRICH_INVENTORY_WITH_REFERENCE.getValue(), map.get(ENRICH_WITH_REFERENCE.getValue()));
        }
        this.catalogById = Collections.unmodifiableMap(map);
    }

    public enum ProcessorIds {
        AGGREGATE_LICENSES("aggregate-licenses"),
        AGGREGATE_REFERENCE_LICENSES("aggregate-reference-licenses"),
        AGGREGATE_SOURCES("aggregate-sources"),
        APPLY_BUSINESS_CASE("apply-business-case"),
        ATTACH_METADATA("attach-metadata"),
        CONVERT_ASSESSMENTS("convert-assessments"),
        COPY_INVENTORIES("copy-inventories"),
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
    public List<ProcessorDefinitions.MavenProcessor> getProcessors() {
        List<ProcessorDefinitions.MavenProcessor> copies = new ArrayList<>(catalog.size());
        for (ProcessorDefinitions.MavenProcessor processor : catalog) {
            copies.add(processor.copy());
        }
        return copies;
    }

    @Override
    public ProcessorDefinitions.MavenProcessor getProcessorById(String processorId) {
        if (processorId == null) {
            return null;
        }
        ProcessorDefinitions.MavenProcessor processor = catalogById.get(processorId);
        return processor != null ? processor.copy() : null;
    }

    @Override
    public ProcessorDefinitions.MavenProcessor getProcessorById(ProcessorIds processorId) {
        if (processorId == null) {
            return null;
        }
        return getProcessorById(processorId.getValue());
    }

    private static List<ProcessorDefinitions.MavenProcessor> initCatalog() {
        List<ProcessorDefinitions.MavenProcessor> list = new ArrayList<>();
        list.add(aggregateLicenses());
        list.add(aggregateReferenceLicenses());
        list.add(aggregateSources());
        list.add(applyBusinessCase());
        list.add(attachMetadata());
        list.add(convertAssessments());
        list.add(copyInventories());
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
        list.add(updateIndex_External());
        list.add(validateReferenceInventory());
        return list;
    }


    private static ProcessorDefinitions.MavenProcessor aggregateLicenses() {
        return mavenProcessor(AGGREGATE_LICENSES, "Aggregate Licenses", "UTIL", "util/util_aggregate-licenses.xml", "process-resources", "This process enables the aggregation of license and component information using a reference inventory and Terms Metadata (TMD) for a specified inventory. The content will be generated to the specified target directories. This process is a part of the creation of a Software Distribution Annex.",
            processorParameter(ENV_TMD_PASSWORD, true),
            processorParameter(ENV_TMD_USERKEYS_FILE, true),
            processorParameter(INPUT_INVENTORY_FILE, true),
            processorParameter(ENV_TMD_SOURCE, false),
            processorParameter(PARAM_FAIL_ON_MISSING_COMPONENT_FILES, false),
            processorParameter(PARAM_FAIL_ON_MISSING_LICENSE_FILE, false),
            processorParameter(PARAM_REFERENCE_COMPONENT_PATH, false),
            processorParameter(PARAM_REFERENCE_INVENTORY_DIR, false),
            processorParameter(PARAM_REFERENCE_INVENTORY_INCLUDES, false),
            processorParameter(PARAM_REFERENCE_LICENSE_PATH, false),
            processorParameter(PARAM_TARGET_COMPONENT_DIR, false),
            processorParameter(PARAM_TARGET_LICENSE_DIR, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor aggregateReferenceLicenses() {
        return mavenProcessor(AGGREGATE_REFERENCE_LICENSES, "Aggregate Reference Licenses", "UTIL", "util/util_aggregate-reference-licenses.xml", "process-resources", "This process enables the aggregation of license and component information from a reference inventory for the specified inventory. The content will be generated to the specified target directories. This process is a part of the creation of a Software Distribution Annex.",
            processorParameter(INPUT_INVENTORY_FILE, true),
            processorParameter(PARAM_REFERENCE_INVENTORY_DIR, true),
            processorParameter(PARAM_FAIL_ON_MISSING_LICENSE_FILE, false),
            processorParameter(PARAM_REFERENCE_COMPONENT_PATH, false),
            processorParameter(PARAM_REFERENCE_INVENTORY_INCLUDES, false),
            processorParameter(PARAM_REFERENCE_LICENSE_PATH, false),
            processorParameter(PARAM_TARGET_COMPONENT_DIR, false),
            processorParameter(PARAM_TARGET_LICENSE_DIR, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor aggregateSources() {
        return mavenProcessor(AGGREGATE_SOURCES, "Aggregate Sources", "UTIL", "util/util_aggregate-sources.xml", "validate", "Checks a reference inventory for the artifacts contained within and downloads them from different configured data sources. This process is a precursor to generating an annex-document, which requires the additional artifact archives during generation.",
            processorParameter(INPUT_INVENTORY_FILE, true),
            processorParameter(OUTPUT_TARGET_DIR, true),
            processorParameter(PARAM_CONFIG_FILE, true),
            processorParameter(PARAM_PROTOCOL_FILE, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor applyBusinessCase() {
        return mavenProcessor(APPLY_BUSINESS_CASE, "Apply Business Case", "UTIL", "util/util_apply-business-case.xml", "process-resources", "An inventory can be evaluated in a defined business case and documentation context. This processor enables to apply business case specific modulation of an inventory. E.g. before producing a distribution annex.",
            processorParameter(ENV_TMD_PASSWORD, true),
            processorParameter(ENV_TMD_USERKEYS_FILE, true),
            processorParameter(INPUT_INVENTORY_FILE, true),
            processorParameter(OUTPUT_INVENTORY_FILE, true),
            processorParameter(ENV_TMD_SOURCE, false),
            processorParameter(PARAM_LANGUAGE_MODE, false),
            processorParameter(PARAM_NOTICE_MODE_OVERWRITE, false),
            processorParameter(PARAM_SOURCE_MODE, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor attachMetadata() {
        return mavenProcessor(ATTACH_METADATA, "Attach Metadata", "ADVISE", "advise/advise_attach-metadata.xml", "process-resources", "This process attaches specified metadata to a given input inventory. This process can be triggered before dashboard / report creation to ensure that necessary metadata is available.",
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
        return mavenProcessor(CONVERT_ASSESSMENTS, "Convert Assessments", "UTIL", "util/util_convert-assessments.xml", "process-resources", "This process converts assessments, based on an older version of the assessment format to the newest assessment format.",
            processorParameter(INPUT_ASSESSMENT_DIR, true),
            processorParameter(OUTPUT_ASSESSMENT_DIR, true),
            processorParameter(PARAM_OUTPUT_FORMAT, true),
            processorParameter(PARAM_OUTPUT_MODE, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor copyInventories() {
        return mavenProcessor(COPY_INVENTORIES, "Copy Inventories", "UTIL", "util/util_copy-inventories.xml", "process-resources", "Copies a list of inventories to a directory. This is a utility processor used to copy different inventories from individual locations to a common directory.",
            processorParameter(OUTPUT_INVENTORIES_DIR, true),
            processorParameter(PARAM_INVENTORIES_LIST, true),
            processorParameter(INPUT_BASE_DIR, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor copyPomDependencies() {
        return mavenProcessor(COPY_POM_DEPENDENCIES, "Copy Pom Dependencies", "FETCH", "extract/extract_copy-pom-dependencies.xml", "process-resources", "This process copies dependencies found in a pom.xml file into a directory for further processing.",
            processorParameter(OUTPUT_DEPENDENCIES_DIR, true),
            processorParameter(PARAM_ARTIFACT_ID, true),
            processorParameter(PARAM_EXCLUDE_TRANSITIVE_ENABLED, true),
            processorParameter(PARAM_GROUP_ID, true),
            processorParameter(PARAM_VERSION, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor copyResources() {
        return mavenProcessor(COPY_RESOURCES, "Copy Resources", "AGGREGATE", "portfolio/portfolio_copy-resources.xml", "process-resources", "This process copies files needed for creating the overview report. The different sources need to be specified and are copied into the intended directory structure for the overview creation.",
            processorParameter(INPUT_ADVISOR_INVENTORIES_DIR, true),
            processorParameter(INPUT_DASHBOARDS_DIR, true),
            processorParameter(INPUT_INVENTORIES_DIR, true),
            processorParameter(INPUT_REPORTS_DIR, true),
            processorParameter(OUTPUT_RESOURCES_DIR, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor createAnnexArchive() {
        return mavenProcessor(CREATE_ANNEX_ARCHIVE, "Create Annex Archive", "REPORT", "report/report_create-annex-archive.xml", "package", "This process is for generating the archive .zip containing all Annex relevant content. The .zip contains the PDF document as well as the aggregated license and component directories of the inventory.",
            processorParameter(OUTPUT_ANNEX_ARCHIVE_FILE, true),
            processorParameter(INPUT_DOCUMENT_DE_PDF_FILE, false),
            processorParameter(INPUT_DOCUMENT_EN_PDF_FILE, false),
            processorParameter(INPUT_INVENTORY_COMPONENTS_DIR, false),
            processorParameter(INPUT_INVENTORY_LICENSES_DIR, false),
            processorParameter(INPUT_INVENTORY_SOURCES_DIR, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor createDashboard() {
        return mavenProcessor(CREATE_DASHBOARD, "Create Dashboard", "REPORT", "advise/advise_create-dashboard.xml", "set-inventory-info", "This process takes an enriched input inventory (see [advise_enrich-inventory](advise_enrich-inventory.md)) and creates a Vulnerability Assessment Dashboard from it. Additional parameters can influence the information contained in the resulting dashboard which are listed in the table below.",
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
        return mavenProcessor(CREATE_DIFF, "Create Diff", "UTIL", "util/util_create-diff.xml", "vulnerability-differ", "This process creates two output files containing the differences between two provided inventory versions. Which inventory version is declared as \"base\" and which as \"compare\" is negligible since the comparison is done in both ways and saved separately. The parameters \"product.version\" and \"product.version.compare\" are only used for naming the two output files.",
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
        return mavenProcessor(CREATE_DOCUMENT, "Create Document", "REPORT", "report/report_create-document.xml", "process-resources", "This process creates a document for a selected document type. Types to choose from are vulnerability report (VR), cert report (CR), software distribution annex (SDA), license documentation (LD) and initial license documentation (ILD). The document is generated for a specified set of inventories using an asset descriptor. The asset descriptor along with the specified inventories is consumed from user-specified sources. Updating the asset descriptor and providing the inventories is the responsibility of the user. The generated document (.pdf) along with aggregated sources (annex .zip) will be saved to the output directory. The different document types require different parameters, the following table lists them and their usage/description:",
            processorParameter(ENV_KONTINUUM_DIR, true),
            processorParameter(ENV_WORKBENCH_DIR, true),
            processorParameter(INPUT_INVENTORY_DIR, true),
            processorParameter(OUTPUT_DOCUMENT_FILE, true),
            processorParameter(PARAM_ASSET_DESCRIPTOR_FILE, true),
            processorParameter(PARAM_ASSET_ID, true),
            processorParameter(PARAM_ASSET_NAME, true),
            processorParameter(PARAM_ASSET_VERSION, true),
            processorParameter(PARAM_DOCUMENT_TYPE, true),
            processorParameter(PARAM_PRODUCT_NAME, true),
            processorParameter(PARAM_PRODUCT_VERSION, true),
            processorParameter(PARAM_PRODUCT_WATERMARK, true),
            processorParameter(PARAM_PROPERTY_SELECTOR_ORGANIZATION, true),
            processorParameter(ENV_KONTINUUM_PROCESSORS_DIR, false),
            processorParameter(ENV_VULNERABILITY_MIRROR_DIR, false),
            processorParameter(ENV_WORKBENCH_PROCESSORS_DIR, false),
            processorParameter(PARAM_ASSET_BUILD, false),
            processorParameter(PARAM_COMPUTED_INVENTORY_DIR, false),
            processorParameter(PARAM_DOCUMENT_LANGUAGE, false),
            processorParameter(PARAM_OVERVIEW_ADVISORS, false),
            processorParameter(PARAM_PROPERTY_SELECTOR_CLASSIFICATION, false),
            processorParameter(PARAM_PROPERTY_SELECTOR_CONTROL, false),
            processorParameter(PARAM_REFERENCE_COMPONENT_DIR, false),
            processorParameter(PARAM_REFERENCE_INVENTORY_DIR, false),
            processorParameter(PARAM_REFERENCE_LICENSE_DIR, false),
            processorParameter(PARAM_SECURITY_POLICY_FILE, false),
            processorParameter(PARAM_TEMPLATE_DIR, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor createOverview() {
        return mavenProcessor(CREATE_OVERVIEW, "Create Overview", "REPORT", "portfolio/portfolio_create-overview.xml", "create-report", "This process creates an overview with the resources copied with the portfolio_copy-resources.xml processor.",
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
        return mavenProcessor(CYCLONEDX_TO_INVENTORY, "Cyclonedx To Inventory", "EXTRACT", "convert/convert_cyclonedx-to-inventory.xml", "convert-cyclonedx-to-inventory", "Used to convert a CycloneDX document into an inventory.",
            processorParameter(INPUT_BOM_FILE, true),
            processorParameter(OUTPUT_INVENTORY_FILE, true),
            processorParameter(PARAM_DERIVE_ATTRIBUTES_FROM_PURL_ENABLED, false),
            processorParameter(PARAM_INCLUDE_ASSETS_ENABLED, false),
            processorParameter(PARAM_INCLUDE_LICENSES_ENABLED, false),
            processorParameter(PARAM_INCLUDE_METADATA_COMPONENT_ENABLED, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor downloadAsset() {
        return mavenProcessor(DOWNLOAD_ASSET, "Download Asset", "FETCH", "fetch/fetch_download-asset.xml", "process-resources", "This processor downloads a remote asset from a specified URL into a target directory.",
            processorParameter(OUTPUT_ASSET_DIR, true),
            processorParameter(PARAM_ASSET_URL, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor downloadDataSources() {
        return mavenProcessor(DOWNLOAD_DATA_SOURCES, "Download Data Sources", "PRE", "mirror/mirror_download-data-sources.xml", "data-mirror", "This process downloads the vulnerability mirror from different data sources.",
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
        return mavenProcessor(DOWNLOAD_INDEX, "Download Index", "PRE", "mirror/mirror_download-index.xml", "compile", "This process downloads the vulnerability mirror index to a specified target directory to be used for later enrichment of inventories.",
            processorParameter(ENV_VULNERABILITY_MIRROR_DIR, true),
            processorParameter(PARAM_MIRROR_ARCHIVE_URL, true),
            processorParameter(PARAM_MIRROR_ARCHIVE_PASSWORD, false),
            processorParameter(PARAM_MIRROR_ARCHIVE_USERNAME, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor enrichAdvisors() {
        return mavenProcessor(ENRICH_ADVISORS, "Enrich Advisors", "UTIL", "util/util_enrich-advisors.xml", "generate-sources", "This process enriches an inventory based on the specified advisors. It is only used for cert-report document generation.",
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
        return mavenProcessor(ENRICH_INVENTORY, "Enrich Inventory", "ADVISE", "advise/advise_enrich-inventory.xml", "set-inventory-info", "This process takes an input inventory and enriches it with vulnerability information. Additional configurations can influence the information contained in the resulting inventory such as which vulnerability databases should be used, custom vulnerabilities and many more, listed in the table below.",
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
        return mavenProcessor(ENRICH_WITH_REFERENCE, "Enrich With Reference", "ADVISE", "util/util_enrich-with-reference.xml", "process-resources", "This process enriches a specified input inventory with a reference inventory to curate it with further information for later enrichment.",
            processorParameter(INPUT_INVENTORY_FILE, true),
            processorParameter(OUTPUT_INVENTORY_FILE, true),
            processorParameter(PARAM_REFERENCE_INVENTORY_DIR, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor executeKotlinScript() {
        return mavenProcessor(EXECUTE_KOTLIN_SCRIPT, "Execute Kotlin Script", "UTIL", "util/util_execute-kotlin-script.xml", "process-resources", "This process executes a specified kotlin script file. Instead of calling the `kotlin` CLI directly, the script is executed via the `ae-kotlin-scripting-maven-plugin` (akin to `util_transform-inventories`). The script is evaluated as an `InventoryFilterScript` and receives its arguments as a named parameter map, accessed in the script via the implicit `params` receiver.",
            processorParameter(INPUT_KOTLIN_SCRIPT_FILE, true),
            processorParameter(INPUT_PROPERTIES_FILE, false),
            processorParameter(INPUT_WORKSPACE_DIR, false),
            processorParameter(OUTPUT_ENV_FILE, false),
            processorParameter(PARAM_CURL_ARGUMENTS, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor generateReportSvg() {
        return mavenProcessor(GENERATE_REPORT_SVG, "Generate Report Svg", "UTIL", "util/util_generate-report-svg.xml", "generate-sources", "This process generates the SVG resources for different types of vulnerability reports for a specified inventory.",
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
        return mavenProcessor(INVENTORY_TO_CYCLONEDX, "Inventory To Cyclonedx", "REPORT", "convert/convert_inventory-to-cyclonedx.xml", "convert-inventory-to-cyclonedx", "Used to convert an inventory into a CycloneDX BOM either xml or json format.",
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
        return mavenProcessor(INVENTORY_TO_SPDX, "Inventory To Spdx", "REPORT", "convert/convert_inventory-to-spdx.xml", "convert-inventory-to-spdx", "This process converts an inventory, independent of which stage it was produced in, into an SPDX document. All available parameters are listed in the table below. Parameters marked as \"not required\" already have default values associated.",
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
        return mavenProcessor(MERGE_ADVISORS, "Merge Advisors", "UTIL", "util/util_merge-advisors.xml", "generate-sources", "This process merges inventories based on an individual security advisor and filters them using the security policy.",
            processorParameter(INPUT_INVENTORY_DIR, true),
            processorParameter(OUTPUT_INVENTORY_FILE, true),
            processorParameter(PARAM_SECURITY_POLICY_FILE, true),
            processorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor mergeAssessments() {
        return mavenProcessor(MERGE_ASSESSMENTS, "Merge Assessments", "UTIL", "util/util_merge-assessments.xml", "generate-sources", "This process merges inventories and their assessments.",
            processorParameter(INPUT_INVENTORY_DIR, true),
            processorParameter(OUTPUT_INVENTORY_FILE, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor mergeInventories() {
        return mavenProcessor(MERGE_INVENTORIES, "Merge Inventories", "UTIL", "util/util_merge-inventories.xml", "generate-sources", "This process merges multiple input inventories into one output inventory. The input inventories are specified using a directory. Additionally, a regex can be provided to further specify the inventories used for merging within the given directory. This process can be triggered at any point in the pipeline and is not bound to a phase.",
            processorParameter(INPUT_INVENTORY_DIR, true),
            processorParameter(OUTPUT_INVENTORY_FILE, true),
            processorParameter(PARAM_INVENTORY_INCLUDES, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor portfolioDownload() {
        return mavenProcessor(PORTFOLIO_DOWNLOAD, "Portfolio Download", "PRE", "aggregate/aggregate_portfolio-download.xml", "process-resources", "This process enables the download of specific assets from a running portfolio manager service.",
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
        return mavenProcessor(PORTFOLIO_DOWNLOAD_JARS, "Portfolio Download Jars", "UTIL", "util/util_portfolio-download-jars.xml", "process-resources", "This process downloads the service and cli jars needed to run the portfolio manager. This is only necessary if the jars do not exist locally already.",
            processorParameter(INPUT_CLI_DIR, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor portfolioUpload() {
        return mavenProcessor(PORTFOLIO_UPLOAD, "Portfolio Upload", "POST", "prepare/prepare_portfolio-upload.xml", "process-resources", "This process enables the upload of specific assets to a running portfolio manager service.",
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
        return mavenProcessor(RESOLVE_INVENTORY, "Resolve Inventory", "RESOLVE", "resolve/resolve_resolve-inventory.xml", "process-resources", "Used to resolve all artifacts contained in an inventory and gather additional information on those artifacts.",
            processorParameter(ENV_MAVEN_INDEX_DIR, true),
            processorParameter(INPUT_INVENTORY_FILE, true),
            processorParameter(OUTPUT_INVENTORY_FILE, true),
            processorParameter(PARAM_ARTIFACT_RESOLVER_CONFIG_FILE, true),
            processorParameter(PARAM_ARTIFACT_RESOLVER_PROXY_FILE, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor saveInspectImage() {
        return mavenProcessor(SAVE_INSPECT_IMAGE, "Save Inspect Image", "FETCH", "fetch/fetch_inspect-image.xml", "process-resources", "This process saves and inspects a docker container image via its id and version. The extracted container information is then saved into a specified directory for further processing.",
            processorParameter(OUTPUT_DIR, true),
            processorParameter(PARAM_IMAGE_ID, true),
            processorParameter(PARAM_IMAGE_VERSION, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor scanDirectory() {
        return mavenProcessor(SCAN_DIRECTORY, "Scan Directory", "PREPARE", "prepare/prepare_scan-directory.xml", "process-resources", "This process scans a directory containing extracted / prepared artifacts into an inventory. The artifacts contained in the input directory are usually a result of the \"prepare\" process which extracts information from container images, dependencies listed in poms and so on.",
            processorParameter(INPUT_EXTRACT_DIR, true),
            processorParameter(OUTPUT_INVENTORY_FILE, true),
            processorParameter(OUTPUT_SCAN_DIR, true),
            processorParameter(PARAM_REFERENCE_INVENTORY_DIR, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor scanInventory() {
        return mavenProcessor(SCAN_INVENTORY, "Scan Inventory", "SCAN", "scan/scan_scan-inventory.xml", "process-resources", "Scans a resolved inventory for licenses and copyrights and writes the resulting information to an inventory.",
            processorParameter(ENV_KOSMOS_PASSWORD, true),
            processorParameter(ENV_KOSMOS_USERKEYS_FILE, true),
            processorParameter(INPUT_INVENTORY_FILE, true),
            processorParameter(INPUT_OUTPUT_ANALYSIS_BASE_DIR, true),
            processorParameter(OUTPUT_INVENTORY_FILE, true),
            processorParameter(PARAM_PROPERTIES_FILE, true)
        );
    }
    private static ProcessorDefinitions.MavenProcessor transformInventories() {
        return mavenProcessor(TRANSFORM_INVENTORIES, "Transform Inventories", "UTIL", "util/util_transform-inventories.xml", "execute-script", "This process performs a transformation on a specified inventory using a Kotlin Script. These transformations can range from inventory merging to field replacements.",
            processorParameter(INPUT_INVENTORY_DIR, true),
            processorParameter(OUTPUT_INVENTORY_DIR, true),
            processorParameter(PARAM_KOTLIN_SCRIPT_FILE, true),
            processorParameter(PARAM_ASSET_NAME, false),
            processorParameter(PARAM_FILTER_PRESET, false)
        );
    }
    private static ProcessorDefinitions.MavenProcessor updateIndex() {
        return mavenProcessor(UPDATE_INDEX, "Update Index", "PRE", "mirror/mirror_update-index.xml", "data-mirror", "This processor creates or updates the indices of the mirror. It uses the previously downloaded data files of the external data sources. As a result the specified mirror directory is extended with the index files.",
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
    private static ProcessorDefinitions.MavenProcessor updateIndex_External() {
        return mavenProcessor(UPDATE_INDEX_EXTERNAL, "Update Index_external", "PRE", "mirror/mirror_update-index_external.xml", "data-mirror", "This processor creates or updates the indices of the mirror. It uses the previously downloaded data files of the external data sources. As a result the specified mirror directory is extended with the index files.",
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
        return mavenProcessor(VALIDATE_REFERENCE_INVENTORY, "Validate Reference Inventory", "UTIL", "util/util_validate-reference-inventory.xml", "compile", "This process takes an input inventory directory and validates the contained inventories.",
            processorParameter(INPUT_INVENTORY_DIR, true)
        );
    }

    private static ProcessorDefinitions.MavenProcessor mavenProcessor(
            ProcessorIds id, String name, String stage, String pomLocation, String goal, String description,
            ProcessorDefinitions.ProcessorParameter... parameters) {
        ProcessorDefinitions.MavenProcessor processor = new ProcessorDefinitions.MavenProcessor(
                description, pomLocation, goal, Arrays.asList(parameters), null, null);
        processor.setId(id.getValue());
        processor.setName(name);
        processor.setStage(stage);
        return processor;
    }

    private static ProcessorDefinitions.ProcessorParameter processorParameter(
            ProcessorParameterKey key, boolean required) {
        return new ProcessorDefinitions.ProcessorParameter(key, required, null);
    }
}
