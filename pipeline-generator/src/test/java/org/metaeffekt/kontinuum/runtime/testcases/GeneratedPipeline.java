package org.metaeffekt.kontinuum.runtime.testcases;

import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A generated local or GitLab pipeline. Single steps of a local pipeline are found by their header line, the maven
 * invocations of both pipelines by their {@code mvn} lines.
 */
final class GeneratedPipeline {

    /** Each step of the local pipeline starts with {@code # --- <STAGE>: <processor id> (<context>) ----}. */
    private static final String STEP_HEADER_PREFIX = "# --- ";

    private final String content;
    private final List<PipelineStep> steps;

    GeneratedPipeline(String content) {
        this.content = content;
        this.steps = splitSteps(content);
    }

    /**
     * Returns the single step of a local pipeline that runs the given processor for the given context. Fails the test
     * if there is no such step or more than one.
     *
     * @param stage       the stage of the step
     * @param processorId the id of the processor, e.g. {@code copy-inventory}
     * @param context     the asset id, the report group or {@code pipeline}
     *
     * @return the step
     */
    PipelineStep step(Stage stage, String processorId, String context) {
        assertHasSteps();
        String header = STEP_HEADER_PREFIX + stage.name() + ": " + processorId + " (" + context + ") ----";
        List<PipelineStep> matches = steps.stream().filter(step -> step.header().equals(header)).toList();
        assertEquals(1, matches.size(), "Expected exactly one step [" + header + "], steps of the pipeline:\n"
                                        + String.join("\n", steps.stream().map(PipelineStep::header).toList()));
        return matches.get(0);
    }

    /**
     * Fails the test if the local pipeline contains a step of the given stage.
     *
     * @param stage the stage that must not have steps
     */
    void assertNoSteps(Stage stage) {
        assertHasSteps();
        String stagePrefix = STEP_HEADER_PREFIX + stage.name() + ":";
        List<String> headers = steps.stream().map(PipelineStep::header)
                .filter(header -> header.startsWith(stagePrefix)).toList();
        assertTrue(headers.isEmpty(), "Expected no steps of stage " + stage + ", but found: " + headers);
    }

    /**
     * Splits the pipeline into its maven invocations. Each invocation starts with a line beginning with {@code mvn}
     * and continues as long as the previous line ends with a line continuation. The lines are trimmed and stripped of
     * the line continuation, so local and GitLab pipelines yield the same parameter lines.
     *
     * @return the lines of each maven invocation
     */
    List<List<String>> mavenInvocations() {
        List<List<String>> invocations = new ArrayList<>();
        List<String> current = null;
        boolean continued = false;

        for (String line : content.lines().toList()) {
            String trimmed = line.trim();
            if (continued && current != null) {
                current.add(stripContinuation(trimmed));
            } else if (trimmed.startsWith("mvn ")) {
                current = new ArrayList<>();
                current.add(stripContinuation(trimmed));
                invocations.add(current);
            } else {
                current = null;
            }
            continued = current != null && trimmed.endsWith("\\");
        }
        return invocations;
    }

    @Override
    public String toString() {
        return content;
    }

    /**
     * Steps are only found in a local pipeline. Without this check, a step query on a GitLab pipeline would fail with
     * an empty list of steps, and {@link #assertNoSteps(Stage)} would always pass.
     */
    private void assertHasSteps() {
        assertFalse(steps.isEmpty(), "The pipeline has no step headers. Only local pipelines are split into steps, "
                                     + "use mavenInvocations() for GitLab pipelines.");
    }

    /**
     * Splits a local pipeline into its steps. Each step starts with a header line and ends before the next header
     * line. A GitLab pipeline has no header lines and therefore no steps.
     */
    private static List<PipelineStep> splitSteps(String content) {
        List<PipelineStep> steps = new ArrayList<>();
        List<String> lines = null;
        for (String line : content.lines().toList()) {
            if (line.startsWith(STEP_HEADER_PREFIX)) {
                lines = new ArrayList<>();
                steps.add(new PipelineStep(line, lines));
            } else if (lines != null) {
                lines.add(line);
            }
        }
        return steps;
    }

    private static String stripContinuation(String line) {
        return line.endsWith("\\") ? line.substring(0, line.length() - 1).trim() : line;
    }

    /**
     * A step of a local pipeline: the header line and the lines of the processor invocation.
     */
    record PipelineStep(String header, List<String> lines) {

        /**
         * Fails the test if the step does not pass the parameter {@code -D<key>='<value>'}.
         */
        void assertParameter(String key, Object value) {
            assertContains("-D" + key + "='" + value + "'");
        }

        /**
         * Fails the test if no line of the step contains the expected text.
         */
        void assertContains(String expected) {
            assertTrue(lines.stream().anyMatch(line -> line.contains(expected)),
                    "Step [" + header + "] expected to contain [" + expected + "]:\n" + String.join("\n", lines));
        }
    }
}
