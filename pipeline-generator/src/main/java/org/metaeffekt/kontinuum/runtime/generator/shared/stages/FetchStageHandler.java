package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

/**
 * Handler for the {@link Stage#FETCH} stage.
 * Responsible for retrieving the target asset via URL, Maven coordinates, or container image resolver.
 * Exactly one resolver is executed per asset, as enforced by pipeline configuration validation.
 */
public class FetchStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.FETCH;
    }

    @Override
    public void process(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor fetchProcessor;
        if (asset.getUrlResolver() != null) {
            fetchProcessor = handleUrlResolver(context);
        } else if (asset.getMavenResolver() != null) {
            fetchProcessor = handleMavenResolver(context);
        } else if (asset.getContainerResolver() != null) {
            fetchProcessor = handleContainerResolver(context);
        } else {
            // This exception is only thrown if there are errors in the PipelineConfigurationLoader and should have been caught by tests.
            throw new IllegalStateException(String.format("Asset %s has no resolver configured but passed the pipeline configuration validation.", asset.getId()));
        }
        context.addProcessor(fetchProcessor);
    }

    /**
     * Downloads an asset from a file URL, remote file URL, or generic URL into the workspace stage directory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/fetch/fetch_download-asset.md">fetch_download-asset.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for downloading the asset.
     */
    private MavenProcessor handleUrlResolver(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        Asset.UrlResolver urlResolver = asset.getUrlResolver();
        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(DOWNLOAD_ASSET);
        mavenProcessor.setStage(Stage.FETCH);
        mavenProcessor.setProcessorParameter(PARAM_ASSET_URL, urlResolver.getUrl());
        mavenProcessor.setProcessorParameter(OUTPUT_ASSET_DIR, context.getStageDirForAsset(Stage.FETCH).toString());
        mavenProcessor.setProcessorParameter(PARAM_USERNAME, urlResolver.getUsername());
        mavenProcessor.setProcessorParameter(PARAM_PASSWORD, urlResolver.getPassword());
        mavenProcessor.setProcessorParameter(PARAM_TOKEN, urlResolver.getToken());
        mavenProcessor.setProcessorParameter(PARAM_HEADER_NAME, urlResolver.getHeaderName());
        mavenProcessor.setProcessorParameter(PARAM_HEADER_VALUE, urlResolver.getHeaderValue());

        return mavenProcessor;
    }

    /**
     * Downloads a Maven artifact using groupId, artifactId, and version conventions from a Maven repository into the workspace stage directory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/fetch/fetch_download-maven-artifact.md">fetch_download-maven-artifact.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for downloading the Maven artifact.
     */
    private MavenProcessor handleMavenResolver(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        Asset.MavenResolver mavenResolver = asset.getMavenResolver();

        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(DOWNLOAD_MAVEN_ARTIFACT);
        mavenProcessor.setStage(Stage.FETCH);
        mavenProcessor.setProcessorParameter(PARAM_GROUP_ID, mavenResolver.getGroupId());
        mavenProcessor.setProcessorParameter(PARAM_ARTIFACT_ID, mavenResolver.getArtifactId());
        mavenProcessor.setProcessorParameter(PARAM_VERSION, mavenResolver.getArtifactVersion());
        mavenProcessor.setProcessorParameter(PARAM_REPO_URL, mavenResolver.getRepoUrl());
        mavenProcessor.setProcessorParameter(OUTPUT_ASSET_DIR, context.getStageDirForAsset(Stage.FETCH).toString());

        return mavenProcessor;
    }

    /**
     * Downloads and saves a container image using its repository URL, image ID, and version tag into the workspace stage directory.
     *
     * @see <a href="https://github.com/org-metaeffekt/metaeffekt-kontinuum/blob/main/processors/fetch/fetch_save-image.md">fetch_save-image.md</a>
     * @param context The asset execution context containing pipeline and asset information.
     * @return The configured {@link MavenProcessor} for saving and inspecting the container image.
     */
    private MavenProcessor handleContainerResolver(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor mavenProcessor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(SAVE_INSPECT_IMAGE);
        mavenProcessor.setStage(Stage.FETCH);

        mavenProcessor.setProcessorParameter(OUTPUT_DIR, context.getStageDirForAsset(Stage.FETCH).toString());
        mavenProcessor.setProcessorParameter(PARAM_IMAGE_ID, asset.getContainerResolver().getImage());
        mavenProcessor.setProcessorParameter(PARAM_IMAGE_VERSION, asset.getContainerResolver().getTag());

        String repoUrl = asset.getContainerResolver().getRepoUrl();
        if (StringUtils.isBlank(repoUrl)) {
            repoUrl = "docker.io";
        }
        mavenProcessor.setProcessorParameter(PARAM_REPO_URL, repoUrl);

        return mavenProcessor;
    }
}
