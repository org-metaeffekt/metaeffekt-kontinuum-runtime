package org.metaeffekt.kontinuum.runtime.models.shared;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessorDefinitionsTest {

    @Test
    public void testMavenProcessorCopy() {
        List<ProcessorDefinitions.ProcessorParameter> params = new ArrayList<>();
        params.add(new ProcessorDefinitions.ProcessorParameter(ProcessorParameterKey.PARAM_FAIL_ON_ERROR, true, "true"));
        params.add(new ProcessorDefinitions.ProcessorParameter(ProcessorParameterKey.PARAM_FAIL_ON_ISSUE, false, "false"));

        ProcessorDefinitions.MavenProcessor original = ProcessorDefinitions.MavenProcessor.builder()
                .id("test-proc")
                .name("Test Processor")
                .stage(Stage.REPORT)
                .preScript("echo 'pre'")
                .postScript("echo 'post'")
                .parameters(params)
                .lifecyclePhase("process-resources")
                .pomLocation("pom.xml")
                .description("Test Description")
                .profile("dev")
                .build();

        ProcessorDefinitions.MavenProcessor copied = original.copy();

        assertNotNull(copied);
        assertNotSame(original, copied);
        assertEquals(original.getId(), copied.getId());
        assertEquals(original.getName(), copied.getName());
        assertEquals(original.getStage(), copied.getStage());
        assertEquals(original.getPreScript(), copied.getPreScript());
        assertEquals(original.getPostScript(), copied.getPostScript());
        assertEquals(original.getLifecyclePhase(), copied.getLifecyclePhase());
        assertEquals(original.getPomLocation(), copied.getPomLocation());
        assertEquals(original.getDescription(), copied.getDescription());
        assertEquals(original.getProfile(), copied.getProfile());

        assertNotNull(copied.getParameters());
        assertNotSame(original.getParameters(), copied.getParameters());
        assertEquals(original.getParameters().size(), copied.getParameters().size());

        for (int i = 0; i < original.getParameters().size(); i++) {
            ProcessorDefinitions.ProcessorParameter origParam = original.getParameters().get(i);
            ProcessorDefinitions.ProcessorParameter copiedParam = copied.getParameters().get(i);
            assertNotSame(origParam, copiedParam);
            assertEquals(origParam.getKey(), copiedParam.getKey());
            assertEquals(origParam.getRequired(), copiedParam.getRequired());
            assertEquals(origParam.getValue(), copiedParam.getValue());
        }

        // Mutating copy does not mutate original
        copied.setProcessorParameter(ProcessorParameterKey.PARAM_FAIL_ON_ERROR, "false");
        assertEquals("false", copied.getParameters().get(0).getValue());
        assertEquals("true", original.getParameters().get(0).getValue());
    }

    @Test
    public void testStandaloneProcessorCopy() {
        List<ProcessorDefinitions.ProcessorParameter> params = new ArrayList<>();
        params.add(new ProcessorDefinitions.ProcessorParameter(ProcessorParameterKey.PARAM_FAIL_ON_ERROR, true, "true"));

        ProcessorDefinitions.StandaloneProcessor original = ProcessorDefinitions.StandaloneProcessor.builder()
                .id("standalone-proc")
                .name("Standalone")
                .stage(Stage.PREPARE)
                .preScript("echo 'pre'")
                .postScript("echo 'post'")
                .parameters(params)
                .scriptLocation("scripts/run.sh")
                .build();

        ProcessorDefinitions.StandaloneProcessor copied = original.copy();

        assertNotNull(copied);
        assertNotSame(original, copied);
        assertEquals(original.getId(), copied.getId());
        assertEquals(original.getScriptLocation(), copied.getScriptLocation());
        assertNotSame(original.getParameters(), copied.getParameters());
        assertNotSame(original.getParameters().get(0), copied.getParameters().get(0));
    }

    @Test
    public void testContextAddDependency() {
        AssetExecutionContext context = new AssetExecutionContext(
                null, null, null, null, null
        );

        ProcessorDefinitions.MavenProcessor p1 = new ProcessorDefinitions.MavenProcessor("p1", "pom1.xml");
        p1.setId("p1");
        p1.setName("P1");

        ProcessorDefinitions.MavenProcessor p2 = new ProcessorDefinitions.MavenProcessor("p2", "pom2.xml");
        p2.setId("p2");
        p2.setName("P2");

        ProcessorDefinitions.MavenProcessor p3 = new ProcessorDefinitions.MavenProcessor("p3", "pom3.xml");
        p3.setId("p3");
        p3.setName("P3");

        context.addProcessor(p1);
        context.addProcessor(p2);
        context.addProcessor(p3);

        context.addDependency(p3, p1, p2);
        assertEquals(2, context.getDependencies(p3).size());
        assertTrue(context.getDependencies(p3).contains(p1));
        assertTrue(context.getDependencies(p3).contains(p2));

        // Duplicate dependencies should not be added multiple times
        context.addDependency(p3, p1);
        assertEquals(2, context.getDependencies(p3).size());
    }

    @Test
    public void testContextAddSequential() {
        AssetExecutionContext context = new AssetExecutionContext(
                null, null, null, null, null
        );

        ProcessorDefinitions.MavenProcessor p1 = new ProcessorDefinitions.MavenProcessor("p1", "pom1.xml");
        p1.setId("p1");
        p1.setName("P1");

        ProcessorDefinitions.MavenProcessor p2 = new ProcessorDefinitions.MavenProcessor("p2", "pom2.xml");
        p2.setId("p2");
        p2.setName("P2");

        ProcessorDefinitions.MavenProcessor p3 = new ProcessorDefinitions.MavenProcessor("p3", "pom3.xml");
        p3.setId("p3");
        p3.setName("P3");

        context.addSequential(p1, p2, p3);

        assertEquals(3, context.getProcessors().size());
        assertTrue(context.getDependencies(p1).isEmpty());
        assertEquals(1, context.getDependencies(p2).size());
        assertTrue(context.getDependencies(p2).contains(p1));
        assertEquals(1, context.getDependencies(p3).size());
        assertTrue(context.getDependencies(p3).contains(p2));
        assertEquals(p3, context.getLastProcessor());
    }

    @Test
    public void testSetProcessorParameter() {
        List<ProcessorDefinitions.ProcessorParameter> params = new ArrayList<>();
        params.add(new ProcessorDefinitions.ProcessorParameter(ProcessorParameterKey.PARAM_FAIL_ON_ERROR, true, null));
        params.add(new ProcessorDefinitions.ProcessorParameter(ProcessorParameterKey.PARAM_FAIL_ON_ISSUE, false, null));

        ProcessorDefinitions.MavenProcessor processor = new ProcessorDefinitions.MavenProcessor("desc", "pom.xml");
        processor.setId("proc");
        processor.setName("Proc");
        processor.setParameters(params);

        // Required param with value
        processor.setProcessorParameter(ProcessorParameterKey.PARAM_FAIL_ON_ERROR, "true");
        assertEquals("true", params.get(0).getValue());

        // Optional param with null value should keep current value (does not overwrite)
        processor.setProcessorParameter(ProcessorParameterKey.PARAM_FAIL_ON_ISSUE, null);
        assertNull(params.get(1).getValue());

        // Setting non-existent key throws exception
        assertThrows(IllegalStateException.class, () ->
                processor.setProcessorParameter(ProcessorParameterKey.ENV_MIRROR_DIR, "dir")
        );
    }

    @Test
    public void testScriptIndentation() {
        ProcessorDefinitions.MavenProcessor processor = new ProcessorDefinitions.MavenProcessor();
        processor.setPreScript("line1\nline2");
        assertEquals("  line1\n  line2", processor.getPreScript(2));
        assertNull(processor.getPostScript(2));
    }
}
