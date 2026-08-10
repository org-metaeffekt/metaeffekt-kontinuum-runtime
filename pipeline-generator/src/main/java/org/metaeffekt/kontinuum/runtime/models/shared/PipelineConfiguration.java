package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.util.KontinuumUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
public class PipelineConfiguration {

    private ProjectProperties projectProperties;
    private List<Report> reports;
    private List<Dashboard> dashboards;
    private PortfolioManager portfolioManager;
    private Options options;

    @Data
    public static class ProjectProperties {

        private Project project;
        private List<Asset> assets;

        @Data
        public static class Project {
            private String name;
            private String version;
            private String tenant;

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                if (StringUtils.isNotBlank(getName())) {
                    sb.append(getName());
                } else {
                    sb.append("unnamed-project");
                }

                if (StringUtils.isNotBlank(getVersion())) {
                    sb.append("-").append(getVersion());
                }
                return sb.toString();
            }
        }

        @Data
        public static class Asset {
            private String id;
            private String name;
            private String version;

            private String assessmentId;
            private String reference;
            private String context;

            private UrlResolver urlResolver;
            private MavenResolver mavenResolver;
            private ContainerResolver containerResolver;

            @Data
            public static class UrlResolver {
                private String url;
                private String urlPattern;
            }

            @Data
            public static class MavenResolver {
                private String groupId;
                private String artifactId;
                private String artifactVersion;
                private String repoUrl = "https://repo1.maven.org/maven2";
            }

            @Data
            public static class ContainerResolver {
                private String image;
                private String tag;
            }

            public String getReferenceDir(String workbenchPath) throws IllegalStateException{
                if (StringUtils.isBlank(reference)) {
                    throw new IllegalStateException("Tried to access reference inventory for asset " + this + " but is not set.");
                }

                if (Files.isDirectory(Path.of(reference))) {
                    return KontinuumUtils.normalizeDir(workbenchPath, reference);
                } else {
                    File referenceFile = new File(reference);
                    return KontinuumUtils.normalizeDir(workbenchPath, referenceFile.getParentFile().getPath());
                }
            }

            public String getContextDir(ProjectProperties.Project project, String workbenchPath) {
                if (StringUtils.isBlank(project.getTenant())) {
                    throw new IllegalStateException("Tried to access tenant for project " + project + " but is not set.");
                }

                if (StringUtils.isBlank(assessmentId)) {
                    throw new IllegalStateException("Tried to access assessment id for asset " + this + " but is not set.");
                }
                
                if (StringUtils.isBlank(context)) {
                    throw new IllegalStateException("Tried to access context for asset " + this + " but is not set.");
                }

                return KontinuumUtils.normalizeDir(workbenchPath, "assessments", project.getTenant(), assessmentId, context, "context");
            }

            public String getAssessmentDir(ProjectProperties.Project project, String workbenchPath) {
                if (StringUtils.isBlank(project.getTenant())) {
                    throw new IllegalStateException("Tried to access tenant for project " + project + " but is not set.");
                }

                if (StringUtils.isBlank(assessmentId)) {
                    throw new IllegalStateException("Tried to access assessment id for asset " + this + " but is not set.");
                }

                return KontinuumUtils.normalizeDir(workbenchPath, "assessments", project.getTenant(), assessmentId);
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                if (StringUtils.isNotBlank(getName())) {
                    sb.append(getName());
                } else {
                    sb.append("unnamed-asset");
                }

                if (StringUtils.isNotBlank(getVersion())) {
                    sb.append("-").append(getVersion());
                }
                return sb.toString();
            }
        }
    }

    @Data
    public static class Report {
        private String assetId;
        private List<String> types;
        private List<String> overviewAdvisors;
        private String watermark;
        private String organization;
        private String classificationRating;
        private String controlRating;
    }

    @Data
    public static class Dashboard {
        private String assetId;
    }

    @Data
    public static class PortfolioManager {
        private String project;
        private String assetGroup;
    }

    @Data
    public static class Options {

        private GlobalOptions global = new GlobalOptions();
        private EnrichmentOptions enrichment = new EnrichmentOptions();

        @Data
        public static class GlobalOptions{
            private String documentLanguage = "en";
            private Boolean enableResolve = false;
            private Boolean enableSpdxBom = false;
            private Boolean enableCycloneDxBom = false;
        }

        @Data
        public static class EnrichmentOptions{

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


            public String getSecurityPolicyFile(String workbenchPath) throws IllegalStateException{
                if (StringUtils.isBlank(securityPolicyFile)) {
                    throw new IllegalStateException("Tried to access reference inventory for asset " + this + " but is not set.");
                }

                File file = new File(securityPolicyFile);
                return KontinuumUtils.normalizeDir(workbenchPath, file.getPath());
            }
        }
    }
}
