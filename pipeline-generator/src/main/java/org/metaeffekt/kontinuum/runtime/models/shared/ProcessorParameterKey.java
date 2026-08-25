package org.metaeffekt.kontinuum.runtime.models.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe enumeration of all processor parameter keys across the Kontinuum processor catalog.
 */
public enum ProcessorParameterKey {

    ENV_KONTINUUM_DIR("env.kontinuum.dir"),
    ENV_KONTINUUM_PROCESSORS_DIR("env.kontinuum.processors.dir"),
    ENV_KOSMOS_PASSWORD("env.kosmos.password"),
    ENV_KOSMOS_USERKEYS_FILE("env.kosmos.userkeys.file"),
    ENV_MAVEN_INDEX_DIR("env.maven.index.dir"),
    ENV_MIRROR_DIR("env.mirror.dir"),
    ENV_NVD_APIKEY("env.nvd.apikey"),
    ENV_TMD_PASSWORD("env.tmd.password"),
    ENV_TMD_SOURCE("env.tmd.source"),
    ENV_TMD_USERKEYS_FILE("env.tmd.userkeys.file"),
    ENV_VULNERABILITY_ASSESSMENT_API("env.vulnerability.assessment.api"),
    ENV_VULNERABILITY_MIRROR_DIR("env.vulnerability.mirror.dir"),
    ENV_WORKBENCH_DIR("env.workbench.dir"),
    ENV_WORKBENCH_PROCESSORS_DIR("env.workbench.processors.dir"),
    INPUT_ADVISOR_INVENTORIES_DIR("input.advisor.inventories.dir"),
    INPUT_ASSESSMENT_DIR("input.assessment.dir"),
    INPUT_BASE_DIR("input.base.dir"),
    INPUT_BOM_FILE("input.bom.file"),
    INPUT_CLI_DIR("input.cli.dir"),
    INPUT_DASHBOARDS_DIR("input.dashboards.dir"),
    INPUT_DOCUMENT_DE_PDF_FILE("input.document.de.pdf.file"),
    INPUT_DOCUMENT_EN_PDF_FILE("input.document.en.pdf.file"),
    INPUT_EXTRACT_DIR("input.extract.dir"),
    INPUT_FILE("input.file"),
    INPUT_INVENTORIES_DIR("input.inventories.dir"),
    INPUT_INVENTORY_COMPARE_FILE("input.inventory.compare.file"),
    INPUT_INVENTORY_COMPONENTS_DIR("input.inventory.components.dir"),
    INPUT_INVENTORY_DIR("input.inventory.dir"),
    INPUT_INVENTORY_FILE("input.inventory.file"),
    INPUT_INVENTORY_LICENSES_DIR("input.inventory.licenses.dir"),
    INPUT_INVENTORY_PATH("input.inventory.path"),
    INPUT_INVENTORY_SOURCES_DIR("input.inventory.sources.dir"),
    INPUT_KOTLIN_SCRIPT_FILE("input.kotlin.script.file"),
    INPUT_OUTPUT_ANALYSIS_BASE_DIR("input.output.analysis.base.dir"),
    INPUT_PROPERTIES_FILE("input.properties.file"),
    INPUT_REPORTS_DIR("input.reports.dir"),
    INPUT_WORKSPACE_DIR("input.workspace.dir"),
    OUTPUT_ANNEX_ARCHIVE_FILE("output.annex.archive.file"),
    OUTPUT_ASSESSMENT_DIR("output.assessment.dir"),
    OUTPUT_ASSET_DIR("output.asset.dir"),
    OUTPUT_BOM_FILE("output.bom.file"),
    OUTPUT_DASHBOARD_FILE("output.dashboard.file"),
    OUTPUT_DEPENDENCIES_DIR("output.dependencies.dir"),
    OUTPUT_DIR("output.dir"),
    OUTPUT_DOCUMENT_FILE("output.document.file"),
    OUTPUT_ENV_FILE("output.env.file"),
    OUTPUT_INVENTORIES_DIR("output.inventories.dir"),
    OUTPUT_INVENTORY_DIR("output.inventory.dir"),
    OUTPUT_INVENTORY_FILE("output.inventory.file"),
    OUTPUT_NOTIFICATION_FILE("output.notification.file"),
    OUTPUT_OVERVIEW_FILE("output.overview.file"),
    OUTPUT_RESOURCES_DIR("output.resources.dir"),
    OUTPUT_SCAN_DIR("output.scan.dir"),
    OUTPUT_SVG_DIR("output.svg.dir"),
    OUTPUT_TARGET_DIR("output.target.dir"),
    OUTPUT_TMP_DIR("output.tmp.dir"),
    PARAM_ACTIVATE_CAPEC("param.activate.capec"),
    PARAM_ACTIVATE_CERTEU("param.activate.certeu"),
    PARAM_ACTIVATE_CERTFR("param.activate.certfr"),
    PARAM_ACTIVATE_CERTSEI("param.activate.certsei"),
    PARAM_ACTIVATE_CORRELATION("param.activate.correlation"),
    PARAM_ACTIVATE_CSAF("param.activate.csaf"),
    PARAM_ACTIVATE_CWE("param.activate.cwe"),
    PARAM_ACTIVATE_EOL("param.activate.eol"),
    PARAM_ACTIVATE_EPSS("param.activate.epss"),
    PARAM_ACTIVATE_KEV("param.activate.kev"),
    PARAM_ACTIVATE_KEYWORDS("param.activate.keywords"),
    PARAM_ACTIVATE_MITRE_ATLAS("param.activate.mitre.atlas"),
    PARAM_ACTIVATE_MITRE_ATTACK("param.activate.mitre.attack"),
    PARAM_ACTIVATE_MSRC("param.activate.msrc"),
    PARAM_ACTIVATE_NVD("param.activate.nvd"),
    PARAM_ACTIVATE_OSV("param.activate.osv"),
    PARAM_ACTIVATE_OSV_PROVIDERS("param.activate.osv.providers"),
    PARAM_ACTIVATE_PURL_DERIVATION("param.activate.purl.derivation"),
    PARAM_ACTIVATE_STATUS("param.activate.status"),
    PARAM_ACTIVATE_THREAT("param.activate.threat"),
    PARAM_ACTIVATE_VALIDATION("param.activate.validation"),
    PARAM_ACTIVATE_VULNERABILITIES_CUSTOM("param.activate.vulnerabilities.custom"),
    PARAM_ARTIFACT_ID("param.artifact.id"),
    PARAM_ARTIFACT_RESOLVER_CONFIG_FILE("param.artifact.resolver.config.file"),
    PARAM_ARTIFACT_RESOLVER_PROXY_FILE("param.artifact.resolver.proxy.file"),
    PARAM_ASSESSMENT_CONTEXT("param.assessment.context"),
    PARAM_ASSESSMENT_DIRS("param.assessment.dirs"),
    PARAM_ASSESSMENT_LABELS("param.assessment.labels"),
    PARAM_ASSET_BUILD("param.asset.build"),
    PARAM_ASSET_DESCRIPTOR_FILE("param.asset.descriptor.file"),
    PARAM_ASSET_GROUP_ID("param.asset.group.id"),
    PARAM_ASSET_ID("param.asset.id"),
    PARAM_ASSET_NAME("param.asset.name"),
    PARAM_ASSET_URL("param.asset.url"),
    PARAM_ASSET_VERSION("param.asset.version"),
    PARAM_COMPUTED_INVENTORY_DIR("param.computed.inventory.dir"),
    PARAM_CONFIG_FILE("param.config.file"),
    PARAM_CONTEXT_DIRS("param.context.dirs"),
    PARAM_CORRELATION_DIR("param.correlation.dir"),
    PARAM_CURL_ARGUMENTS("param.curl.arguments"),
    PARAM_CUSTOM_LICENSE_MAPPINGS("param.custom.license.mappings"),
    PARAM_CVSS_ACTIVE("param.cvss.active"),
    PARAM_CVSS_VULNERABILITY_COUNT_LIMIT("param.cvss.vulnerability.count.limit"),
    PARAM_DASHBOARD_FOOTER("param.dashboard.footer"),
    PARAM_DASHBOARD_SUBTITLE("param.dashboard.subtitle"),
    PARAM_DASHBOARD_TITLE("param.dashboard.title"),
    PARAM_DERIVE_ATTRIBUTES_FROM_PURL_ENABLED("param.derive.attributes.from.purl.enabled"),
    PARAM_DOCUMENT_COMMENT("param.document.comment"),
    PARAM_DOCUMENT_DESCRIPTION("param.document.description"),
    PARAM_DOCUMENT_ID_PREFIX("param.document.id.prefix"),
    PARAM_DOCUMENT_LANGUAGE("param.document.language"),
    PARAM_DOCUMENT_NAME("param.document.name"),
    PARAM_DOCUMENT_ORGANIZATION("param.document.organization"),
    PARAM_DOCUMENT_ORGANIZATION_URL("param.document.organization.url"),
    PARAM_DOCUMENT_OUTPUT_FORMAT("param.document.output.format"),
    PARAM_DOCUMENT_PERSON("param.document.person"),
    PARAM_DOCUMENT_TYPE("param.document.type"),
    PARAM_DOCUMENT_VERSION("param.document.version"),
    PARAM_EVENTS_SINCE_TIMESTAMP_FOR_DASHBOARD("param.events.since.timestamp.for.dashboard"),
    PARAM_EXCLUDE_NVD_EQUIVALENT_MSRC("param.exclude.nvd.equivalent.msrc"),
    PARAM_EXCLUDE_NVD_EQUIVALENT_OSV("param.exclude.nvd.equivalent.osv"),
    PARAM_EXCLUDE_TRANSITIVE_ENABLED("param.exclude.transitive.enabled"),
    PARAM_FAIL_ON_ERROR("param.fail.on.error"),
    PARAM_FAIL_ON_ISSUE("param.fail.on.issue"),
    PARAM_FAIL_ON_MISSING_COMPONENT_FILES("param.fail.on.missing.component.files"),
    PARAM_FAIL_ON_MISSING_LICENSE_FILE("param.fail.on.missing.license.file"),
    PARAM_FILTER_PRESET("param.filter.preset"),
    PARAM_GROUP_ID("param.group.id"),
    PARAM_IMAGE_ID("param.image.id"),
    PARAM_IMAGE_VERSION("param.image.version"),
    PARAM_INCLUDE_ASSETS_ENABLED("param.include.assets.enabled"),
    PARAM_INCLUDE_LICENSE_TEXTS_ENABLED("param.include.license.texts.enabled"),
    PARAM_INCLUDE_LICENSES_ENABLED("param.include.licenses.enabled"),
    PARAM_INCLUDE_METADATA_COMPONENT_ENABLED("param.include.metadata.component.enabled"),
    PARAM_INVENTORIES_LIST("param.inventories.list"),
    PARAM_INVENTORY_COMPARE_VERSION("param.inventory.compare.version"),
    PARAM_INVENTORY_INCLUDES("param.inventory.includes"),
    PARAM_INVENTORY_MODIFIER("param.inventory.modifier"),
    PARAM_INVENTORY_VERSION("param.inventory.version"),
    PARAM_KEYSTORE_CONFIG_FILE("param.keystore.config.file"),
    PARAM_KEYSTORE_PASSWORD("param.keystore.password"),
    PARAM_KOTLIN_SCRIPT_FILE("param.kotlin.script.file"),
    PARAM_LANGUAGE_MODE("param.language.mode"),
    PARAM_LICENSE_EXPRESSIONS_ENABLED("param.license.expressions.enabled"),
    PARAM_MAP_RELATIONSHIPS_ENABLED("param.map.relationships.enabled"),
    PARAM_METADATA_ASSET_ID("param.metadata.asset.id"),
    PARAM_METADATA_ASSET_NAME("param.metadata.asset.name"),
    PARAM_METADATA_ASSET_PATH("param.metadata.asset.path"),
    PARAM_METADATA_ASSET_TYPE("param.metadata.asset.type"),
    PARAM_METADATA_ASSET_VERSION("param.metadata.asset.version"),
    PARAM_MIRROR_ARCHIVE_PASSWORD("param.mirror.archive.password"),
    PARAM_MIRROR_ARCHIVE_URL("param.mirror.archive.url"),
    PARAM_MIRROR_ARCHIVE_USERNAME("param.mirror.archive.username"),
    PARAM_NOTICE_MODE_OVERWRITE("param.notice.mode.overwrite"),
    PARAM_NOTIFICATION_CONFIG_FILE("param.notification.config.file"),
    PARAM_NOTIFICATION_RULE_FILE("param.notification.rule.file"),
    PARAM_OUTPUT_FORMAT("param.output.format"),
    PARAM_OUTPUT_MODE("param.output.mode"),
    PARAM_OVERVIEW_ACTIVE("param.overview.active"),
    PARAM_OVERVIEW_ADVISORS("param.overview.advisors"),
    PARAM_PORTFOLIO_MANAGER_TOKEN("param.portfolio.manager.token"),
    PARAM_PORTFOLIO_MANAGER_URL("param.portfolio.manager.url"),
    PARAM_PRODUCT_NAME("param.product.name"),
    PARAM_PRODUCT_VERSION("param.product.version"),
    PARAM_PRODUCT_WATERMARK("param.product.watermark"),
    PARAM_PROJECT_NAME("param.project.name"),
    PARAM_PROPERTIES_FILE("param.properties.file"),
    PARAM_PROPERTY_SELECTOR_CLASSIFICATION("param.property.selector.classification"),
    PARAM_PROPERTY_SELECTOR_CONTROL("param.property.selector.control"),
    PARAM_PROPERTY_SELECTOR_ORGANIZATION("param.property.selector.organization"),
    PARAM_PROTOCOL_FILE("param.protocol.file"),
    PARAM_PROXY_HOST("param.proxy.host"),
    PARAM_PROXY_PASS("param.proxy.pass"),
    PARAM_PROXY_PORT("param.proxy.port"),
    PARAM_PROXY_SCHEME("param.proxy.scheme"),
    PARAM_PROXY_USER("param.proxy.user"),
    PARAM_PUT_EVENT_FOR_DASHBOARD("param.put.event.for.dashboard"),
    PARAM_REFERENCE_COMPONENT_DIR("param.reference.component.dir"),
    PARAM_REFERENCE_COMPONENT_PATH("param.reference.component.path"),
    PARAM_REFERENCE_INVENTORY_DIR("param.reference.inventory.dir"),
    PARAM_REFERENCE_INVENTORY_INCLUDES("param.reference.inventory.includes"),
    PARAM_REFERENCE_LICENSE_DIR("param.reference.license.dir"),
    PARAM_REFERENCE_LICENSE_PATH("param.reference.license.path"),
    PARAM_REMOVE_GHSA_UNREVIEWED("param.remove.ghsa.unreviewed"),
    PARAM_REPO_URL("param.repo.url"),
    PARAM_REPORT_PERIOD_SINCE("param.report.period.since"),
    PARAM_REPORT_PERIOD_UNTIL("param.report.period.until"),
    PARAM_SECURITY_POLICY_ACTIVE_IDS("param.security.policy.active.ids"),
    PARAM_SECURITY_POLICY_FILE("param.security.policy.file"),
    PARAM_SOURCE_MODE("param.source.mode"),
    PARAM_TARGET_COMPONENT_DIR("param.target.component.dir"),
    PARAM_TARGET_LICENSE_DIR("param.target.license.dir"),
    PARAM_TECHNICAL_PROPERTIES_ENABLED("param.technical.properties.enabled"),
    PARAM_TEMPLATE_DIR("param.template.dir"),
    PARAM_TENANT_ID("param.tenant.id"),
    PARAM_THREAT_CATALOG_FILE("param.threat.catalog.file"),
    PARAM_TIMELINE_CONF_ENABLED("param.timeline.conf.enabled"),
    PARAM_TIMELINE_MAX_THREADS("param.timeline.max.threads"),
    PARAM_TIMELINE_TIME_SPENT_MAX("param.timeline.time.spent.max"),
    PARAM_TIMELINE_VULN_PROVIDERS_LIST("param.timeline.vuln.providers.list"),
    PARAM_TRUSTSTORE_CONFIG_FILE("param.truststore.config.file"),
    PARAM_TRUSTSTORE_PASSWORD("param.truststore.password"),
    PARAM_VERSION("param.version"),
    PARAM_VULNERABILITIES_CUSTOM_DIR("param.vulnerabilities.custom.dir");

    private final String key;

    private static final Map<String, ProcessorParameterKey> LOOKUP_MAP;

    static {
        Map<String, ProcessorParameterKey> map = new HashMap<>();
        for (ProcessorParameterKey parameterKey : values()) {
            map.put(parameterKey.key, parameterKey);
        }
        LOOKUP_MAP = Collections.unmodifiableMap(map);
    }

    ProcessorParameterKey(String key) {
        this.key = key;
    }

    @JsonValue
    public String getKey() {
        return key;
    }

    @JsonCreator
    public static ProcessorParameterKey fromKey(String key) {
        if (key == null) {
            return null;
        }
        ProcessorParameterKey parameterKey = LOOKUP_MAP.get(key);
        if (parameterKey != null) {
            return parameterKey;
        }
        throw new IllegalArgumentException("Unknown processor parameter key: [" + key + "]");
    }

    @Override
    public String toString() {
        return key;
    }
}
