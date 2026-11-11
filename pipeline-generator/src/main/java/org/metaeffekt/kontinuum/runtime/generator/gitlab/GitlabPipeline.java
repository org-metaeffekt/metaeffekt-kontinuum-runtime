package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.generator.shared.LineEmitter;
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
        LineEmitter document = new LineEmitter();
        generateStagesSection(document);
        generateVariablesSection(document);
        generateDefaultSection(document);
        generateJobsSection(document);
        return document.render();
    }

    private void generateStagesSection(LineEmitter document) {
        document.line("stages:");
        Set<String> requiredStages = new HashSet<>();

        execution.getContexts().stream()
                .flatMap(ctx -> ctx.getProcessors().stream())
                .forEach(p -> requiredStages.add(p.getStage().name()));

        try (var stageIndent = document.indent()) {
            for (String stage : requiredStages.stream()
                    .sorted(Comparator.comparingInt(s -> Stage.valueOf(s).ordinal()))
                    .toList()) {
                document.line("- " + stage);
            }
        }
        document.blankLine();
    }

    private void generateVariablesSection(LineEmitter document) {
        document.line("variables:");
        try (var variablesIndent = document.indent()) {
            document.line("GIT_DEPTH: " + gitlabConfiguration.GIT_DEPTH);
            document.line("GIT_STRATEGY: " + gitlabConfiguration.GIT_STRATEGY);
            document.line("CONTAINER_IMAGE: " + gitlabConfiguration.CONTAINER_IMAGE);
        }
        document.blankLine();
    }

    private void generateDefaultSection(LineEmitter document) {
        if (StringUtils.isBlank(gitlabConfiguration.RUNNER_TAG)) {
            return;
        }

        document.line("default:");
        try (var defaultIndent = document.indent()) {
            document.line("tags:");
            try (var tagsIndent = document.indent()) {
                document.line("- " + gitlabConfiguration.RUNNER_TAG);
            }
        }
        document.blankLine();
    }

    private void generateJobsSection(LineEmitter document) {
        for (ExecutionContext context : execution.getContexts()) {
            Map<String, List<Processor>> stageToProcessorMap = orderProcessorsByStage(context);

            for (Map.Entry<String, List<Processor>> stageEntry : stageToProcessorMap.entrySet()) {
                String stage = stageEntry.getKey();
                generateJob(document, context, stage, stageEntry.getValue());
            }
        }
    }

    private void generateJob(LineEmitter document, ExecutionContext context, String stage, List<Processor> processors) {
        document.line(generateJobName(context, stage) + ":");
        try (var jobIndent = document.indent()) {
            document.line("stage: " + stage);
            document.line("image: " + gitlabConfiguration.CONTAINER_IMAGE);
            document.line("script:");
            try (var scriptListIndent = document.indent()) {
                document.line("- |");
                try (var scriptIndent = document.indent()) {
                    document.line("set -o pipefail");
                    for (Processor processor : processors) {
                        if (processor instanceof MavenProcessor mavenProcessor) {
                            if (StringUtils.isNotEmpty(gitlabConfiguration.CONTAINER_PATCH_DIRECTORY)) {
                                generateContainerPatchScript(document);
                            }
                            if (StringUtils.isNotBlank(mavenProcessor.getPreScript())) {
                                document.multiline(mavenProcessor.getPreScript());
                            }
                            generateMavenScriptBlock(document, mavenProcessor);
                            if (StringUtils.isNotBlank(mavenProcessor.getPostScript())) {
                                document.multiline(mavenProcessor.getPostScript());
                            }
                        } else if (processor instanceof StandaloneProcessor standaloneProcessor) {
                            generateStandaloneScriptBlock(document, standaloneProcessor);
                        }
                        document.blankLine();
                    }
                    document.line("2>&1 | tee $CI_JOB_NAME.log");
                }
            }
            generateArtifactsSection(document);
        }
        document.blankLine();
    }

    private void generateArtifactsSection(LineEmitter document) {
        document.line("artifacts:");
        try (var artifactsIndent = document.indent()) {
            document.line("when: on_failure");
            document.line("paths:");
            try (var pathsIndent = document.indent()) {
                document.line("- $CI_JOB_NAME.log");
            }
            document.line("expire_in: 1 Week");
        }
    }

    private void generateMavenScriptBlock(LineEmitter document, MavenProcessor processor) {
        List<ProcessorParameter> nonBlankParams = processor.getParameters().stream()
                .filter(p -> StringUtils.isNotBlank(p.getValue()))
                .toList();

        StringBuilder command = new StringBuilder("mvn ");
        if (StringUtils.isNotBlank(gitlabConfiguration.MAVEN_CLI_OPTS)) {
            command.append(gitlabConfiguration.MAVEN_CLI_OPTS).append(" ");
        }
        if (StringUtils.isNotBlank(gitlabConfiguration.LOCAL_MAVEN_REPO)) {
            command.append("-Dmaven.repo.local=")
                    .append(PosixShellQuoting.quote(gitlabConfiguration.LOCAL_MAVEN_REPO))
                    .append(" ");
        }
        String pomPath = gitlabConfiguration.getKontinuumProcessorsDirNormalized() + processor.getPomLocation();
        command.append("-f ").append(PosixShellQuoting.quote(pomPath)).append(" process-resources");
        if (!nonBlankParams.isEmpty()) {
            command.append(" \\");
        }
        document.line(command.toString());

        for (int i = 0; i < nonBlankParams.size(); i++) {
            ProcessorParameter param = nonBlankParams.get(i);
            StringBuilder argument = new StringBuilder("-D")
                    .append(param.getKey())
                    .append("=")
                    .append(PosixShellQuoting.quote(param.getValue()));
            if (i < nonBlankParams.size() - 1) {
                argument.append(" \\");
            }
            document.line(argument.toString());
        }
    }

    private void generateStandaloneScriptBlock(LineEmitter document, StandaloneProcessor processor) {
        String scriptPath = gitlabConfiguration.getKontinuumProcessorsDirNormalized() + processor.getScriptLocation();
        StringBuilder command = new StringBuilder("sh ").append(PosixShellQuoting.quote(scriptPath));

        for (ProcessorParameter parameter : processor.getParameters()) {
            if (parameter != null && StringUtils.isNotBlank(parameter.getValue())) {
                command.append(" ").append(PosixShellQuoting.quote(parameter.getValue()));
            }
        }
        document.line(command.toString());
    }

    private void generateContainerPatchScript(LineEmitter document) {
        String patchDirectory = gitlabConfiguration.CONTAINER_PATCH_DIRECTORY;
        if (patchDirectory.endsWith("/")) {
            patchDirectory = patchDirectory.substring(0, patchDirectory.length() - 1);
        }
        document.line("cp -r \"" + patchDirectory + "/.\" /");
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
