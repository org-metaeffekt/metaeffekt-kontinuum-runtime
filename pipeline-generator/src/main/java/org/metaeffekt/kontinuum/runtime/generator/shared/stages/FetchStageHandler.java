package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.ProcessorParameterKey.*;

public class FetchStageHandler implements StageHandler {

    @Override
    public Stage getStage() {
        return Stage.FETCH;
    }

    @Override
    public void process(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        if (asset.getUrlResolver() != null) {
            handleUrlResolver(context);
        } else if (asset.getMavenResolver() != null) {
            handleMavenResolver(context);
        } else if (asset.getContainerResolver() != null) {
            handleContainerResolver(context);
        } else {
            // This exception is only thrown if there are errors in the PipelineConfigurationLoader and should have been cought by tests.
            throw new IllegalStateException(String.format("Asset %s has no resolver configured but passed the pipeline configuration validation.", asset.getId()));
        }
    }

    private void handleUrlResolver(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        Asset.UrlResolver urlResolver = asset.getUrlResolver();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(DOWNLOAD_ASSET);
        processor.setStage(Stage.FETCH);
        processor.setProcessorParameter(PARAM_ASSET_URL, urlResolver.getUrl());
        processor.setProcessorParameter(OUTPUT_ASSET_DIR, context.getStageDirForAsset(Stage.FETCH).toString());
        processor.setProcessorParameter(PARAM_ASSET_USERNAME, urlResolver.getUsername());
        processor.setProcessorParameter(PARAM_ASSET_PASSWORD, urlResolver.getPassword());
        processor.setProcessorParameter(PARAM_ASSET_TOKEN, urlResolver.getToken());
        processor.setProcessorParameter(PARAM_ASSET_HEADER_NAME, urlResolver.getHeaderName());
        processor.setProcessorParameter(PARAM_ASSET_HEADER_VALUE, urlResolver.getHeaderValue());

        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.FETCH).toString());
        context.addProcessor(processor);
    }

    private void handleMavenResolver(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        Asset.MavenResolver mavenResolver = asset.getMavenResolver();

        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(DOWNLOAD_MAVEN_ARTIFACT);
        processor.setStage(Stage.FETCH);
        processor.setProcessorParameter(PARAM_GROUP_ID, mavenResolver.getGroupId());
        processor.setProcessorParameter(PARAM_ARTIFACT_ID, mavenResolver.getArtifactId());
        processor.setProcessorParameter(PARAM_VERSION, mavenResolver.getArtifactVersion());
        processor.setProcessorParameter(PARAM_REPO_URL, mavenResolver.getRepoUrl());
        processor.setProcessorParameter(OUTPUT_ASSET_DIR, context.getStageDirForAsset(Stage.FETCH).toString());

        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.FETCH).toString());
        context.addProcessor(processor);
    }

    private void handleContainerResolver(AssetExecutionContext context) {
        Asset asset = context.getAsset();
        MavenProcessor processor = (MavenProcessor) context.getProcessorCatalog().getProcessorById(SAVE_INSPECT_IMAGE);
        processor.setStage(Stage.FETCH);

        processor.setProcessorParameter(OUTPUT_DIR, context.getStageDirForAsset(Stage.FETCH).toString());
        processor.setProcessorParameter(PARAM_IMAGE_ID, asset.getContainerResolver().getImage());
        processor.setProcessorParameter(PARAM_IMAGE_VERSION, asset.getContainerResolver().getTag());

        context.setCurrentInventoryDir(context.getStageDirForAsset(Stage.FETCH).toString());
        context.addProcessor(processor);
    }
}
