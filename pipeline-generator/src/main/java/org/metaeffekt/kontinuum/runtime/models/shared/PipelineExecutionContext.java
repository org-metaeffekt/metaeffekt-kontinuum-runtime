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

    public PipelineExecutionContext(PipelineConfiguration configuration,
                                    EnvironmentConfiguration environment,
                                    ProcessorCatalog processorCatalog) {
        this.configuration = configuration;
        this.environment = environment;
        this.processorCatalog = processorCatalog;
    }

    @Override
    public String getName() {
        return "pipeline";
    }
}
