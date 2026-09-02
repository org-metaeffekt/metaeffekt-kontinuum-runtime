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
    public abstract static class Processor {
        @NonNull
        String id;
        @NonNull
        String name;
        @NonNull
        Stage stage;

        String preScript;
        String postScript;
        List<ProcessorParameter> parameters;

        public void setProcessorParameter(ProcessorParameterKey key, String value) {
            if (parameters.stream().noneMatch(p -> p.key == key)) {
                throw new IllegalStateException("The key " + key + " for processor " + id + " required during pipeline " +
                        "creation does not exist in the processor definition.");
            }

            for (ProcessorParameter processorParameter : parameters) {
                if (processorParameter.key == key) {
                    if (value == null && !processorParameter.required) {
                        return;
                    }
                    processorParameter.setValue(value);
                }
            }
        }
    }

    @Data
    @Builder
    public static class StandaloneProcessor extends Processor {
        @NonNull
        String scriptLocation;
    }

    @Data
    @Builder
    public static class MavenProcessor extends Processor {
        @Builder.Default
        String lifecyclePhase = "process-resources";

        @NonNull
        String pomLocation;

        String description;
        String profile;

        public MavenProcessor copy() {

        }

        public String getPreScript(int indent) {
            return indentScript(preScript, indent);
        }

        public String getPostScript(int indent) {
            return indentScript(postScript, indent);
        }

        private static String indentScript(String script, int indent) {
            if (script == null) return null;
            String padding = " ".repeat(indent);
            return padding + script.replace("\n", "\n" + padding);
        }
    }

    @Data
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
