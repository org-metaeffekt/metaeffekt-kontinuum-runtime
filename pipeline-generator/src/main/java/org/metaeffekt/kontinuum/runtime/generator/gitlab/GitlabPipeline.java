package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.generator.shared.Pipeline;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.ProcessorParameter;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.StandaloneProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

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

    public void generateJobsSection() {
        for (Map.Entry<Asset, List<Processor>> entry : assetProcessorsMap.entrySet()) {
            Processor lastProcessor = null;
            for (Processor processor : entry.getValue()) {

                StringBuilder job = new StringBuilder();
                job.append(generateJobName(processor, entry.getKey().toString(), processor.getStage())).append(":").append(System.lineSeparator());
                job.append("  ").append("stage: ").append(processor.getStage().name()).append(System.lineSeparator());
                job.append("  ").append("image: ").append(gitlabConfiguration.CONTAINER_IMAGE).append(System.lineSeparator());

                if (lastProcessor != null && Objects.equals(lastProcessor.getStage().name(), processor.getStage().name())) {
                    if (!processor.getStage().equals(Stage.REPORT)) {
                        job.append("  ").append("needs: [")
                                .append(generateJobName(lastProcessor, entry.getKey().toString(), processor.getStage()))
                                .append("]").append(System.lineSeparator());
                    }
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
        script.append("      mvn ")
                .append(gitlabConfiguration.MAVEN_CLI_OPTS)
                .append(" -f ")
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

    private String generateJobName(Processor processor, String assetName, Stage stage) {
        StringBuilder processorName =  new StringBuilder()
                .append(assetName)
                .append("-")
                .append(processor.getId())
                .append("-")
                .append(stage.name());

        if (processor instanceof MavenProcessor mavenProcessor
                && processor.getId().equals("create-document")) {
            Optional<ProcessorParameter> processorParameter = mavenProcessor.getParameters()
                    .stream()
                    .filter(p -> p.getKey() == ProcessorParameterKey.PARAM_DOCUMENT_TYPE)
                    .findFirst();

            processorParameter.ifPresent(parameter -> processorName.append("-").append(parameter.getValue()));
        }
        return processorName.toString();
    }
 }
