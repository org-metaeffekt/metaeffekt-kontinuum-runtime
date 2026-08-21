package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessorDefinitions {
    List<Processor> processors;

    @Data
    public abstract static class Processor {
        String id;
        String name;
        String stage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MavenProcessor extends Processor {
        String description;
        String pomLocation;
        String goal;
        List<ProcessorParameter> parameters;
        String preScript;
        String postScript;

        public MavenProcessor copy() {
            MavenProcessor copy = new MavenProcessor();
            copy.setId(this.getId());
            copy.setName(this.getName());
            copy.setStage(this.getStage());
            copy.setDescription(this.getDescription());
            copy.setPomLocation(this.getPomLocation());
            copy.setGoal(this.getGoal());
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

        // TODO: Implement this instead of having logic in the gitlab pipeline
        public String buildMavenCall() {
            StringBuilder mavenCall = new StringBuilder();
            mavenCall.append("mvn ")
                    .append("-f ").append(pomLocation).append(" ")
                    .append(goal).append(" ");

            for (ProcessorParameter parameter : parameters) {
                if (StringUtils.isNotBlank(parameter.value)) {
                    mavenCall.append("-D").append(parameter.getKey()).append("=")
                            .append("'").append(parameter.getValue()).append("' ");
                }
            }

            return mavenCall.toString();
        }
    }

    @Data
    public static class StandaloneProcessor extends Processor {
        String script;

        public StandaloneProcessor(String id, String name, String stage) {
            super.id = id;
            this.name = name;
            this.stage = stage;
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
