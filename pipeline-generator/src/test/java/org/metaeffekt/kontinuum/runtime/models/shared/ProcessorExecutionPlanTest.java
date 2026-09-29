package org.metaeffekt.kontinuum.runtime.models.shared;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorExecutionPlanTest {

    @Test
    void ordersDependenciesBeforeStagePriorityAndKeepsStableOrderForUnconstrainedProcessors() {
        ProcessorExecutionPlan plan = new ProcessorExecutionPlan();
        PipelineExecutionContext pipeline = new PipelineExecutionContext(plan, null, null, null);

        MavenProcessor target = processor("target", Stage.PRE);
        MavenProcessor firstFree = processor("first-free", Stage.FETCH);
        MavenProcessor secondFree = processor("second-free", Stage.FETCH);
        MavenProcessor prerequisite = processor("prerequisite", Stage.POST);

        pipeline.addProcessor(target);
        pipeline.addProcessor(firstFree);
        pipeline.addProcessor(secondFree);
        pipeline.addProcessor(prerequisite);
        pipeline.addDependency(target, prerequisite);

        assertEquals(List.of(firstFree, secondFree, prerequisite, target), plan.getOrderedProcessors());
    }

    @Test
    void rejectsDependenciesOnUnregisteredProcessorsWithContext() {
        ProcessorExecutionPlan plan = new ProcessorExecutionPlan();
        PipelineExecutionContext pipeline = new PipelineExecutionContext(plan, null, null, null);
        MavenProcessor target = processor("target", Stage.REPORT);
        MavenProcessor missing = processor("missing", Stage.GROUP);
        pipeline.addProcessor(target);
        pipeline.addDependency(target, missing);

        IllegalStateException error = assertThrows(IllegalStateException.class, plan::validate);

        assertTrue(error.getMessage().contains("target"));
        assertTrue(error.getMessage().contains("missing"));
        assertTrue(error.getMessage().contains("pipeline"));
    }

    @Test
    void rejectsCyclesAndNamesTheProcessorsInTheCycle() {
        ProcessorExecutionPlan plan = new ProcessorExecutionPlan();
        PipelineExecutionContext pipeline = new PipelineExecutionContext(plan, null, null, null);
        MavenProcessor first = processor("first", Stage.PREPARE);
        MavenProcessor second = processor("second", Stage.REPORT);
        pipeline.addProcessor(first);
        pipeline.addProcessor(second);
        pipeline.addDependency(first, second);
        pipeline.addDependency(second, first);

        IllegalStateException error = assertThrows(IllegalStateException.class, plan::validate);

        assertTrue(error.getMessage().contains("cycle"));
        assertTrue(error.getMessage().contains("first"));
        assertTrue(error.getMessage().contains("second"));
        assertTrue(error.getMessage().contains("pipeline"));
    }

    @Test
    void rejectsUnregisteredContextPrerequisites() {
        ProcessorExecutionPlan plan = new ProcessorExecutionPlan();
        PipelineExecutionContext pipeline = new PipelineExecutionContext(plan, null, null, null);
        plan.addPrerequisite(pipeline, processor("missing-prerequisite", Stage.GROUP));

        IllegalStateException error = assertThrows(IllegalStateException.class, plan::validate);

        assertTrue(error.getMessage().contains("missing-prerequisite"));
        assertTrue(error.getMessage().contains("pipeline"));
    }

    @Test
    void dependenciesAreSharedAcrossExecutionContexts() {
        ProcessorExecutionPlan plan = new ProcessorExecutionPlan();
        PipelineExecutionContext pipeline = new PipelineExecutionContext(plan, null, null, null);
        AssetExecutionContext asset = new AssetExecutionContext(plan, null, null, null, null, null);
        MavenProcessor prerequisite = processor("asset-prerequisite", Stage.GROUP);
        MavenProcessor target = processor("report", Stage.REPORT);
        asset.addProcessor(prerequisite);
        pipeline.addProcessor(target);

        pipeline.addDependency(target, prerequisite);

        assertEquals(List.of(prerequisite), List.copyOf(pipeline.getDependencies(target)));
        assertEquals(List.of(prerequisite, target), plan.getOrderedProcessors());
    }

    private static MavenProcessor processor(String id, Stage stage) {
        MavenProcessor processor = new MavenProcessor(id, id + ".xml");
        processor.setId(id);
        processor.setName(id);
        processor.setStage(stage);
        return processor;
    }
}
