package org.metaeffekt.kontinuum.runtime.models.shared;

import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared context view over the pipeline-wide processor execution plan.
 */
public abstract class AbstractExecutionContext implements ExecutionContext {

    private final ProcessorExecutionPlan executionPlan;

    protected AbstractExecutionContext() {
        this(new ProcessorExecutionPlan());
    }

    protected AbstractExecutionContext(ProcessorExecutionPlan executionPlan) {
        this.executionPlan = Objects.requireNonNull(executionPlan, "Execution plan must not be null.");
        this.executionPlan.registerContext(this);
    }

    @Override
    public List<Processor> getProcessors() {
        return executionPlan.getProcessors(this);
    }

    @Override
    public <T extends Processor> T addProcessor(T processor) {
        return executionPlan.addProcessor(this, processor);
    }

    @Override
    public void addDependency(Processor target, Processor... dependsOn) {
        executionPlan.addDependency(target, dependsOn);
    }

    @Override
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

    @Override
    public Set<Processor> getDependencies(Processor processor) {
        return executionPlan.getDependencies(processor);
    }

    @Override
    public Processor getLastProcessor() {
        List<Processor> processors = getProcessors();
        return processors.isEmpty() ? null : processors.get(processors.size() - 1);
    }

    @Override
    public Processor getLastProcessor(Stage stage) {
        List<Processor> processors = getProcessors();
        for (int i = processors.size() - 1; i >= 0; i--) {
            if (processors.get(i).getStage() == stage) {
                return processors.get(i);
            }
        }
        return null;
    }

    protected ProcessorExecutionPlan getExecutionPlan() {
        return executionPlan;
    }
}
