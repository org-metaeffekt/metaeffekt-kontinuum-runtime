package org.metaeffekt.kontinuum.runtime.models.shared;

import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.List;
import java.util.Set;

/**
 * A named grouping of processors in one pipeline execution.
 * <p>
 * Handlers are invoked at different scopes: a pipeline-wide context runs once, an
 * {@link AssetExecutionContext} once per asset, and a {@link ReportGroupExecutionContext}
 * once per report group. This interface gives generators a uniform view over all of them.
 */
public interface ExecutionContext {

    /**
     * A stable, human readable name used for job naming and diagnostics.
     *
     * @return the context name
     */
    String getName();

    /**
     * The processors registered by handlers for this context, in insertion order.
     *
     * @return the registered processors in insertion order
     */
    List<Processor> getProcessors();

    /**
     * The processors the given processor depends on.
     *
     * @param processor the processor whose dependencies are requested
     * @return the processors the given processor depends on
     */
    Set<Processor> getDependencies(Processor processor);

    /**
     * Registers a processor in the shared execution plan and associates it with this context.
     *
     * @param <T>       the processor type
     * @param processor the processor to register
     * @return the registered processor
     */
    <T extends Processor> T addProcessor(T processor);

    /**
     * Registers directed dependencies between tasks for branching (fan-out) or merging (fan-in).
     *
     * @param target    the dependent processor
     * @param dependsOn the processors the target depends on
     */
    void addDependency(Processor target, Processor... dependsOn);

    /**
     * Convenience helper for linear stages. Registers each processor and automatically
     * creates predecessor dependencies between them.
     *
     * @param processors the processors to register in sequence
     */
    void addSequential(Processor... processors);

    /**
     * The most recently registered processor or null if none was registered yet.
     *
     * @return the most recently registered processor, or null if none
     */
    Processor getLastProcessor();

    /**
     * The most recently registered processor belonging to the given stage.
     *
     * @param stage the stage to filter by
     * @return the most recently registered processor of the stage
     */
    Processor getLastProcessor(Stage stage);
}
