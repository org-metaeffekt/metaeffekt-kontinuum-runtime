package org.metaeffekt.kontinuum.runtime.models.gitlab;

import java.util.Map;
import java.util.TreeMap;

public class IncludedComponent {

    public enum IncludeScope {
        LOCAL("local"),
        PROJECT("project"),
        REMOTE("remote"),
        TEMPLATE("template"),
        COMPONENT("component");

        private final String yamlKey;

        IncludeScope(String yamlKey) {
            this.yamlKey = yamlKey;
        }

        public String getYamlKey() {
            return yamlKey;
        }
    }

    IncludeScope includeScope;
    String resourceReference;
    Map<String, Object> inputs = new TreeMap<>();

    public IncludedComponent(IncludeScope includeScope, String resourceReference) {
        this.includeScope = includeScope;
        this.resourceReference = resourceReference;
    }

    public void addInput(String key, String value) {
        inputs.put(key, value);
    }

    public void addInput(String key, Boolean value) {
        inputs.put(key, value);
    }

    public String toYaml(int indent) {
        String pad = " ".repeat(indent);
        String pad2 = " ".repeat(indent + 2);
        String pad4 = " ".repeat(indent + 4);

        StringBuilder sb = new StringBuilder();
        sb.append(pad).append("- ").append(includeScope.getYamlKey()).append(": '").append(resourceReference).append("'");

        if (!inputs.isEmpty()) {
            sb.append(System.lineSeparator()).append(pad2).append("inputs:");
            for (Map.Entry<String, Object> entry : inputs.entrySet()) {
                sb.append(System.lineSeparator())
                        .append(pad4)
                        .append(entry.getKey())
                        .append(": ");
                if (entry.getValue() instanceof String) {
                    sb.append("'").append(entry.getValue()).append("'");
                } else if (entry.getValue() instanceof Boolean) {
                    sb.append(entry.getValue());
                }
            }
        }
        sb.append(System.lineSeparator());

        return sb.append(System.lineSeparator()).toString();
    }
}
