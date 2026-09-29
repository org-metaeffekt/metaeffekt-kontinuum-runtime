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

    /**
     * Creates an asset execution context backed by a new processor execution plan.
     *
     * @param asset            the asset this context belongs to
     * @param configuration    the pipeline configuration
     * @param environment      the environment configuration
     * @param workspace        the workspace used to resolve output directories
     * @param processorCatalog the catalog of available processor definitions
     */
    public AssetExecutionContext(Asset asset,
                                 PipelineConfiguration configuration,
                                 EnvironmentConfiguration environment,
                                 Workspace workspace,
                                 ProcessorCatalog processorCatalog) {
        this(new ProcessorExecutionPlan(), asset, configuration, environment, workspace, processorCatalog);
    }

    /**
     * Creates an asset execution context using the given processor execution plan.
     *
     * @param executionPlan    the processor execution plan that tracks prerequisites
     * @param asset            the asset this context belongs to
     * @param configuration    the pipeline configuration
     * @param environment      the environment configuration
     * @param workspace        the workspace used to resolve output directories
     * @param processorCatalog the catalog of available processor definitions
     */
    public AssetExecutionContext(ProcessorExecutionPlan executionPlan,
                                 Asset asset,
                                 PipelineConfiguration configuration,
                                 EnvironmentConfiguration environment,
                                 Workspace workspace,
                                 ProcessorCatalog processorCatalog) {
        super(executionPlan);
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


    /**
     * Returns the stage output directory for the current asset.
     *
     * @param stage the pipeline stage
     * @return the stage directory for the asset
     */
    public Workspace.AssetPath getStageDirForAsset(Stage stage) {
        return workspace.getStageDirForAsset(asset, stage);
    }

    /**
     * Returns the grouped directory for the current asset and report.
     *
     * @param report     the pipeline report
     * @param reportType the kind of report
     * @param locale     the locale
     * @return the grouped directory for the asset
     */
    public Workspace.AssetPath getGroupedStage(PipelineConfiguration.Report report, ReportType reportType,
                                               SupportedLocale locale) {
        return workspace.getGroupedDir(report, asset, reportType, locale);
    }

    /**
     * Returns the root asset that the current asset belongs to.
     *
     * @return the root asset, or the current asset when none is configured
     */
    public Asset getRootAsset() {
        if (configuration == null || configuration.getProjectProperties() == null) {
            return asset;
        }
        return configuration.getProjectProperties().getRootAssetFor(asset);
    }
}
