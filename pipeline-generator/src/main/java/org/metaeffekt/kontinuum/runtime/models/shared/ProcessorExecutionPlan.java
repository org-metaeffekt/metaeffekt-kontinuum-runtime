package org.metaeffekt.kontinuum.runtime.models.shared;

import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.Processor;

import java.util.*;

/**
 * The authoritative processor registry and dependency graph for one pipeline execution.
 * Execution contexts contribute processors while retaining their grouping and naming role.
 */
public class ProcessorExecutionPlan {

    private final List<Processor> processors = new ArrayList<>();
    private final Map<ExecutionContext, List<Processor>> processorsByContext = new IdentityHashMap<>();
    private final Map<Processor, ExecutionContext> contextByProcessor = new IdentityHashMap<>();
    private final Map<Processor, List<Processor>> dependencies = new IdentityHashMap<>();
    private final Map<ExecutionContext, List<Processor>> prerequisitesByContext = new IdentityHashMap<>();

    /**
     * Registers an execution context with the plan, initializing its processor and
     * prerequisite lists.
     *
     * @param context the execution context to register
     */
    public void registerContext(ExecutionContext context) {
        Objects.requireNonNull(context, "Execution context must not be null.");
        if (processorsByContext.containsKey(context)) {
            throw new IllegalStateException("Execution context '" + context.getName() + "' is already registered.");
        }
        processorsByContext.put(context, new ArrayList<>());
        prerequisitesByContext.put(context, new ArrayList<>());
    }

    /**
     * Adds a processor under a registered execution context.
     *
     * @param <T>       the processor type
     * @param context   the execution context owning the processor
     * @param processor the processor to add
     * @return the added processor, or null if the processor was null
     */
    public <T extends Processor> T addProcessor(ExecutionContext context, T processor) {
        if (processor == null) {
            return null;
        }
        List<Processor> contextProcessors = processorsByContext.get(context);
        if (contextProcessors == null) {
            throw new IllegalStateException("Execution context '" + contextName(context) + "' is not registered.");
        }
        ExecutionContext previousContext = contextByProcessor.get(processor);
        if (previousContext != null) {
            throw new IllegalStateException(
                    "Processor '" + processor.getId() + "' is already registered in execution context '" +
                    previousContext.getName() + "'.");
        }

        processors.add(processor);
        contextProcessors.add(processor);
        contextByProcessor.put(processor, context);
        return processor;
    }

    /**
     * Returns the processors registered for the given execution context.
     *
     * @param context the execution context to query
     * @return the context's processors, or an empty list if the context is unregistered
     */
    public List<Processor> getProcessors(ExecutionContext context) {
        List<Processor> contextProcessors = processorsByContext.get(context);
        return contextProcessors == null ? List.of() : Collections.unmodifiableList(contextProcessors);
    }

    public List<Processor> getAllProcessors() {
        return Collections.unmodifiableList(processors);
    }

    /**
     * Returns the execution context a processor was registered under.
     *
     * @param processor the processor to look up
     * @return the owning execution context, or null if the processor is unknown
     */
    public ExecutionContext getContext(Processor processor) {
        return contextByProcessor.get(processor);
    }

    /**
     * Records that a target processor depends on the given processors.
     *
     * @param target    the processor that depends on others
     * @param dependsOn the processors the target depends on
     */
    public void addDependency(Processor target, Processor... dependsOn) {
        if (target == null || dependsOn == null) {
            return;
        }
        List<Processor> targetDependencies = dependencies.computeIfAbsent(target, ignored -> new ArrayList<>());
        for (Processor dependency : dependsOn) {
            if (dependency != null && !containsIdentity(targetDependencies, dependency)) {
                targetDependencies.add(dependency);
            }
        }
    }

    /**
     * Returns the direct dependencies recorded for a processor.
     *
     * @param processor the processor to query
     * @return the processor's dependencies, or an empty set if it has none
     */
    public Set<Processor> getDependencies(Processor processor) {
        List<Processor> processorDependencies = dependencies.get(processor);
        if (processorDependencies == null || processorDependencies.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(processorDependencies));
    }

    /**
     * Records prerequisites for a report-group context before its report processors are created.
     * The prerequisite facts stay in the shared plan until the report handler connects them to
     * their generated processor nodes.
     *
     * @param context      the report-group execution context receiving the prerequisite
     * @param prerequisite the prerequisite processor to record
     */
    public void addPrerequisite(ExecutionContext context, Processor prerequisite) {
        if (prerequisite == null) {
            return;
        }
        List<Processor> contextPrerequisites = prerequisitesByContext.get(context);
        if (contextPrerequisites == null) {
            throw new IllegalStateException("Execution context '" + contextName(context) + "' is not registered.");
        }
        if (!containsIdentity(contextPrerequisites, prerequisite)) {
            contextPrerequisites.add(prerequisite);
        }
    }

    /**
     * Returns the prerequisites recorded for the given execution context.
     *
     * @param context the execution context to query
     * @return the context's prerequisites, or an empty list if it has none
     */
    public List<Processor> getPrerequisites(ExecutionContext context) {
        List<Processor> contextPrerequisites = prerequisitesByContext.get(context);
        return contextPrerequisites == null ? List.of() : Collections.unmodifiableList(contextPrerequisites);
    }

    /**
     * Rejects missing graph nodes and dependency cycles.
     */
    public void validate() {
        getOrderedProcessors();
    }

    /**
     * Returns a deterministic serial order that honors every dependency. Stage order and then
     * registration order choose among processors that are currently unconstrained.
     *
     * @return the processors in a valid, deterministic execution order
     */
    public List<Processor> getOrderedProcessors() {
        validateRegisteredDependencies();

        Map<Processor, Integer> registrationOrder = new IdentityHashMap<>();
        Map<Processor, Integer> remainingDependencies = new IdentityHashMap<>();
        Map<Processor, List<Processor>> dependents = new IdentityHashMap<>();
        for (int index = 0; index < processors.size(); index++) {
            Processor processor = processors.get(index);
            registrationOrder.put(processor, index);
            List<Processor> processorDependencies = dependencies.getOrDefault(processor, List.of());
            remainingDependencies.put(processor, processorDependencies.size());
            for (Processor prerequisite : processorDependencies) {
                dependents.computeIfAbsent(prerequisite, ignored -> new ArrayList<>()).add(processor);
            }
        }

        Comparator<Processor> priority = Comparator
                .comparingInt(this::stageOrder)
                .thenComparingInt(registrationOrder::get);
        PriorityQueue<Processor> ready = new PriorityQueue<>(priority);
        for (Processor processor : processors) {
            if (remainingDependencies.get(processor) == 0) {
                ready.add(processor);
            }
        }

        List<Processor> ordered = new ArrayList<>(processors.size());
        while (!ready.isEmpty()) {
            Processor processor = ready.remove();
            ordered.add(processor);
            for (Processor dependent : dependents.getOrDefault(processor, List.of())) {
                int remaining = remainingDependencies.compute(dependent, (ignored, count) -> count - 1);
                if (remaining == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (ordered.size() != processors.size()) {
            throw new IllegalStateException("Processor dependency cycle detected: " + describeCycle());
        }
        return Collections.unmodifiableList(ordered);
    }

    private void validateRegisteredDependencies() {
        for (Map.Entry<Processor, List<Processor>> entry : dependencies.entrySet()) {
            Processor target = entry.getKey();
            ExecutionContext targetContext = contextByProcessor.get(target);
            if (targetContext == null) {
                throw new IllegalStateException("Dependency target '" + processorName(target) +
                                                "' is not registered in the execution plan.");
            }
            for (Processor prerequisite : entry.getValue()) {
                if (!contextByProcessor.containsKey(prerequisite)) {
                    throw new IllegalStateException("Processor '" + describe(target) + "' in execution context '" +
                                                    targetContext.getName() + "' depends on unregistered " +
                                                    "processor '" + processorName(
                            prerequisite) + "'.");
                }
            }
        }
        for (Map.Entry<ExecutionContext, List<Processor>> entry : prerequisitesByContext.entrySet()) {
            for (Processor prerequisite : entry.getValue()) {
                if (!contextByProcessor.containsKey(prerequisite)) {
                    throw new IllegalStateException("Execution context '" + entry.getKey().getName() +
                                                    "' has unregistered prerequisite processor '" +
                                                    processorName(
                                                            prerequisite) + "'.");
                }
            }
        }
    }

    private String describeCycle() {
        Map<Processor, Integer> state = new IdentityHashMap<>();
        List<Processor> stack = new ArrayList<>();
        for (Processor processor : processors) {
            List<Processor> cycle = findCycle(processor, state, stack);
            if (cycle != null) {
                return String.join(" -> ", cycle.stream().map(this::describe).toList());
            }
        }
        return "unable to resolve cycle path";
    }

    private List<Processor> findCycle(Processor processor,
                                      Map<Processor, Integer> state,
                                      List<Processor> stack) {
        Integer currentState = state.get(processor);
        if (currentState != null) {
            return null;
        }
        state.put(processor, 1);
        stack.add(processor);
        for (Processor prerequisite : dependencies.getOrDefault(processor, List.of())) {
            Integer prerequisiteState = state.get(prerequisite);
            if (prerequisiteState != null && prerequisiteState == 1) {
                int cycleStart = indexOfIdentity(stack, prerequisite);
                List<Processor> cycle = new ArrayList<>(stack.subList(cycleStart, stack.size()));
                cycle.add(prerequisite);
                return cycle;
            }
            if (prerequisiteState == null) {
                List<Processor> cycle = findCycle(prerequisite, state, stack);
                if (cycle != null) {
                    return cycle;
                }
            }
        }
        stack.remove(stack.size() - 1);
        state.put(processor, 2);
        return null;
    }

    private int stageOrder(Processor processor) {
        return processor.getStage() == null ? Integer.MAX_VALUE : processor.getStage().ordinal();
    }

    private String describe(Processor processor) {
        ExecutionContext context = contextByProcessor.get(processor);
        return (context == null ? "unknown-context" : context.getName()) + "/" + processorName(processor) +
               "[" + (processor.getStage() == null ? "unknown-stage" : processor.getStage().name()) + "]";
    }

    private String processorName(Processor processor) {
        return processor == null || processor.getId() == null ? "unknown-processor" : processor.getId();
    }

    private String contextName(ExecutionContext context) {
        return context == null ? "unknown-context" : context.getName();
    }

    private boolean containsIdentity(List<Processor> processors, Processor candidate) {
        for (Processor processor : processors) {
            if (processor == candidate) {
                return true;
            }
        }
        return false;
    }

    private int indexOfIdentity(List<Processor> processors, Processor candidate) {
        for (int index = 0; index < processors.size(); index++) {
            if (processors.get(index) == candidate) {
                return index;
            }
        }
        return -1;
    }
}
