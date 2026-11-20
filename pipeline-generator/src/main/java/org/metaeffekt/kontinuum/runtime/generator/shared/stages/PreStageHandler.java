package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.gitlab.GitlabConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.COPY_DIRECTORY;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.DOWNLOAD_INDEX;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.ENV_VULNERABILITY_MIRROR_DIR;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.INPUT_DIR;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.OUTPUT_DIR;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.PARAM_MIRROR_ARCHIVE_URL;

/**
 * Handler for the {@link Stage#PRE} stage.
 * Responsible for preliminary setup tasks such as updating and downloading the vulnerability index.
 */
public class PreStageHandler implements PipelineStageHandler {

    @Override
    public Stage getStage() {
        return Stage.PRE;
    }

    @Override
    public void process(PipelineExecutionContext context) {

        /*
         *   Copy maven dependencies from container to local maven repo if configured but only for GitLab pipelines.
         *   This MUST run as the first processor in the pipeline.
         */
        ProcessorDefinitions.StandaloneProcessor localMavenRepoCopyProcessor = null;
        if (context.getEnvironment() instanceof GitlabConfiguration
            && context.getEnvironment().LOCAL_MAVEN_REPO != null) {
            localMavenRepoCopyProcessor = handleMavenRepoCopy(context);
        }

        if (context.getConfiguration().requiresVulnerabilityEnrichment()) {
            MavenProcessor vulnerabilityIndexDownloadProcessor = handleVulnerabilityIndexDownload(context);
            if (localMavenRepoCopyProcessor != null) {
                context.addDependency(vulnerabilityIndexDownloadProcessor, localMavenRepoCopyProcessor);
            }
            context.addProcessor(vulnerabilityIndexDownloadProcessor);
        }
    }

    /**
     * Downloads the vulnerability index if the mirror is not currently up to date.
     *
     * @param context The pipeline execution context containing pipeline information.
     * @return The configured {@link MavenProcessor} for downloading the index.
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/mirror/mirror_download-index.md">mirror_download-index.md</a>
     */
    private MavenProcessor handleVulnerabilityIndexDownload(PipelineExecutionContext context) {
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(DOWNLOAD_INDEX);
        processor.setStage(Stage.PRE);

        processor.setProcessorParameter(PARAM_MIRROR_ARCHIVE_URL, context.getEnvironment().VULNERABILITY_MIRROR_URL);
        processor.setProcessorParameter(ENV_VULNERABILITY_MIRROR_DIR, context.getEnvironment().getMirrorDir());

        return processor;
    }

    private ProcessorDefinitions.StandaloneProcessor handleMavenRepoCopy(PipelineExecutionContext context) {
        ProcessorDefinitions.StandaloneProcessor processor =
                (ProcessorDefinitions.StandaloneProcessor) context.getProcessorCatalog()
                .getProcessorById(COPY_DIRECTORY);
        processor.setStage(Stage.PRE);

        processor.setProcessorParameter(INPUT_DIR, "/root/.m2/repository");
        processor.setProcessorParameter(OUTPUT_DIR, context.getEnvironment().LOCAL_MAVEN_REPO);

        return processor;
    }
}
