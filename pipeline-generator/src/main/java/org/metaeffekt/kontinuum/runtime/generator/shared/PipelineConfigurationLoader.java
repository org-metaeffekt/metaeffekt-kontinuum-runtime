package org.metaeffekt.kontinuum.runtime.generator.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.ReportType;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Dashboard;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.Report;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Project;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Slf4j
public class PipelineConfigurationLoader {

    private static final Set<ReportType> ASSESSMENT_REPORT_TYPES = Set.of(
            ReportType.CERT_REPORT, ReportType.VULNERABILITY_REPORT, ReportType.VULNERABILITY_SUMMARY_REPORT);

    private boolean isValid = true;
    private PipelineConfiguration pipelineConfiguration;

    public PipelineConfiguration readConfig(File pipelineConfigFile) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            validatePipelineConfigFile(objectMapper.readValue(pipelineConfigFile, PipelineConfiguration.class));
            return pipelineConfiguration;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read pipeline configuration file.", e);
        }
    }
    

    public void validatePipelineConfigFile(PipelineConfiguration pipelineConfiguration) {
        this.pipelineConfiguration = pipelineConfiguration;

        if (pipelineConfiguration == null) {
            log.error("Pipeline configuration is missing.");
            isValid = false;
            return;
        }

        if (pipelineConfiguration.getProjectProperties() == null) {
            log.error("Project properties are missing.");
            isValid = false;
            return;
        }

        validateProject();
        validateAssets();
        validateReports();
        validateDashboards();
        validatePortfolioManager();
        validateOptions();

        if (!isValid) {
            throw new IllegalStateException("Pipeline configuration contains errors.");
        }
    }

    private void validateProject() {
        Project project = pipelineConfiguration.getProjectProperties().getProject();
        if (project == null) {
            log.error("Project is missing.");
            isValid = false;
        }

        if (StringUtils.isBlank(project.getId())) {
            log.error("Project id is missing.");
            isValid = false;
        }

        if (StringUtils.isBlank(project.getTenant()) && assessmentFieldsRequired()) {
            log.error("Project tenant is empty but required for reports or dashboards.");
            isValid = false;
        }
    }

    private void validateAssets() {
        List<Asset> topLevelAssets = pipelineConfiguration.getProjectProperties().getAssets();
        if (topLevelAssets == null || topLevelAssets.isEmpty()) {
            log.error("Assets are missing.");
            isValid = false;
        }

        List<Asset> allAssets = getAllAssets();

        for (Asset asset : allAssets) {
            if (asset == null) {
                log.error("Asset entry is null.");
                isValid = false;
                continue;
            }
            if (StringUtils.isBlank(asset.getId())) {
                log.error("Asset {} requires an id to be set.", asset);
                isValid = false;
            }

            if (StringUtils.isBlank(asset.getAssessmentId()) && assessmentFieldsRequiredForAsset(asset)) {
                log.error("Asset {} requires 'assessmentId' to be set because either reports or dashboards require it.", asset);
                isValid = false;
            }

            if (StringUtils.isBlank(asset.getContext()) && assessmentFieldsRequiredForAsset(asset)) {
                log.error("Asset {} requires 'context' to be set because either reports or dashboards require it.", asset);
                isValid = false;
            }

            if (StringUtils.isBlank(asset.getReference())) {
                log.error("Asset {} requires 'reference' to be set.", asset);
                isValid = false;
            }
            
            if (asset.getMavenResolver() == null && asset.getUrlResolver() == null && asset.getContainerResolver() == null) {
                log.error("Asset {} requires a resolver to bet set.", asset);
                isValid = false;
            }

            validateMavenResolver(asset);
            validateUrlResolver(asset);
            validateContainerResolver(asset);
        }
    }

    private void validateMavenResolver(Asset asset) {

        Asset.MavenResolver mavenResolver = asset.getMavenResolver();
        if (mavenResolver == null) {
            return;
        }

        if (StringUtils.isBlank(mavenResolver.getGroupId())) {
            log.error("Asset {} requires 'mavenResolver.groupId' to contain a group id.", asset);
            isValid = false;
        }

        if (StringUtils.isBlank(mavenResolver.getArtifactVersion())) {
            log.error("Asset {} requires 'mavenResolver.artifactVersion' to contain a version.", asset);
            isValid = false;
        }
    }
    
    private void validateUrlResolver(Asset asset) {
        Asset.UrlResolver urlResolver = asset.getUrlResolver();
        if (urlResolver == null) {
            return;
        }

        if (StringUtils.isNotBlank(urlResolver.getUrl())) {
            try {
                new URL(urlResolver.getUrl());
            } catch (MalformedURLException e) {
                isValid = false;
                log.error("Asset {} requires 'urlResolver.url' to contain a valid URL.", asset);
            }
        } else if (StringUtils.isNotBlank(urlResolver.getUrlPattern())) {
            String urlPattern = urlResolver.getUrlPattern();
            if ((urlPattern.contains("${name}") && StringUtils.isBlank(asset.getName()))
                    || (urlPattern.contains("${version}") && StringUtils.isBlank(asset.getVersion()))) {
                isValid = false;
                log.error("Asset {} requires 'urlResolver.urlPattern' to only contain placeholders which are set.", asset);
            } else {
                if (urlPattern.contains("${name}")) {
                    urlPattern = urlPattern.replace("${name}", asset.getName());
                }
                if (urlPattern.contains("${version}")) {
                    urlPattern = urlPattern.replace("${version}", asset.getVersion());
                }
                urlResolver.setUrl(urlPattern);
            }
        } else {
            log.error("Asset {} requires 'urlResolver' to contain either a valid 'url' or 'urlPattern'.", asset);
            isValid = false;
        }
    }

    private void validateContainerResolver(Asset asset) {
        Asset.ContainerResolver containerResolver = asset.getContainerResolver();
        if (containerResolver == null) {
            return;
        }

        if (StringUtils.isBlank(containerResolver.getImage())) {
            log.error("Asset {} requires 'containerResolver.image' to be set.", asset);
            isValid = false;
        }

        if (StringUtils.isBlank(containerResolver.getTag())) {
            log.error("Asset {} requires 'containerResolver.tag' to be set.", asset);
            isValid = false;
        }
    }

    private void validateReports() {
        List<String> assetIds = getAllAssets()
            .stream()
            .map(Asset::getId)
            .toList();

        List<Report> reports = getReports();

        if (reports == null || reports.isEmpty()) {
            return;
        }

        for (PipelineConfiguration.Report report : getReports()) {
            if (report.getAssetIds().isEmpty()) {
                log.error("A report is missing 'assetIds'.");
                isValid = false;
                continue;
            }

            if (!new HashSet<>(assetIds).containsAll(report.getAssetIds())) {
                log.error("A report contains an invalid 'assetIds'.");
                isValid = false;
            }

            if (report.getTypes() == null || report.getTypes().isEmpty()) {
                log.error("A report with 'assetIds': {} contains an empty 'types' list.", report.getAssetIds());
                isValid = false;
                continue;
            }

            for (String type : report.getTypes()) {
                if (!ReportType.allKeys().contains(type)) {
                    log.error("A report with 'assetIds': {} contains an invalid type in 'types': {}.", report.getAssetIds(), type);
                    isValid = false;
                }
            }
        }
    }

    private void validateDashboards() {
        List<String> assetIds = getAllAssets()
            .stream()
            .map(Asset::getId)
            .toList();

        List<Dashboard> dashboards = getDashboards();

        if (dashboards == null || dashboards.isEmpty()) {
            return;
        }

        for (PipelineConfiguration.Dashboard dashboard : dashboards) {
            if (dashboard.getAssetIds().isEmpty()) {
                log.error("A dashboard is missing 'assetIds'.");
                isValid = false;
                continue;
            }

            if (!new HashSet<>(assetIds).containsAll(dashboard.getAssetIds())) {
                log.error("A dashboard contains invalid 'assetIds'.");
                isValid = false;
            }
        }
    }

    private void validatePortfolioManager() {
        PipelineConfiguration.PortfolioManager portfolioManager = pipelineConfiguration.getPortfolioManager();

        if (portfolioManager == null) {
            return;
        }

        if (StringUtils.isBlank(portfolioManager.getProject())) {
            log.error("Portfolio Manager entry exists, but 'project' is not defined.");
            isValid = false;
        }

        if (StringUtils.isBlank(portfolioManager.getAssetGroup())) {
            log.error("Portfolio Manager entry exists, but 'assetGroup' is not defined.");
            isValid = false;
        }
    }

    private void validateOptions() {
        if (pipelineConfiguration.getOptions() == null) {
            if (assessmentFieldsRequired()) {
                log.error("Pipeline configuration requires 'options' and 'enrichment' to be set because either reports or dashboards require it.");
                isValid = false;
                return;
            }
            return;
        }

        validateGlobalOptions();
        validateEnrichmentOptions();
    }

    private void validateGlobalOptions() {
        // Nothing to validate for now
        return;
    }

    private void validateEnrichmentOptions() {
        assert pipelineConfiguration.getOptions() != null;
        PipelineConfiguration.Options.EnrichmentOptions enrichmentOptions = pipelineConfiguration.getOptions().getEnrichment();
        if (enrichmentOptions == null) {
            return;
        }

        if (StringUtils.isBlank(enrichmentOptions.getSecurityPolicyFile()) && assessmentFieldsRequired()) {
            log.error("Enrichment Options requires 'securityPolicyFile' to be set because either reports or dashboards require it.");
            isValid = false;
        }
    }

    private boolean assessmentFieldsRequired() {
        for (Asset asset : getAllAssets()) {
            if (assessmentFieldsRequiredForAsset(asset)) {
                 return true;
            }
        }
        return false;
    }


    private boolean assessmentFieldsRequiredForAsset(Asset asset) {
        List<Report> reportsRequiringAssessmentFields;
        List<Dashboard> dashboardsRequiringAssessmentFields = new ArrayList<>();

        if (asset == null) {
            reportsRequiringAssessmentFields = getReports()
                .stream()
                .filter(r -> hasAssessmentType(r, null))
                .toList();
            dashboardsRequiringAssessmentFields.addAll(getDashboards());
        } else {
            reportsRequiringAssessmentFields = getReports()
                .stream()
                .filter(r -> r.getAssetIds().contains(asset.getId()))
                .filter(r -> hasAssessmentType(r, asset.getId()))
                .toList();
            dashboardsRequiringAssessmentFields.addAll(getDashboards()
                .stream()
                .filter(d -> d.getAssetIds().contains(asset.getId()))
                .toList());
        }
        
        return !reportsRequiringAssessmentFields.isEmpty() || !dashboardsRequiringAssessmentFields.isEmpty();
    }

    private boolean hasAssessmentType(PipelineConfiguration.Report report, String assetId) {
        if (report.getTypes() == null) {
            return false;
        }
        for (String type : report.getTypes()) {
            if (type == null || !ReportType.allKeys().contains(type)) {
                continue;
            }
            if (ASSESSMENT_REPORT_TYPES.contains(ReportType.fromKey(type))) {
                if (assetId == null || report.getAssetIds().contains(assetId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Asset> getAllAssets() {
        assert pipelineConfiguration != null && pipelineConfiguration.getProjectProperties() != null;
        return pipelineConfiguration.getProjectProperties().getAllAssets();

    }

    private List<Report> getReports() {
        if (pipelineConfiguration.getReports() == null) {
            return Collections.emptyList();
        }
        return pipelineConfiguration.getReports();
    }

    private List<Dashboard> getDashboards() {
        if (pipelineConfiguration.getDashboards() == null) {
            return Collections.emptyList();
        }
        return pipelineConfiguration.getDashboards();
    }

}
