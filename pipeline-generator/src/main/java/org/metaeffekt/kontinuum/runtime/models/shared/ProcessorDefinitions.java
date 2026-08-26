package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessorDefinitions {
    List<Processor> processors;

    @Data
    public abstract static class Processor {
        String id;
        String name;
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StandaloneProcessor extends Processor {
        String scriptLocation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MavenProcessor extends Processor {
        String description;
        String pomLocation;

        public MavenProcessor copy() {
            MavenProcessor copy = new MavenProcessor();
            copy.setId(this.getId());
            copy.setName(this.getName());
            copy.setStage(this.getStage());
            copy.setDescription(this.getDescription());
            copy.setPomLocation(this.getPomLocation());
            copy.setPreScript(this.getPreScript());
            copy.setPostScript(this.getPostScript());
            if (this.getParameters() != null) {
                List<ProcessorParameter> copiedParams = new ArrayList<>(this.getParameters().size());
                for (ProcessorParameter parameter : this.getParameters()) {
                    copiedParams.add(parameter.copy());
                }
                copy.setParameters(copiedParams);
            }
            return copy;
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
