package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.generator.shared.Pipeline;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineExecution;
import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.*;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.ProcessorParameter;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.StandaloneProcessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class generates a gitlab pipeline from the given configuration files to include as a
 * downstream pipeline in a gitlab project.
 */
@Slf4j
public class GitlabPipeline {

    PipelineExecution execution;

    StringBuilder gitlabPipelineDocument = new StringBuilder();

    GitlabConfiguration gitlabConfiguration;

    /**
     * Creates a GitLab pipeline from the given pipeline and GitLab configuration.
     *
     * @param pipelineConfiguration the pipeline configuration to generate from
     * @param gitlabConfiguration   the GitLab-specific configuration
     */
    public GitlabPipeline(PipelineConfiguration pipelineConfiguration, GitlabConfiguration gitlabConfiguration) {
        this.gitlabConfiguration = gitlabConfiguration;
        Pipeline pipeline = new Pipeline(pipelineConfiguration, gitlabConfiguration);
        execution = pipeline.generatePipeline();
    }

    /**
     * Generates the complete GitLab CI pipeline document.
     *
     * @return the generated GitLab pipeline document
     */
    public String generatePipeline() {
        execution.validate();
        generateStagesSection();
        generateVariablesSection();
        generateDefaultSection();
        generateJobsSection();
        return gitlabPipelineDocument.toString();
    }

    /**
     * Appends the stages section to the GitLab pipeline document.
     */
    public void generateStagesSection() {
        StringBuilder stagesSection = new StringBuilder();
        stagesSection.append("stages:").append(System.lineSeparator());
        Set<String> requiredStages = new HashSet<>();

        execution.getContexts().stream()
                .flatMap(ctx -> ctx.getProcessors().stream())
                .forEach(p -> requiredStages.add(p.getStage().name()));

        for (String stage : requiredStages.stream()
                .sorted(Comparator.comparingInt(s -> Stage.valueOf(s).ordinal()))
                .toList()) {
            stagesSection.append("  - ").append(stage).append(System.lineSeparator());
        }

        gitlabPipelineDocument.append(stagesSection.append(System.lineSeparator()));
    }

    /**
     * Appends the variables section to the GitLab pipeline document.
     */
    public void generateVariablesSection() {
        StringBuilder variablesSection = new StringBuilder();
        variablesSection.append("variables:").append(System.lineSeparator())
                .append("  GIT_DEPTH: ").append(gitlabConfiguration.GIT_DEPTH).append(System.lineSeparator())
                .append("  GIT_STRATEGY: ").append(gitlabConfiguration.GIT_STRATEGY)
                .append(System.lineSeparator())
                .append("  CONTAINER_IMAGE: ").append(gitlabConfiguration.CONTAINER_IMAGE).append(
                        System.lineSeparator());

        gitlabPipelineDocument.append(variablesSection).append(System.lineSeparator());
    }

    /**
     * Appends the default section to the GitLab pipeline document.
     */
    public void generateDefaultSection() {
        StringBuilder defaultContent = new StringBuilder();

        if (StringUtils.isNotBlank(gitlabConfiguration.RUNNER_TAG)) {
            defaultContent.append("  tags:").append(System.lineSeparator())
                    .append("    - ").append(gitlabConfiguration.RUNNER_TAG).append(System.lineSeparator());
        }

        if (!defaultContent.isEmpty()) {
            gitlabPipelineDocument.append("default:").append(System.lineSeparator()).append(defaultContent).append(
                    System.lineSeparator());
        }
    }

    /**
     * Appends the jobs section to the GitLab pipeline document.
     */
    public void generateJobsSection() {
        for (ExecutionContext context : execution.getContexts()) {
            Map<String, List<Processor>> stageToProcessorMap = orderProcessorsByStage(context);

            for (Map.Entry<String, List<Processor>> stageEntry : stageToProcessorMap.entrySet()) {
                String stage = stageEntry.getKey();

                StringBuilder job = new StringBuilder();
                job.append(generateJobName(context, stage)).append(":").append(System.lineSeparator());
                job.append("  ").append("stage: ").append(stage).append(System.lineSeparator());
                job.append("  ").append("image: ").append(gitlabConfiguration.CONTAINER_IMAGE).append(
                        System.lineSeparator());
                job.append("  ").append("script: ").append(System.lineSeparator());
                job.append("    - |").append(System.lineSeparator());
                job.append("      set -o pipefail").append(System.lineSeparator());

                for (Processor processor : stageEntry.getValue()) {

                    if (processor instanceof MavenProcessor mavenProcessor) {

                        if (StringUtils.isNotEmpty(gitlabConfiguration.CONTAINER_PATCH_DIRECTORY)) {
                            job.append(generateContainerPatchScript(6)).append(System.lineSeparator());
                        }

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
                    job.append(System.lineSeparator());
                }

                job.append("      ").append("2>&1 | tee $CI_JOB_NAME.log").append(System.lineSeparator());
                job.append("  ").append("artifacts: ").append(System.lineSeparator());
                job.append("    ").append("when: on_failure").append(System.lineSeparator());
                job.append("    ").append("paths:").append(System.lineSeparator());
                job.append("      ").append("- $CI_JOB_NAME.log").append(System.lineSeparator());
                job.append("    ").append("expire_in: 1 Week").append(System.lineSeparator());
                gitlabPipelineDocument.append(job).append(System.lineSeparator());
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
            if (parameter != null && StringUtils.isNotBlank(parameter.getValue())) {
                script.append(" ").append(parameter.getValue());
            }
        }
        return script.append(System.lineSeparator()).toString();
    }

    private String generateContainerPatchScript(int indent) {
        StringBuilder containerPatchScript = new StringBuilder();
        String patchDirectory = gitlabConfiguration.CONTAINER_PATCH_DIRECTORY;
        if (patchDirectory.endsWith("/")) {
            patchDirectory = patchDirectory.substring(0, patchDirectory.length() - 1);
        }

        containerPatchScript.append(" ".repeat(indent))
                .append("cp -r \"")
                .append(patchDirectory)
                .append("/.\" /");

        return containerPatchScript.append(System.lineSeparator()).toString();
    }


    private Map<String, List<Processor>> orderProcessorsByStage(ExecutionContext context) {
        return context.getProcessors().stream()
                .collect(Collectors.groupingBy(
                        processor -> processor.getStage().name(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private String generateJobName(ExecutionContext context, String stage) {
        return context.getName() + "-" + stage + "-" + UUID.randomUUID();
    }

    private String sanitizeNameSegment(String segment) {
        return segment.trim().replaceAll("[^a-zA-Z0-9_.-]+", "-");
    }
}
