package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessorDefinitions {
    List<Processor> processors;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class Processor {
        @NonNull
        String id;
        @NonNull
        String name;

        Stage stage;

        String preScript;
        String postScript;
        List<ProcessorParameter> parameters;

        public abstract Processor copy();

        public void setProcessorParameter(ProcessorParameterKey key, String value) {
            if (parameters == null || parameters.stream().noneMatch(p -> p.getKey() == key)) {
                throw new IllegalStateException("The key " + key + " for processor " + id + " required during pipeline " +
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

        public String getPreScript(int indent) {
            return indentScript(preScript, indent);
        }

        public String getPostScript(int indent) {
            return indentScript(postScript, indent);
        }

        protected static String indentScript(String script, int indent) {
            if (script == null) return null;
            String padding = " ".repeat(indent);
            return padding + script.replace("\n", "\n" + padding);
        }
    }

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

        String description;
        String profile;

        public MavenProcessor(String description, String pomLocation) {
            this.description = description;
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
            copy.setDescription(this.getDescription());
            copy.setProfile(this.getProfile());
            return copy;
        }
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessorParameter {
        ProcessorParameterKey key;
        Boolean required;
        String value;

        public ProcessorParameter(ProcessorParameterKey key, Boolean required) {
            this.key = key;
            this.required = required;
        }

        public ProcessorParameter copy() {
            ProcessorParameter copy = new ProcessorParameter();
            copy.setKey(this.getKey());
            copy.setRequired(this.getRequired());
            copy.setValue(this.getValue());
            return copy;
        }
    }
}
