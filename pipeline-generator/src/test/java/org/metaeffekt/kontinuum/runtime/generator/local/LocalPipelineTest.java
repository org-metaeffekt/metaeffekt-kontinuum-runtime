package org.metaeffekt.kontinuum.runtime.generator.local;

import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineExecution;
import org.metaeffekt.kontinuum.runtime.models.local.LocalConfiguration;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.ProcessorDefinitions.MavenProcessor;
import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalPipelineTest {

    @Test
    void generatedScriptHonorsDependenciesEvenWhenTheyOverrideStageOrder() {
        PipelineExecution execution = new PipelineExecution();
        PipelineExecutionContext context = new PipelineExecutionContext(execution, null, null, null);
        MavenProcessor target = processor("target", Stage.PRE);
        MavenProcessor prerequisite = processor("prerequisite", Stage.REPORT);
        context.addProcessor(target);
        context.addProcessor(prerequisite);
        context.addDependency(target, prerequisite);

        LocalConfiguration configuration = LocalConfiguration.builder()
                .KONTINUUM_DIR("kontinuum")
                .build();
        String script = new LocalPipeline(execution, configuration).generatePipeline();

        int prerequisiteIndex = script.indexOf("# --- REPORT: prerequisite");
        int targetIndex = script.indexOf("# --- PRE: target");
        assertTrue(prerequisiteIndex >= 0, script);
        assertTrue(targetIndex >= 0, script);
        assertTrue(prerequisiteIndex < targetIndex, script);
    }

    private static MavenProcessor processor(String id, Stage stage) {
        MavenProcessor processor = new MavenProcessor(id, id + ".xml");
        processor.setId(id);
        processor.setName(id);
        processor.setStage(stage);
        processor.setParameters(List.of());
        return processor;
    }
}
