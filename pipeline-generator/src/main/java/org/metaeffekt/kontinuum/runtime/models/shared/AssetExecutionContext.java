package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;

/**
 * Execution context that utilized by stage handlers during pipeline generation.
 * Tracks the current asset, configuration and passes information such as output paths from previous stages.
 */
@Getter
public class AssetExecutionContext extends AbstractExecutionContext {

    private final Asset asset;
    private final PipelineConfiguration configuration;
    private final EnvironmentConfiguration environment;
    private final Workspace workspace;
    private final ProcessorCatalog processorCatalog;

    /**
     * Tracks the path to the artifact file produced by the most recent stage.
     * Each stage that produces an output artifact should update this field so downstream
     * stages can consume it.
     */
    @Setter
    private String currentInventoryFile;

    /**
     * Tracks the directory containing the last output artifact.
     * Used by stages that need to reference the directory rather than the file.
     */
    @Setter
    private String currentInventoryDir;

    /**
     * Tracks the path of the reference inventory pulled from the portfolio manager if active.
     */
    @Setter
    private String portfolioManagerReferenceInventoryDir;

    public AssetExecutionContext(Asset asset,
                                 PipelineConfiguration configuration,
                                 EnvironmentConfiguration environment,
                                 Workspace workspace,
                                 ProcessorCatalog processorCatalog) {
        this.asset = asset;
        this.configuration = configuration;
        this.environment = environment;
        this.workspace = workspace;
        this.processorCatalog = processorCatalog;
    }

    @Override
    public String getName() {
        return asset != null ? asset.toString() : "asset";
    }

    public void removeProcessorsWithId(DefaultProcessorCatalog.ProcessorIds processorId) {
        getProcessors().removeIf(processor -> processor.getId().equals(processorId.getValue()));
    }

    public Workspace.AssetPath getStageDirForAsset(Stage stage) {
        return workspace.getStageDirForAsset(asset, stage);
    }

    public Workspace.AssetPath getGroupedStage(PipelineConfiguration.Report report, ReportType reportType, SupportedLocale locale) {
        return workspace.getGroupedDir(report, asset, reportType, locale);
    }

    public Asset getRootAsset() {
        if (configuration == null || configuration.getProjectProperties() == null) {
            return asset;
        }
        return configuration.getProjectProperties().getRootAssetFor(asset);
    }
}
