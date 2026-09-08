package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration.ProjectProperties.Asset;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.*;

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
    private final Map<Processor, Set<Processor>> dependencies = new IdentityHashMap<>();

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

    public <T extends Processor> T addProcessor(T processor) {
        if (processor != null) {
            this.processors.add(processor);
        }
        return processor;
    }

    public void addDependency(Processor target, Processor... dependsOn) {
        if (target != null && dependsOn != null) {
            Set<Processor> deps = this.dependencies.computeIfAbsent(target, k -> new LinkedHashSet<>());
            for (Processor dep : dependsOn) {
                if (dep != null && dep != target) {
                    deps.add(dep);
                }
            }
        }
    }

    public void addDependency(Processor target, Collection<? extends Processor> dependsOn) {
        if (target != null && dependsOn != null) {
            Set<Processor> deps = this.dependencies.computeIfAbsent(target, k -> new LinkedHashSet<>());
            for (Processor dep : dependsOn) {
                if (dep != null && dep != target) {
                    deps.add(dep);
                }
            }
        }
    }

    public void addSequential(Processor... processors) {
        if (processors == null) return;
        Processor prev = null;
        for (Processor p : processors) {
            if (p != null) {
                addProcessor(p);
                if (prev != null) {
                    addDependency(p, prev);
                }
                prev = p;
            }
        }
    }

    public void addSequential(List<? extends Processor> processors) {
        if (processors == null) return;
        Processor prev = null;
        for (Processor p : processors) {
            if (p != null) {
                addProcessor(p);
                if (prev != null) {
                    addDependency(p, prev);
                }
                prev = p;
            }
        }
    }

    public Set<Processor> getDependencies(Processor processor) {
        return this.dependencies.getOrDefault(processor, Collections.emptySet());
    }

    public Processor getLastProcessor() {
        return processors.isEmpty() ? null : processors.get(processors.size() - 1);
    }

    public Processor getLastProcessor(Stage stage) {
        for (int i = processors.size() - 1; i >= 0; i--) {
            if (processors.get(i).getStage() == stage) {
                return processors.get(i);
            }
        }
        return null;
    }

    public Workspace.AssetPath getStageDirForAsset(Stage stage) {
        return workspace.getStageDirForAsset(asset, stage);
    }

    public Workspace.AssetPath getGroupedStageForAsset(ReportType reportType, SupportedLocale locale) {
        return workspace.getGroupedDirForAsset(asset, reportType, locale);
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
