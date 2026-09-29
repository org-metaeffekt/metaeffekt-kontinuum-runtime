package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Getter;

/**
 * Execution context for pipeline-wide, asset-independent work. Handlers bound to this context run
 * exactly once per generated pipeline (e.g. downloading the vulnerability index).
 */
@Getter
public class PipelineExecutionContext extends AbstractExecutionContext {

    private final PipelineConfiguration configuration;
    private final EnvironmentConfiguration environment;
    private final ProcessorCatalog processorCatalog;

    /**
     * Creates a pipeline execution context backed by a new processor execution plan.
     *
     * @param configuration    the pipeline configuration
     * @param environment      the environment configuration
     * @param processorCatalog the catalog of available processor definitions
     */
    public PipelineExecutionContext(PipelineConfiguration configuration,
                                    EnvironmentConfiguration environment,
                                    ProcessorCatalog processorCatalog) {
        this(new ProcessorExecutionPlan(), configuration, environment, processorCatalog);
    }

    /**
     * Creates a pipeline execution context using the given processor execution plan.
     *
     * @param executionPlan    the processor execution plan that tracks prerequisites
     * @param configuration    the pipeline configuration
     * @param environment      the environment configuration
     * @param processorCatalog the catalog of available processor definitions
     */
    public PipelineExecutionContext(ProcessorExecutionPlan executionPlan,
                                    PipelineConfiguration configuration,
                                    EnvironmentConfiguration environment,
                                    ProcessorCatalog processorCatalog) {
        super(executionPlan);
        this.configuration = configuration;
        this.environment = environment;
        this.processorCatalog = processorCatalog;
    }

    @Override
    public String getName() {
        return "pipeline";
    }
}
