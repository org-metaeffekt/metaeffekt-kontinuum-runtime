package org.metaeffekt.kontinuum.runtime.generator.shared;

import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Dashboard;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Report;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Options.EnrichmentOptions;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

public class Pipeline {

    private final List<AssetPlan> assetPlans = new ArrayList<>();

    private final Map<Asset, List<Processor>> assetProcessorsMap = new LinkedHashMap<>();

    private final PipelineConfiguration pipelineConfiguration;

    private final Workspace workspace;

    private final EnvironmentConfiguration environmentConfiguration;
    private final YamlProcessorCatalog yamlProcessorCatalog = new YamlProcessorCatalog();

    public Pipeline(PipelineConfiguration pipelineConfiguration,
                    EnvironmentConfiguration environmentConfiguration) {
        this.environmentConfiguration = environmentConfiguration;
        this.pipelineConfiguration = pipelineConfiguration;
        this.workspace = new Workspace(pipelineConfiguration, environmentConfiguration);

        if (pipelineConfiguration.getOptions() == null) {
            pipelineConfiguration.setOptions(new PipelineConfiguration.Options());
        }

        pipelineConfiguration.getProjectProperties().getAssets()
                .forEach(a -> assetPlans.add(new AssetPlan(a, pipelineConfiguration, environmentConfiguration)));
    }

    public Map<Asset, List<Processor>> generatePipeline() {
        for (AssetPlan assetPlan : assetPlans) {
            addPreStageProcessors(assetPlan);
            addFetchStageProcessors(assetPlan);
            addExtractStageProcessors(assetPlan);
            addPrepareStageProcessors(assetPlan);
            addAggregateStageProcessors(assetPlan);
            addResolveStageProcessors(assetPlan);
            addScanStageProcessors(assetPlan);
            addAdviseStageProcessors(assetPlan);
            addGroupStageProcessors(assetPlan);
            addReportStageProcessors(assetPlan);
            addSummarizeStageProcessors(assetPlan);
            addPostStageProcessors(assetPlan);
        }

        return assetProcessorsMap;
    }


    private void addPreStageProcessors(AssetPlan assetPlan) {
        if (assetPlan.isRequireVulnerabilityMirror()) {
            addDownloadIndexProcessor(assetPlan);
        }
    }

    private void addFetchStageProcessors(AssetPlan assetPlan) {
        if (assetPlan.isRequireFetch()) {
            if (assetPlan.getAsset().getUrlResolver() != null) {
                addDownloadAssetProcessor(assetPlan);
            } else if (assetPlan.getAsset().getMavenResolver() != null) {
                addMavenDownloadProcessor(assetPlan);
            }
        }
    }

    private void addExtractStageProcessors(AssetPlan assetPlan) {
        if (assetPlan.isRequireContainerInspect()) {
            addInspectImageProcessor(assetPlan);
        }
    }

    private void addPrepareStageProcessors(AssetPlan assetPlan) {
        if (assetPlan.isRequireExtract()) {
            addScanDirectoryProcessor(assetPlan);
        } else {
            addCopyInventoryProcessor(assetPlan);
        }

        if (assetPlan.isRequireCycloneDx()) {
            addInventoryToCycloneDxProcessor(assetPlan, Stage.PREPARE);
        }

        if (assetPlan.isRequireSpdx()) {
            addInventoryToSpdxProcessor(assetPlan, Stage.PREPARE);
        }

        if (assetPlan.isRequirePortfolioIntegration()) {
            addPortfolioUploadProcessor(assetPlan);
        }
    }

    private void addAggregateStageProcessors(AssetPlan assetPlan) {
        if (assetPlan.isRequirePortfolioIntegration()) {
            addPortfolioDownloadProcessor(assetPlan);
            addEnrichInventoryWithReferenceProcessor(assetPlan, Stage.AGGREGATE,
                    workspace.getStageDirForAsset(assetPlan.getAsset(), Stage.AGGREGATE).appendAssetInventory(),
                    workspace.getStageDirForAsset(assetPlan.getAsset(), Stage.AGGREGATE).toString());
        } else {
            addEnrichInventoryWithReferenceProcessor(assetPlan, Stage.AGGREGATE,
                    workspace.getStageDirForAsset(assetPlan.getAsset(), Stage.PREPARE).appendAssetInventory(),
                    assetPlan.getAsset().getReferenceDir(environmentConfiguration.getWorkbenchDirNormalized()));
        }
    }

    private void addResolveStageProcessors(AssetPlan assetPlan) {
        if (assetPlan.isRequireResolve()) {
            addResolveProcessor(assetPlan);
        }
    }

    private void addScanStageProcessors(AssetPlan assetPlan) {
        if (assetPlan.isRequireLicenseScan()) {
            addScanProcessor(assetPlan);
        }
    }

    private void addAdviseStageProcessors(AssetPlan assetPlan) {
        if (assetPlan.isRequireVulnerabilityEnrichment()) {
            addVulnerabilityEnrichmentProcessor(assetPlan);
        }
    }

    private void addGroupStageProcessors(AssetPlan assetPlan) {}

    private void addReportStageProcessors(AssetPlan assetPlan) {
        addDashboardProcessors(assetPlan);
        addReportProcessors(assetPlan);
    }

    private void addSummarizeStageProcessors(AssetPlan assetPlan) {}

    private void addPostStageProcessors(AssetPlan assetPlan) {}


    private void addDownloadIndexProcessor(AssetPlan assetPlan) {
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("download-index");
        processor.setProcessorParameter(PARAM_MIRROR_ARCHIVE_URL, environmentConfiguration.VULNERABILITY_MIRROR_URL);
        processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR, environmentConfiguration.getMirrorDir());

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addDownloadAssetProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("download-asset");
        processor.setProcessorParameter(PARAM_ASSET_URL, asset.getUrlResolver().getUrl());
        processor.setProcessorParameter(OUTPUT_ASSET_DIR, workspace.getStageDirForAsset(asset, Stage.FETCH).toString());

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addMavenDownloadProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        PipelineConfiguration.ProjectProperties.Asset.MavenResolver mavenResolver = asset.getMavenResolver();

        String groupId = mavenResolver.getGroupId();
        String artifactId = mavenResolver.getArtifactId() != null ? mavenResolver.getArtifactId() : "";
        String version = mavenResolver.getArtifactVersion();
        String repoUrl = mavenResolver.getRepoUrl();
        String fetchedDir = workspace.getStageDirForAsset(asset, Stage.FETCH).toString();

        String scriptPath = environmentConfiguration.getKontinuumProcessorsDirNormalized()
                + "scripts/download-maven-artifacts.sh";
        String invocation = "bash \"" + scriptPath + "\" \"" + groupId + "\" \""
                + artifactId + "\" \"" + version + "\" \""
                + fetchedDir + "\" \"" + repoUrl + "\"";

        ProcessorDefinitions.StandaloneProcessor standaloneProcessor =
                new ProcessorDefinitions.StandaloneProcessor("download-maven-artifacts", "Download Maven Artifacts", Stage.FETCH.name());
        standaloneProcessor.setScript(invocation);
        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(standaloneProcessor);
    }

    private void addInspectImageProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("save-inspect-image");

        processor.setProcessorParameter(OUTPUT_DIR, workspace.getStageDirForAsset(asset, Stage.PREPARE).toString());
        processor.setProcessorParameter(PARAM_IMAGE_ID,
                asset.getContainerResolver().getImage());
        processor.setProcessorParameter(PARAM_IMAGE_VERSION,
                asset.getContainerResolver().getTag());

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addCopyInventoryProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("copy-inventories");

        File inputFile;
        try {
            URL fileUrl = new URL(asset.getUrlResolver().getUrl());
            inputFile = new File(fileUrl.getPath());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


        processor.setProcessorParameter(INPUT_BASE_DIR, inputFile.getParent());
        processor.setProcessorParameter(OUTPUT_INVENTORIES_DIR, workspace.getStageDirForAsset(asset, Stage.PREPARE).toString());
        processor.setProcessorParameter(PARAM_INVENTORIES_LIST, inputFile.getName());

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addScanDirectoryProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("scan-directory");

        if (assetPlan.isRequireFetch()) {
            processor.setProcessorParameter(INPUT_EXTRACT_DIR,
                    workspace.getStageDirForAsset(asset, Stage.FETCH).toString());
        } else if (assetPlan.isRequireContainerInspect()) {
            processor.setProcessorParameter(INPUT_EXTRACT_DIR,
                    workspace.getStageDirForAsset(asset, Stage.EXTRACT).toString());
        }

        processor.setProcessorParameter(OUTPUT_SCAN_DIR,
                workspace.getStageDirForAsset(asset, Stage.PREPARE).toString() + "scan/");
        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                workspace.getStageDirForAsset(asset, Stage.PREPARE).appendAssetInventory());

        String referenceDir = asset.getReferenceDir(environmentConfiguration.getWorkbenchDirNormalized());
        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, referenceDir);

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addInventoryToCycloneDxProcessor(AssetPlan assetPlan, Stage stage) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("inventory-to-cyclonedx");

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, workspace.getStageDirForAsset(asset, stage).appendAssetInventory());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, workspace.getStageDirForAsset(asset, stage).appendCycloneDxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME" );
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addInventoryToSpdxProcessor(AssetPlan assetPlan, Stage stage) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("inventory-to-spdx");

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, workspace.getStageDirForAsset(asset, stage).appendAssetInventory());
        processor.setProcessorParameter(OUTPUT_BOM_FILE, workspace.getStageDirForAsset(asset, stage).appendSpdxFile("JSON"));
        processor.setProcessorParameter(PARAM_DOCUMENT_OUTPUT_FORMAT, "JSON");
        processor.setProcessorParameter(PARAM_DOCUMENT_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION, "FIXME");
        processor.setProcessorParameter(PARAM_DOCUMENT_ORGANIZATION_URL, "FIXME");

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addPortfolioUploadProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("portfolio-upload");

        processor.setProcessorParameter(INPUT_FILE, workspace.getStageDirForAsset(asset, Stage.PREPARE).appendAssetInventory());
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_URL, environmentConfiguration.PORTFOLIO_MANAGER_URL);
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_TOKEN, environmentConfiguration.PORTFOLIO_MANAGER_TOKEN);
        processor.setProcessorParameter(PARAM_PROJECT_NAME, pipelineConfiguration.getPortfolioManager().getProject());
        processor.setProcessorParameter(PARAM_ASSET_GROUP_ID, pipelineConfiguration.getPortfolioManager().getAssetGroup());
        processor.setProcessorParameter(PARAM_ASSET_NAME, asset.getName());
        processor.setProcessorParameter(PARAM_ASSET_VERSION, asset.getVersion());
        processor.setProcessorParameter(PARAM_KEYSTORE_CONFIG_FILE, environmentConfiguration.getPortfolioManagerClientKeystoreFile());
        processor.setProcessorParameter(PARAM_TRUSTSTORE_CONFIG_FILE, environmentConfiguration.getPortfolioManagerClientTruststoreFile());
        processor.setProcessorParameter(PARAM_KEYSTORE_PASSWORD, environmentConfiguration.PORTFOLIO_MANAGER_CLIENT_KEYSTORE_PASSWORD);
        processor.setProcessorParameter(PARAM_TRUSTSTORE_PASSWORD, environmentConfiguration.PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_PASSWORD);

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addDashboardProcessors(AssetPlan assetPlan) {
        List<Dashboard> dashboards = pipelineConfiguration.getDashboards();
        Asset asset = assetPlan.getAsset();
        if (dashboards == null || dashboards.isEmpty()) {
            return;
        }

        for (Dashboard dashboard : dashboards) {
            if (!dashboard.getAssetId().equals(assetPlan.getAsset().getId())) {
                continue;
            }

            EnrichmentOptions enrichmentOptions = pipelineConfiguration.getOptions().getEnrichment();
            MavenProcessor processor = yamlProcessorCatalog.getProcessorById("create-dashboard");
            PipelineConfiguration.ProjectProperties.Project project = pipelineConfiguration
                    .getProjectProperties()
                    .getProject();

            processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                    workspace.getStageDirForAsset(asset, Stage.ADVISE).appendAssetInventory());
            processor.setProcessorParameter(OUTPUT_DASHBOARD_FILE,
                    workspace.getStageDirForAsset(asset, Stage.REPORT).appendDashboardFile());
            processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE,
                    enrichmentOptions.getSecurityPolicyFile(environmentConfiguration.getWorkbenchDirNormalized()));
            processor.setProcessorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS,
                    enrichmentOptions.getSecurityPolicyActiveIds() != null
                            ? String.join(",",
                            pipelineConfiguration.getOptions()
                                    .getEnrichment()
                                    .getSecurityPolicyActiveIds()) : null);
            processor.setProcessorParameter(PARAM_TENANT_ID,
                    project.getTenant());
            processor.setProcessorParameter(PARAM_ASSET_ID,
                    assetPlan.getAsset().getAssessmentId());
            processor.setProcessorParameter(PARAM_ASSESSMENT_CONTEXT,
                    assetPlan.getAsset().getContext());
            processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR,
                    environmentConfiguration.getMirrorDatabaseDir());

            assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
        }
    }


    private void addReportProcessors(AssetPlan assetPlan) {
        List<Report> reports = pipelineConfiguration.getReports();
        if (reports == null || reports.isEmpty()) {
            return;
        }

        for (Report report : reports) {
            if (!report.getAssetId().equals(assetPlan.getAsset().getId())) {
                continue;
            }

            List<String> types = report.getTypes();
            if (types == null || types.isEmpty()) {
                continue;
            }

            for (String type : types) {
                if (type == null) {
                    continue;
                }

                MavenProcessor processor = yamlProcessorCatalog.getProcessorById("create-document");
                ReportType reportType = ReportType.fromKey(type);
                Asset asset = assetPlan.getAsset();

                if (ReportType.requiresScan(reportType)) {
                    processor.setProcessorParameter(INPUT_INVENTORY_DIR, workspace.getStageDirForAsset(asset, Stage.SCAN).toString());
                } else if (ReportType.requiresVulnerabilityEnrichment(reportType)) {
                    processor.setProcessorParameter(INPUT_INVENTORY_DIR, workspace.getStageDirForAsset(asset, Stage.ADVISE).toString());
                } else if (assetPlan.isRequireResolve()){
                    processor.setProcessorParameter(INPUT_INVENTORY_DIR, workspace.getStageDirForAsset(asset, Stage.RESOLVE).toString());
                } else if (assetPlan.isRequireAggregation()){
                    processor.setProcessorParameter(INPUT_INVENTORY_DIR, workspace.getStageDirForAsset(asset, Stage.AGGREGATE).toString());
                } else {
                    processor.setProcessorParameter(INPUT_INVENTORY_DIR, workspace.getStageDirForAsset(asset, Stage.PREPARE).toString());
                }

                if (ReportType.fromKey(type).equals(ReportType.CERT_REPORT)) {
                    processor.setProcessorParameter(PARAM_OVERVIEW_ADVISORS, "[\"CERT_FR\"]");
                } else {
                    processor.setProcessorParameter(PARAM_OVERVIEW_ADVISORS,
                            report.getOverviewAdvisors() == null || report.getOverviewAdvisors().isEmpty()
                                    ? null
                                    : String.join(", ", report.getOverviewAdvisors()));
                }

                if (ReportType.requiresVulnerabilityEnrichment(reportType)) {
                    processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE,
                            pipelineConfiguration.getOptions().getEnrichment().getSecurityPolicyFile(environmentConfiguration.getWorkbenchDirNormalized()));
                }

                processor.setProcessorParameter(OUTPUT_DOCUMENT_FILE, workspace.getStageDirForAsset(asset, Stage.REPORT).appendReportFile(ReportType.fromKey(type)));

                processor.setProcessorParameter(PARAM_COMPUTED_INVENTORY_DIR,
                        workspace.getStageDirForAsset(asset, Stage.REPORT).toString());
                processor.setProcessorParameter(PARAM_DOCUMENT_TYPE, type);
                processor.setProcessorParameter(PARAM_DOCUMENT_LANGUAGE,
                        pipelineConfiguration.getOptions().getGlobal().getDocumentLanguage());

                processor.setProcessorParameter(PARAM_ASSET_ID, assetPlan.getAsset().getId());
                processor.setProcessorParameter(PARAM_ASSET_NAME, assetPlan.getAsset().getName());
                processor.setProcessorParameter(PARAM_ASSET_VERSION, assetPlan.getAsset().getVersion());

                processor.setProcessorParameter(PARAM_PRODUCT_NAME,
                        pipelineConfiguration.getProjectProperties().getProject().getName());
                processor.setProcessorParameter(PARAM_PRODUCT_VERSION,
                        pipelineConfiguration.getProjectProperties().getProject().getVersion());
                processor.setProcessorParameter(PARAM_PRODUCT_WATERMARK, report.getWatermark());
                processor.setProcessorParameter(PARAM_PROPERTY_SELECTOR_ORGANIZATION, report.getOrganization());
                processor.setProcessorParameter(PARAM_PROPERTY_SELECTOR_CLASSIFICATION, report.getClassificationRating());
                processor.setProcessorParameter(PARAM_PROPERTY_SELECTOR_CONTROL, report.getControlRating());
                processor.setProcessorParameter(PARAM_ASSET_DESCRIPTOR_FILE, KontinuumUtils.normalizeDir(environmentConfiguration.getDescriptorsDirNormalized(), reportType.getAssetDescriptorFile()));
                processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR,
                        assetPlan.getAsset().getReferenceDir(environmentConfiguration.getWorkbenchDirNormalized()));
                processor.setProcessorParameter(PARAM_REFERENCE_LICENSE_DIR, null);
                processor.setProcessorParameter(PARAM_REFERENCE_COMPONENT_DIR, null);
                processor.setProcessorParameter(ENV_KONTINUUM_DIR,
                        environmentConfiguration.getKontinuumDirNormalized());
                processor.setProcessorParameter(ENV_KONTINUUM_PROCESSORS_DIR,
                        environmentConfiguration.getKontinuumProcessorsDirNormalized());
                processor.setProcessorParameter(ENV_WORKBENCH_DIR,
                        environmentConfiguration.getWorkbenchDirNormalized());
                processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR,
                        environmentConfiguration.getMirrorDatabaseDir());

                assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
            }
        }
    }

    private void addPortfolioDownloadProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("portfolio-download");

        processor.setProcessorParameter(OUTPUT_INVENTORY_DIR, workspace.getStageDirForAsset(asset, Stage.AGGREGATE).toString());
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_URL, environmentConfiguration.PORTFOLIO_MANAGER_URL);
        processor.setProcessorParameter(PARAM_PORTFOLIO_MANAGER_TOKEN, environmentConfiguration.PORTFOLIO_MANAGER_TOKEN);
        processor.setProcessorParameter(PARAM_PROJECT_NAME, pipelineConfiguration.getPortfolioManager().getProject());
        processor.setProcessorParameter(PARAM_ASSET_GROUP_ID, "Reports:SNAPSHOT");
        processor.setProcessorParameter(PARAM_ASSET_ID, pipelineConfiguration.getPortfolioManager().getProject());
        processor.setProcessorParameter(PARAM_KEYSTORE_CONFIG_FILE, environmentConfiguration.getPortfolioManagerClientKeystoreFile());
        processor.setProcessorParameter(PARAM_TRUSTSTORE_CONFIG_FILE, environmentConfiguration.getPortfolioManagerClientTruststoreFile());
        processor.setProcessorParameter(PARAM_KEYSTORE_PASSWORD, environmentConfiguration.PORTFOLIO_MANAGER_CLIENT_KEYSTORE_PASSWORD);
        processor.setProcessorParameter(PARAM_TRUSTSTORE_PASSWORD, environmentConfiguration.PORTFOLIO_MANAGER_CLIENT_TRUSTSTORE_PASSWORD);
        processor.setProcessorParameter(PARAM_INVENTORY_MODIFIER, "report");

        String targetInventoryPath = workspace.getStageDirForAsset(asset, Stage.AGGREGATE).appendAssetInventory();

        StringBuilder postScript = new StringBuilder();
        postScript.append("find ").append(workspace.getStageDirForAsset(asset, Stage.AGGREGATE).toString()).append(" -type f -name \"*.zip\" -print0 | while IFS= read -r -d '' zip_file; do").append(System.lineSeparator());
        postScript.append("    zip_dir=$(dirname \"$zip_file\")").append(System.lineSeparator());
        postScript.append("    unzip -q -j \"$zip_file\" \"*_report.xlsx\" \"*_report.xls\" -d \"$zip_dir\" || true").append(System.lineSeparator());
        postScript.append("    extracted_file=$(find \"$zip_dir\" -maxdepth 1 -type f \\( -name \"*_report.xlsx\" -o -name \"*_report.xls\" \\) | head -n 1)").append(System.lineSeparator());
        postScript.append("    if [ -n \"$extracted_file\" ]; then").append(System.lineSeparator());
        postScript.append("        mv \"$extracted_file\" \"").append(targetInventoryPath).append("\"").append(System.lineSeparator());
        postScript.append("    fi").append(System.lineSeparator());
        postScript.append("done").append(System.lineSeparator());

        processor.setPostScript(postScript.toString());

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }


    private void addEnrichInventoryWithReferenceProcessor(AssetPlan assetPlan, Stage stage, String inputInventory, String referenceInventoryDir) {
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("enrich-inventory-with-reference");
        processor.setStage(stage.name());

        processor.setProcessorParameter(INPUT_INVENTORY_FILE, inputInventory);
        processor.setProcessorParameter(PARAM_REFERENCE_INVENTORY_DIR, referenceInventoryDir);

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);

    }

    private void addResolveProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("resolve-inventory");

        if (assetPlan.isRequireAggregation()) {
            processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                    workspace.getStageDirForAsset(asset, Stage.AGGREGATE).appendAssetInventory());
        } else {
            processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                    workspace.getStageDirForAsset(asset, Stage.PREPARE).appendAssetInventory());
        }

        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                workspace.getStageDirForAsset(asset, Stage.RESOLVE).appendAssetInventory());
        processor.setProcessorParameter(PARAM_ARTIFACT_RESOLVER_CONFIG_FILE,
                environmentConfiguration.ARTIFACT_RESOLVER_CONFIG_FILE);
        processor.setProcessorParameter(PARAM_ARTIFACT_RESOLVER_PROXY_FILE,
                environmentConfiguration.ARTIFACT_RESOLVER_PROXY_FILE);
        processor.setProcessorParameter(ENV_MAVEN_INDEX_DIR, workspace.MAVEN_INDEX_DIR);

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addScanProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("scan-inventory");

        if (assetPlan.isRequireResolve()) {
            processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                    workspace.getStageDirForAsset(asset, Stage.RESOLVE).appendAssetInventory());
        } else if (assetPlan.isRequireAggregation()) {
            processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                    workspace.getStageDirForAsset(asset, Stage.AGGREGATE).appendAssetInventory());
        } else {
            processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                    workspace.getStageDirForAsset(asset, Stage.PREPARE).appendAssetInventory());
        }

        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                workspace.getStageDirForAsset(asset, Stage.SCAN).appendAssetInventory());
        processor.setProcessorParameter(INPUT_OUTPUT_ANALYSIS_BASE_DIR,
                workspace.getStageDirForAsset(asset, Stage.SCAN) + "analysis/");
        processor.setProcessorParameter(PARAM_PROPERTIES_FILE,
                environmentConfiguration.SCAN_PROPERTIES_FILE);
        processor.setProcessorParameter(ENV_KOSMOS_PASSWORD,
                environmentConfiguration.KOSMOS_PASSWORD);
        processor.setProcessorParameter(ENV_KOSMOS_USERKEYS_FILE,
                environmentConfiguration.KOSMOS_USERKEYS_FILE);

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }

    private void addVulnerabilityEnrichmentProcessor(AssetPlan assetPlan) {
        Asset asset = assetPlan.getAsset();
        MavenProcessor processor = yamlProcessorCatalog.getProcessorById("enrich-inventory");

        if (assetPlan.isRequireResolve()) {
            processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                    workspace.getStageDirForAsset(asset, Stage.RESOLVE).appendAssetInventory());
        } else if (assetPlan.isRequireAggregation()) {
            processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                    workspace.getStageDirForAsset(asset, Stage.AGGREGATE).appendAssetInventory());
        } else {
            processor.setProcessorParameter(INPUT_INVENTORY_FILE,
                    workspace.getStageDirForAsset(asset, Stage.PREPARE).appendAssetInventory());
        }

        processor.setProcessorParameter(OUTPUT_INVENTORY_FILE,
                workspace.getStageDirForAsset(asset, Stage.ADVISE).appendAssetInventory());
        processor.setProcessorParameter(OUTPUT_TMP_DIR, workspace.getStageDirForAsset(asset, Stage.ADVISE) + "tmp/");
        processor.setProcessorParameter(PARAM_CORRELATION_DIR,
                environmentConfiguration.getCorrelationDir());

        PipelineConfiguration.Options.EnrichmentOptions enrichment = pipelineConfiguration.getOptions()
                .getEnrichment();

        processor.setProcessorParameter(PARAM_SECURITY_POLICY_FILE, enrichment.getSecurityPolicyFile(environmentConfiguration.getWorkbenchDirNormalized()));
        processor.setProcessorParameter(PARAM_SECURITY_POLICY_ACTIVE_IDS,
                enrichment.getSecurityPolicyActiveIds() != null
                        ? String.join(",", enrichment.getSecurityPolicyActiveIds())
                        : null);

        processor.setProcessorParameter(PARAM_ACTIVATE_MSRC, String.valueOf(enrichment.getActivateMsrc()));
        processor.setProcessorParameter(PARAM_ACTIVATE_NVD, String.valueOf(enrichment.getActivateNvd()));
        processor.setProcessorParameter(PARAM_ACTIVATE_CERTFR,
                String.valueOf(enrichment.getActivateCertFr()));
        processor.setProcessorParameter(PARAM_ACTIVATE_CERTEU,
                String.valueOf(enrichment.getActivateCertEu()));
        processor.setProcessorParameter(PARAM_ACTIVATE_CERTSEI,
                String.valueOf(enrichment.getActivateCertSei()));
        processor.setProcessorParameter(PARAM_ACTIVATE_KEV, String.valueOf(enrichment.getActivateKev()));
        processor.setProcessorParameter(PARAM_ACTIVATE_EPSS, String.valueOf(enrichment.getActivateEpss()));
        processor.setProcessorParameter(PARAM_ACTIVATE_EOL, String.valueOf(enrichment.getActivateEol()));
        processor.setProcessorParameter(PARAM_ACTIVATE_OSV, String.valueOf(enrichment.getActivateOsv()));
        processor.setProcessorParameter(PARAM_ACTIVATE_CSAF, String.valueOf(enrichment.getActivateCsaf()));
        PipelineConfiguration.ProjectProperties.Project project = pipelineConfiguration.getProjectProperties()
                .getProject();
        processor.setProcessorParameter(PARAM_ASSESSMENT_DIRS, asset.getAssessmentDir(project, environmentConfiguration.getWorkbenchDirNormalized()));
        processor.setProcessorParameter(PARAM_CONTEXT_DIRS, asset.getContextDir(project, environmentConfiguration.getWorkbenchDirNormalized()));

        processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR,
                environmentConfiguration.getMirrorDatabaseDir());

        assetProcessorsMap.computeIfAbsent(assetPlan.getAsset(), k -> new ArrayList<>()).add(processor);
    }
}
