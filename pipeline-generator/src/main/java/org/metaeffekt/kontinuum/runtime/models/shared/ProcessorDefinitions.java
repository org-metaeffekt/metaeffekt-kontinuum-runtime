package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The processor catalog: the list of processor definitions available to a pipeline.
 */
@Data
public class ProcessorDefinitions {
    List<Processor> processors;

    /**
     * Base definition of a processor: identity, stage, optional scripts, and parameters.
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class Processor {
        @Builder.Default
        UUID uid = UUID.randomUUID();
        @NonNull
        String id;
        @NonNull
        String name;

        Stage stage;
        String preScript;
        String postScript;
        List<ProcessorParameter> parameters;

        /**
         * Creates a deep copy of this processor definition.
         *
         * @return a copy of this processor
         */
        public abstract Processor copy();

        /**
         * Sets the value of an existing parameter, leaving optional parameters untouched when
         * a null value is supplied.
         *
         * @param key   the parameter key to update
         * @param value the new value, or null to clear it
         */
        public void setProcessorParameter(ProcessorParameterKey key, String value) {
            if (parameters == null || parameters.stream().noneMatch(p -> p.getKey() == key)) {
                throw new IllegalStateException(
                        "The key " + key + " for processor " + id + " required during pipeline " +
                        "creation does not exist in the processor definition.");
            }

            for (ProcessorParameter processorParameter : parameters) {
                if (processorParameter.getKey() == key) {
                    if (value == null && Boolean.FALSE.equals(processorParameter.getRequired())) {
                        return;
                    }
                    processorParameter.setValue(value);
                }
            }
        }

        /**
         * Returns the pre-script indented by the given number of spaces.
         *
         * @param indent the number of spaces to indent each line
         * @return the indented pre-script, or null if none is configured
         */
        public String getPreScript(int indent) {
            return indentScript(preScript, indent);
        }

        /**
         * Returns the post-script indented by the given number of spaces.
         *
         * @param indent the number of spaces to indent each line
         * @return the indented post-script, or null if none is configured
         */
        public String getPostScript(int indent) {
            return indentScript(postScript, indent);
        }

        protected static String indentScript(String script, int indent) {
            if (script == null) {
                return null;
            }
            String padding = " ".repeat(indent);
            return padding + script.replace("\n", "\n" + padding);
        }
    }

    /**
     * A processor that runs a standalone script located at a configured path.
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class StandaloneProcessor extends Processor {
        @NonNull
        String scriptLocation;

        @Override
        public StandaloneProcessor copy() {
            StandaloneProcessor copy = new StandaloneProcessor();
            copy.setId(this.getId());
            copy.setName(this.getName());
            copy.setStage(this.getStage());
            copy.setPreScript(this.getPreScript());
            copy.setPostScript(this.getPostScript());
            if (this.getParameters() != null) {
                List<ProcessorParameter> copiedParams = new ArrayList<>(this.getParameters().size());
                for (ProcessorParameter parameter : this.getParameters()) {
                    copiedParams.add(parameter != null ? parameter.copy() : null);
                }
                copy.setParameters(copiedParams);
            }
            copy.setScriptLocation(this.getScriptLocation());
            return copy;
        }
    }

    /**
     * A processor that invokes a Maven lifecycle phase on a configured POM.
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class MavenProcessor extends Processor {
        @Builder.Default
        String lifecyclePhase = "process-resources";

        @NonNull
        String pomLocation;

        String profile;

        /**
         * Creates a Maven processor for the given POM location.
         *
         * @param pomLocation the path to the POM file to invoke
         */
        public MavenProcessor(String pomLocation) {
            this.pomLocation = pomLocation;
        }

        @Override
        public MavenProcessor copy() {
            MavenProcessor copy = new MavenProcessor();
            copy.setId(this.getId());
            copy.setName(this.getName());
            copy.setStage(this.getStage());
            copy.setPreScript(this.getPreScript());
            copy.setPostScript(this.getPostScript());
            if (this.getParameters() != null) {
                List<ProcessorParameter> copiedParams = new ArrayList<>(this.getParameters().size());
                for (ProcessorParameter parameter : this.getParameters()) {
                    copiedParams.add(parameter != null ? parameter.copy() : null);
                }
                copy.setParameters(copiedParams);
            }
            copy.setLifecyclePhase(this.getLifecyclePhase());
            copy.setPomLocation(this.getPomLocation());
            copy.setProfile(this.getProfile());
            return copy;
        }
    }

    /**
     * A single configurable processor parameter with its key, required flag, and value.
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessorParameter {
        ProcessorParameterKey key;
        Boolean required;
        String value;

        /**
         * Creates a processor parameter for the given key with its required flag.
         *
         * @param key      the parameter key
         * @param required whether the parameter is required
         */
        public ProcessorParameter(ProcessorParameterKey key, Boolean required) {
            this.key = key;
            this.required = required;
        }

        /**
         * Creates a copy of this processor parameter.
         *
         * @return a copy of this parameter
         */
        public ProcessorParameter copy() {
            ProcessorParameter copy = new ProcessorParameter();
            copy.setKey(this.getKey());
            copy.setRequired(this.getRequired());
            copy.setValue(this.getValue());
            return copy;
        }
    }
}
