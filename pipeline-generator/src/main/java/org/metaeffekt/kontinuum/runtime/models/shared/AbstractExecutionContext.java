package org.metaeffekt.kontinuum.runtime.models.shared;

import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.*;

/**
 * Shared implementation of the processor and dependency bookkeeping for all execution contexts.
 */
public abstract class AbstractExecutionContext implements ExecutionContext {

    private final List<Processor> processors = new ArrayList<>();
    private final Map<Processor, Set<Processor>> dependencies = new IdentityHashMap<>();

    @Override
    public List<Processor> getProcessors() {
        return processors;
    }

    @Override
    public <T extends Processor> T addProcessor(T processor) {
        if (processor != null) {
            this.processors.add(processor);
        }
        return processor;
    }

    @Override
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
        return this.dependencies.getOrDefault(processor, Collections.emptySet());
    }

    @Override
    public Processor getLastProcessor() {
        return processors.isEmpty() ? null : processors.get(processors.size() - 1);
    }

    @Override
    public Processor getLastProcessor(Stage stage) {
        for (int i = processors.size() - 1; i >= 0; i--) {
            if (processors.get(i).getStage() == stage) {
                return processors.get(i);
            }
        }
        return null;
    }
}
