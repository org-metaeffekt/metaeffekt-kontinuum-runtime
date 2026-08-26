package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.ArrayList;
import java.util.List;

/**
 * Execution context that utilized by stage handlers during pipeline generation.
 * Tracks the current asset, configuration and passes information such as output paths from previous stages.
 */
@Getter
public class AssetExecutionContext {

    private final Asset asset;
    private final PipelineConfiguration configuration;
    private final EnvironmentConfiguration environment;
    private final Workspace workspace;
    private final ProcessorCatalog processorCatalog;

    private final List<Processor> processors = new ArrayList<>();

    /**
     * Tracks the path to the artifact file produced by the most recent stage.
     * Each stage that produces an output artifact should update this field so downstream
     * stages can consume it.
     */
    @Setter
    private String currentInventoryPath;

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

    public void addProcessor(Processor processor) {
        this.processors.add(processor);
    }

    public Workspace.AssetPath getStageDirForAsset(Stage stage) {
        return workspace.getStageDirForAsset(asset, stage);
    }
}
