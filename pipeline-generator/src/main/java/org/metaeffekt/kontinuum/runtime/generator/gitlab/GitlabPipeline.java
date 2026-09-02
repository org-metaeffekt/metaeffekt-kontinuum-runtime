package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.generator.shared.Pipeline;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.ProcessorParameter;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.StandaloneProcessor;

/**
 * This class generates a gitlab pipeline from the given configuration files to include as a
 * downstream pipeline in a gitlab project.
 */
@Slf4j
public class GitlabPipeline {

    Map<Asset, List<Processor>> assetProcessorsMap;

    StringBuilder gitlabPipelineDocument = new StringBuilder();

    GitlabConfiguration gitlabConfiguration;

    public GitlabPipeline(PipelineConfiguration pipelineConfiguration, GitlabConfiguration gitlabConfiguration) {
        this.gitlabConfiguration = gitlabConfiguration;
        Pipeline pipeline = new Pipeline(pipelineConfiguration, gitlabConfiguration);
        assetProcessorsMap = pipeline.generatePipeline();
    }

    public String generatePipeline() {
        generateStagesSection();
        generateVariablesSection();
        generateDefaultSection();
        generateJobsSection();
        return gitlabPipelineDocument.toString();
    }

    public void generateStagesSection() {
        StringBuilder stagesSection = new StringBuilder();
        stagesSection.append("stages:").append(System.lineSeparator());
        Set<String> requiredStages = new HashSet<>();

        assetProcessorsMap.values().stream()
            .flatMap(List::stream)
            .forEach(p -> requiredStages.add(p.getStage().name()));

        for (String stage : requiredStages.stream()
                .sorted(Comparator.comparingInt(s -> Stage.valueOf(s).ordinal()))
                .toList()) {
            stagesSection.append("  - ").append(stage).append(System.lineSeparator());
        }

        gitlabPipelineDocument.append(stagesSection.append(System.lineSeparator()));
    }

    public void generateVariablesSection() {
        StringBuilder variablesSection = new StringBuilder();
        variablesSection.append("variables:").append(System.lineSeparator())
                .append("  GIT_DEPTH: ").append(gitlabConfiguration.GIT_DEPTH).append(System.lineSeparator())
                .append("  GIT_STRATEGY: ").append(gitlabConfiguration.GIT_STRATEGY).append(System.lineSeparator())
                .append("  CONTAINER_IMAGE: ").append(gitlabConfiguration.CONTAINER_IMAGE).append(System.lineSeparator());

        gitlabPipelineDocument.append(variablesSection).append(System.lineSeparator());
    }

    public void generateDefaultSection() {
        StringBuilder defaultContent = new StringBuilder();

        if (StringUtils.isNotBlank(gitlabConfiguration.RUNNER_TAG)) {
            defaultContent.append("  tags:").append(System.lineSeparator())
                    .append("    - ").append(gitlabConfiguration.RUNNER_TAG).append(System.lineSeparator());
        }

        if (!defaultContent.isEmpty()) {
            gitlabPipelineDocument.append("default:").append(System.lineSeparator()).append(defaultContent).append(System.lineSeparator());
        }
    }

    private static final List<ProcessorParameterKey> DISCRIMINATOR_KEYS = List.of(
            ProcessorParameterKey.PARAM_DOCUMENT_TYPE,
            ProcessorParameterKey.PARAM_DOCUMENT_LANGUAGE,
            ProcessorParameterKey.PARAM_LANGUAGE_MODE,
            ProcessorParameterKey.PARAM_OUTPUT_FORMAT,
            ProcessorParameterKey.PARAM_OUTPUT_MODE,
            ProcessorParameterKey.PARAM_SOURCE_MODE
    );

    public void generateJobsSection() {
        Map<Processor, String> jobNames = assignJobNames();

        for (Map.Entry<Asset, List<Processor>> entry : assetProcessorsMap.entrySet()) {
            Processor lastProcessor = null;
            for (Processor processor : entry.getValue()) {
                String jobName = jobNames.get(processor);

                StringBuilder job = new StringBuilder();
                job.append(jobName).append(":").append(System.lineSeparator());
                job.append("  ").append("stage: ").append(processor.getStage().name()).append(System.lineSeparator());
                job.append("  ").append("image: ").append(gitlabConfiguration.CONTAINER_IMAGE).append(System.lineSeparator());

                if (lastProcessor != null && Objects.equals(lastProcessor.getStage().name(), processor.getStage().name())) {
                    job.append("  ").append("needs: [")
                            .append(jobNames.get(lastProcessor))
                            .append("]").append(System.lineSeparator());
                }

                job.append("  ").append("script: ").append(System.lineSeparator());
                job.append("    - |").append(System.lineSeparator());

                if (processor instanceof MavenProcessor mavenProcessor) {
                    if (mavenProcessor.getPreScript() != null) {
                        job.append(mavenProcessor.getPreScript(6)).append(System.lineSeparator());
                    }

                    job.append(generateMavenScriptBlock(mavenProcessor));

                    if (mavenProcessor.getPostScript() != null) {
                        job.append(mavenProcessor.getPostScript(6)).append(System.lineSeparator());
                    }
                } else if (processor instanceof StandaloneProcessor standaloneProcessor) {
                    job.append(generateStandaloneScriptBlock(standaloneProcessor));
                }

                gitlabPipelineDocument.append(job).append(System.lineSeparator());
                lastProcessor = processor;
            }
        }
    }

    private String generateMavenScriptBlock(ProcessorDefinitions.MavenProcessor processor) {
        StringBuilder script = new StringBuilder();
        script.append("      mvn ");
        if (StringUtils.isNotBlank(gitlabConfiguration.MAVEN_CLI_OPTS)) {
            script.append(gitlabConfiguration.MAVEN_CLI_OPTS).append(" ");
        }
        if (StringUtils.isNotBlank(gitlabConfiguration.LOCAL_MAVEN_REPO)) {
            script.append("-Dmaven.repo.local=").append(gitlabConfiguration.LOCAL_MAVEN_REPO).append(" ");
        }
        script.append("-f ")
                .append(gitlabConfiguration.getKontinuumProcessorsDirNormalized())
                .append(processor.getPomLocation())
                .append(" process-resources").append(" \\").append(System.lineSeparator());

        List<ProcessorParameter> nonBlankParams = processor.getParameters().stream()
                .filter(p -> StringUtils.isNotBlank(p.getValue()))
                .toList();

        for (int i = 0; i < nonBlankParams.size(); i++) {
            ProcessorParameter param = nonBlankParams.get(i);
            script.append("      -D").append(param.getKey()).append("='").append(param.getValue()).append("'");
            if (i < nonBlankParams.size() - 1) {
                script.append(" \\");
            }
            script.append(System.lineSeparator());
        }

        return script.toString();
    }

    private String generateStandaloneScriptBlock(StandaloneProcessor processor) {
        StringBuilder script = new StringBuilder();
        script.append("      sh ")
                .append(gitlabConfiguration.getKontinuumProcessorsDirNormalized())
                .append(processor.getScriptLocation());

        for (ProcessorParameter parameter : processor.getParameters()) {
            script.append(" ").append(parameter.getValue());
        }
        return script.append(System.lineSeparator()).toString();
    }

    private Map<Processor, String> assignJobNames() {
        Map<Processor, String> jobNameMap = new IdentityHashMap<>();
        Map<String, Integer> nameCounts = new HashMap<>();

        for (Map.Entry<Asset, List<Processor>> entry : assetProcessorsMap.entrySet()) {
            String assetName = entry.getKey().toString();
            for (Processor processor : entry.getValue()) {
                String baseName = buildBaseJobName(processor, assetName);
                int count = nameCounts.getOrDefault(baseName, 0) + 1;
                nameCounts.put(baseName, count);

                String uniqueName = (count == 1) ? baseName : baseName + "-" + count;
                jobNameMap.put(processor, uniqueName);
            }
        }
        return jobNameMap;
    }

    private String buildBaseJobName(Processor processor, String assetName) {
        StringBuilder name = new StringBuilder()
                .append(assetName)
                .append("-")
                .append(processor.getId())
                .append("-")
                .append(processor.getStage().name());

        for (ProcessorParameterKey key : DISCRIMINATOR_KEYS) {
            processor.getParameters().stream()
                    .filter(p -> p.getKey() == key && StringUtils.isNotBlank(p.getValue()))
                    .findFirst()
                    .ifPresent(p -> name.append("-").append(sanitizeNameSegment(p.getValue())));
        }

        return name.toString();
    }

    private String sanitizeNameSegment(String segment) {
        return segment.trim().replaceAll("[^a-zA-Z0-9_.-]+", "-");
    }
 }
