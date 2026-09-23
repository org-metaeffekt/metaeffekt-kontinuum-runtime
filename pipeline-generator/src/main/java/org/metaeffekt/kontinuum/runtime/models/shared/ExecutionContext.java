package org.metaeffekt.kontinuum.runtime.models.shared;

import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.List;
import java.util.Set;

/**
 * A named unit of pipeline execution that accumulates processors and their dependency graph.
 * <p>
 * Handlers are invoked at different scopes: a pipeline-wide context runs once, an
 * {@link AssetExecutionContext} once per asset, and a {@link ReportGroupExecutionContext}
 * once per report group. This interface gives generators a uniform view over all of them.
 */
public interface ExecutionContext {

    /**
     * A stable, human readable name used for job naming and diagnostics.
     */
    String getName();

    /**
     * The processors accumulated by the handlers in insertion order.
     */
    List<Processor> getProcessors();

    /**
     * The processors the given processor depends on.
     */
    Set<Processor> getDependencies(Processor processor);

    /**
     * Registers a processor with this context.
     */
    <T extends Processor> T addProcessor(T processor);

    /**
     * Registers directed dependencies between tasks for branching (fan-out) or merging (fan-in).
     */
    void addDependency(Processor target, Processor... dependsOn);

    /**
     * Convenience helper for linear stages. Registers each processor and automatically
     * creates predecessor dependencies between them.
     */
    void addSequential(Processor... processors);

    /**
     * The most recently registered processor or null if none was registered yet.
     */
    Processor getLastProcessor();

    /**
     * The most recently registered processor belonging to the given stage.
     */
    Processor getLastProcessor(Stage stage);
}
