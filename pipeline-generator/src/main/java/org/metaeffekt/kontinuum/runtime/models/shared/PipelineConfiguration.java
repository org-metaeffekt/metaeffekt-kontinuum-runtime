package org.metaeffekt.kontinuum.runtime.models.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Jackson-mapped model of a Kontinuum pipeline YAML configuration, covering project
 * properties, reports, dashboards, overviews, portfolio manager settings, and options.
 */
@Data
public class PipelineConfiguration {

    private ProjectProperties projectProperties;
    private List<Report> reports;
    private List<Dashboard> dashboards;
    private List<Overview> overviews;
    private PortfolioManager portfolioManager;
    private Options options;

    /**
     * Whether vulnerability enrichment is required by any dashboard or report type.
     *
     * @return true if the pipeline requires vulnerability enrichment
     */
    public boolean requiresVulnerabilityEnrichment() {
        boolean hasDashboard = dashboards != null && dashboards.stream()
                .filter(Objects::nonNull)
                .anyMatch(d -> d.getAssetIds() != null &&
                               !d.getAssetIds().isEmpty());
        if (hasDashboard) {
            return true;
        }
        if (reports == null) {
            return false;
        }
        return reports.stream()
                .map(Report::getTypes)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(ReportType::fromKey)
                .anyMatch(ReportType::requiresVulnerabilityEnrichment);
    }

    /**
     * Whether vulnerability enrichment is required for the given asset.
     *
     * @param asset the asset to check
     * @return true if the pipeline requires vulnerability enrichment for the asset
     */
    public boolean requiresVulnerabilityEnrichment(ProjectProperties.Asset asset) {
        if (asset == null) {
            return false;
        }
        return requiresVulnerabilityEnrichment(asset.getId());
    }

    /**
     * Whether vulnerability enrichment is required for the given asset id.
     *
     * @param assetId the asset identifier to check
     * @return true if the pipeline requires vulnerability enrichment for the asset id
     */
    public boolean requiresVulnerabilityEnrichment(String assetId) {
        if (assetId == null) {
            return false;
        }
        boolean hasDashboard = dashboards != null && dashboards.stream()
                .filter(Objects::nonNull)
                .filter(d -> d.getAssetIds() != null)
                .anyMatch(d -> d.getAssetIds().contains(assetId));
        if (hasDashboard) {
            return true;
        }
        if (reports == null) {
            return false;
        }
        return reports.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getAssetIds() != null && r.getAssetIds().contains(assetId))
                .map(Report::getTypes)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(ReportType::fromKey)
                .anyMatch(ReportType::requiresVulnerabilityEnrichment);
    }

    /**
     * The {@code projectProperties} section: the project descriptor and its asset tree.
     */
    @Data
    public static class ProjectProperties {

        private Project project;
        private List<Asset> assets;

        /**
         * Returns all assets in the project, flattened depth-first across the asset tree.
         *
         * @return the flattened list of assets, or an empty list if none are configured
         */
        public List<Asset> getAllAssets() {
            if (this.assets == null || this.assets.isEmpty()) {
                return Collections.emptyList();
            }
            return this.assets.stream()
                    .flatMap(Asset::flattenStream)
                    .collect(Collectors.toList());
        }

        /**
         * Finds the root asset in the configured asset tree that contains the target asset.
         *
         * @param targetAsset the asset to locate within the asset tree
         * @return the containing root asset, or the target asset if it is not nested
         */
        public Asset getRootAssetFor(Asset targetAsset) {
            if (this.assets == null || this.assets.isEmpty() || targetAsset == null) {
                return targetAsset;
            }
            for (Asset root : this.assets) {
                if (containsAsset(root, targetAsset)) {
                    return root;
                }
            }
            return targetAsset;
        }

        private boolean containsAsset(Asset current, Asset target) {
            if (current == target || (current.getId() != null && current.getId().equals(target.getId()))) {
                return true;
            }
            if (current.getAssets() != null) {
                for (Asset child : current.getAssets()) {
                    if (containsAsset(child, target)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * The {@code project} descriptor identifying a project by id, name, version, and tenant.
         */
        @Data
        public static class Project {
            private String id;
            private String name;
            private String version;
            private String tenant;

            @Override
            public String toString() {
                return id;
            }
        }

        /**
         * A single asset entry, optionally nesting further assets and resolver configuration.
         */
        @Data
        public static class Asset {
            private String id;
            private String name;
            private String version;
            private String build;

            private List<Asset> assets;

            private String assessmentId;
            private String reference;
            private String context;

            private UrlResolver urlResolver;
            private MavenResolver mavenResolver;
            private ContainerResolver containerResolver;

            /**
             * Resolver configuration for fetching an asset from a URL, including optional
             * credentials and a custom header.
             */
            @Data
            public static class UrlResolver {
                private String url;
                private String urlPattern;
                private String username;
                private String password;
                private String token;
                private String headerName;
                private String headerValue;
            }

            /**
             * Resolver configuration for fetching an asset as a Maven artifact.
             */
            @Data
            public static class MavenResolver {
                private String groupId;
                private String artifactId;
                private String artifactVersion;
                private String repoUrl = "https://repo1.maven.org/maven2";
            }

            /**
             * Resolver configuration for fetching an asset as a container image.
             */
            @Data
            public static class ContainerResolver {
                private String image;
                private String tag;
                private String repoUrl = "docker.io";
            }

            /**
             * Resolves the reference directory for this asset relative to the workbench path.
             *
             * @param workbenchPath the workbench root path used to normalize the reference directory
             * @return the normalized reference directory
             */
            public String getReferenceDir(String workbenchPath) throws IllegalStateException {
                if (StringUtils.isBlank(reference)) {
                    throw new IllegalStateException(
                            "Tried to access reference inventory for asset " + this + " but is not set.");
                }

                if (Files.isDirectory(Path.of(reference))) {
                    return KontinuumUtils.normalizeDir(workbenchPath, reference);
                } else {
                    File referenceFile = new File(reference);
                    return KontinuumUtils.normalizeDir(workbenchPath, referenceFile.getParentFile().getPath());
                }
            }

            /**
             * Resolves the assessment context directory for this asset.
             *
             * @param project       the project providing the tenant
             * @param workbenchPath the workbench root path used to normalize the context directory
             * @return the normalized context directory
             */
            public String getContextDir(ProjectProperties.Project project, String workbenchPath) {
                if (StringUtils.isBlank(project.getName())) {
                    throw new IllegalStateException(
                            "Tried to access tenant for project " + project + " but is not set.");
                }

                if (StringUtils.isBlank(assessmentId)) {
                    throw new IllegalStateException(
                            "Tried to access assessment id for asset " + this + " but is not set.");
                }

                if (StringUtils.isBlank(context)) {
                    throw new IllegalStateException("Tried to access context for asset " + this + " but is not set.");
                }

                return KontinuumUtils.normalizeDir(workbenchPath, "assessments", project.getTenant(), assessmentId,
                                                   context, "context");
            }

            /**
             * Resolves the assessment directory for this asset.
             *
             * @param project       the project providing the tenant
             * @param workbenchPath the workbench root path used to normalize the assessment directory
             * @return the normalized assessment directory
             */
            public String getAssessmentDir(ProjectProperties.Project project, String workbenchPath) {
                if (StringUtils.isBlank(project.getName())) {
                    throw new IllegalStateException(
                            "Tried to access tenant for project " + project + " but is not set.");
                }

                if (StringUtils.isBlank(assessmentId)) {
                    throw new IllegalStateException(
                            "Tried to access assessment id for asset " + this + " but is not set.");
                }

                return KontinuumUtils.normalizeDir(workbenchPath, "assessments", project.getTenant(), assessmentId);
            }

            /**
             * Whether this asset is fetched from a local file URL that already points to an extracted
             * inventory (an {@code .xls} or {@code .xlsx} file). Such assets do not require the
             * extract stage, since the fetched file already is an inventory.
             *
             * @return true if the url resolver references a local {@code .xls}/{@code .xlsx} file
             */
            public boolean isPreExtractedInventory() {
                if (urlResolver == null || StringUtils.isBlank(urlResolver.getUrl())) {
                    return false;
                }
                String url = urlResolver.getUrl();
                if (!StringUtils.startsWithIgnoreCase(url, "file:")) {
                    return false;
                }
                String lowerCaseUrl = url.toLowerCase(Locale.ROOT);
                return lowerCaseUrl.endsWith(".xls") || lowerCaseUrl.endsWith(".xlsx");
            }

            /**
             * Derives the file name the url resolver asset is downloaded as, mirroring the naming
             * convention of the Ant {@code <get>} task used by the fetch processor.
             *
             * @return the file name of the resolved url or null if no url is configured
             */
            public String getUrlResolverFileName() {
                if (urlResolver == null || StringUtils.isBlank(urlResolver.getUrl())) {
                    return null;
                }
                String path = urlResolver.getUrl();
                try {
                    path = new URL(path).getPath();
                } catch (MalformedURLException e) {
                    // fall back to the raw url, which is still parsed below
                }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                int slash = path.lastIndexOf('/');
                if (slash > -1) {
                    path = path.substring(slash + 1);
                }
                return path;
            }

            /**
             * Streams this asset followed by all of its nested assets depth-first.
             *
             * @return a stream of this asset and its descendants
             */
            public Stream<Asset> flattenStream() {
                Stream<Asset> children = (this.assets == null)
                        ? Stream.empty()
                        : this.assets.stream().flatMap(Asset::flattenStream);

                return Stream.concat(Stream.of(this), children);
            }

            @Override
            public String toString() {
                return id;
            }
        }
    }

    /**
     * The {@code reports} section: report entries selecting assets and report types to generate.
     */
    @Data
    public static class Report {
        private String id;
        private List<String> assetIds;
        private List<String> types;
        private List<String> overviewAdvisors;
        private String productName;
        private String productVersion;
        private String productWatermark;
        private String organization;
        private String classificationRating;
        private String controlRating;
        private List<SupportedLocale> locales;
        private String preReportFilterFile;

        /**
         * Short, position-derived group name ({@code group-1}, {@code group-2}, ...) resolved by
         * {@link org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader} for
         * report entries that do not declare an explicit {@link #id}. Kept out of the YAML schema.
         */
        @JsonIgnore
        private String resolvedGroupId;

        /**
         * The name identifying this report entry's output folder and report files. Prefers the
         * configured {@link #id}; otherwise the loader-assigned {@link #resolvedGroupId}. Falls back
         * to joined asset ids only when the entry was never processed by the loader.
         *
         * @return the resolved group id used for the output folder and report files
         */
        public String getGroupId() {
            if (StringUtils.isNotBlank(id)) {
                return id;
            }
            if (StringUtils.isNotBlank(resolvedGroupId)) {
                return resolvedGroupId;
            }
            if (assetIds != null && !assetIds.isEmpty()) {
                return String.join("-", assetIds);
            }
            return "default";
        }
    }

    /**
     * The {@code dashboards} section: dashboard entries selecting the assets to display.
     */
    @Data
    public static class Dashboard {
        private List<String> assetIds;
    }

    /**
     * The {@code overviews} section: overview entries selecting the assets to summarize.
     */
    @Data
    public static class Overview {
        private List<String> assetIds;
    }

    /**
     * The {@code portfolioManager} section: the portfolio manager project to publish to.
     */
    @Data
    public static class PortfolioManager {
        private String project;
    }

    /**
     * The {@code options} section grouping global execution and enrichment options.
     */
    @Data
    public static class Options {

        private GlobalOptions global = new GlobalOptions();
        private EnrichmentOptions enrichment = new EnrichmentOptions();

        /**
         * Global execution toggles for resolve, scan, and SBOM generation.
         */
        @Data
        public static class GlobalOptions {
            private Boolean enableResolve = false;
            private Boolean enableScan = false;
            private Boolean enableSpdxBom = false;
            private Boolean enableCycloneDxBom = false;
            private String debugParam;
        }

        /**
         * Enrichment toggles and security policy configuration for advisory data sources.
         */
        @Data
        public static class EnrichmentOptions {

            private String securityPolicyFile;
            private List<String> securityPolicyActiveIds = new ArrayList<>();
            private Boolean activateMsrc = true;
            private Boolean activateNvd = true;
            private Boolean activateCertFr = true;
            private Boolean activateCertEu = true;
            private Boolean activateCertSei = true;
            private Boolean activateOsv = true;
            private Boolean activateKev = true;
            private Boolean activateEpss = true;
            private Boolean activateEol = true;
            private Boolean activateCsaf = true;


            /**
             * Resolves the configured security policy file relative to the workbench path.
             *
             * @param workbenchPath the workbench root path used to normalize the policy file path
             * @return the normalized security policy file path
             */
            public String getSecurityPolicyFile(String workbenchPath) throws IllegalStateException {
                if (StringUtils.isBlank(securityPolicyFile)) {
                    throw new IllegalStateException(
                            "Tried to access reference inventory for asset " + this + " but is not set.");
                }

                File file = new File(securityPolicyFile);
                return KontinuumUtils.normalizeDir(workbenchPath, file.getPath());
            }
        }
    }
}
