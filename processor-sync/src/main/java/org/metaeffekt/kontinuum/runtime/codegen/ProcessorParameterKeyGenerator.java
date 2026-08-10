package org.metaeffekt.kontinuum.runtime.codegen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProcessorParameterKeyGenerator {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Expected exactly 2 arguments: <processors.yaml path> <output .java path>, got " + args.length);
        }
        Path yamlPath = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);

        if (!Files.exists(yamlPath)) {
            throw new IllegalStateException("processors.yaml not found at " + yamlPath.toAbsolutePath());
        }

        TreeSet<String> sortedKeys = collectDistinctKeys(yamlPath);

        String enumSource = renderEnum(sortedKeys);

        writeIfChanged(outputPath, enumSource);
        System.out.println("Generated " + outputPath + " with " + sortedKeys.size() + " parameter keys.");
    }

    private static TreeSet<String> collectDistinctKeys(Path yamlPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Catalog catalog = mapper.readValue(yamlPath.toFile(), Catalog.class);

        TreeSet<String> keys = new TreeSet<>();
        for (CatalogProcessor processor : catalog.processors) {
            if (processor.parameters == null) continue;
            for (CatalogParameter parameter : processor.parameters) {
                if (parameter.key == null || parameter.key.isBlank()) {
                    throw new IllegalStateException("Encountered blank/null parameter key in processor ["
                            + processor.id + "] while generating ProcessorParameterKey.");
                }
                keys.add(parameter.key);
            }
        }
        return keys;
    }

    private static String constantName(String key) {
        return key.toUpperCase().replace('.', '_').replace('-', '_');
    }

    private static String renderEnum(TreeSet<String> sortedKeys) {
        List<String> constantNames = new ArrayList<>();
        Map<String, String> nameToKey = new LinkedHashMap<>();
        for (String key : sortedKeys) {
            String name = constantName(key);
            String existing = nameToKey.put(name, key);
            if (existing != null) {
                throw new IllegalStateException("Constant name collision: keys [" + existing
                        + "] and [" + key + "] both map to " + name
                        + ". Rename one of them in processors.yaml.");
            }
            constantNames.add(name);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("// Generated from processors.yaml by ae-kontinuum-runtime-generator-codegen. Do not edit.\n");
        sb.append("package org.metaeffekt.kontinuum.runtime.models.shared;\n\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonCreator;\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonValue;\n");
        sb.append("import lombok.Getter;\n\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("/**\n");
        sb.append(" * Type-safe representation of every parameter key declared in {@code processors.yaml}.\n");
        sb.append(" * <p>\n");
        sb.append(" * This enum is <strong>generated</strong> from the processor catalog — the YAML is the\n");
        sb.append(" * single source of truth. Adding a {@code key:} entry in the catalog produces a new\n");
        sb.append(" * constant on the next build; removing one breaks compilation at every call site that\n");
        sb.append(" * still references it. Jackson decodes the YAML {@code key:} string to this enum via\n");
        sb.append(" * {@link #fromKey(String)}, so an unknown key fails catalog loading at runtime.\n");
        sb.append(" */\n");
        sb.append("public enum ProcessorParameterKey {\n\n");

        for (int i = 0; i < sortedKeys.size(); i++) {
            String key = new ArrayList<>(sortedKeys).get(i);
            String name = constantNames.get(i);
            sb.append("    ").append(name).append("(\"").append(key).append("\")");
            sb.append(i < sortedKeys.size() - 1 ? "," : ";");
            sb.append("\n");
        }

        sb.append("\n");
        sb.append("    @Getter\n");
        sb.append("    private final String key;\n\n");
        sb.append("    private static final Map<String, ProcessorParameterKey> KEY_INDEX = new HashMap<>();\n\n");
        sb.append("    static {\n");
        sb.append("        for (ProcessorParameterKey value : values()) {\n");
        sb.append("            ProcessorParameterKey existing = KEY_INDEX.put(value.key, value);\n");
        sb.append("            if (existing != null) {\n");
        sb.append("                throw new IllegalStateException(\"Duplicate parameter key string [\" + value.key\n");
        sb.append("                        + \"] declared on both \" + existing.name() + \" and \" + value.name());\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    ProcessorParameterKey(String key) {\n");
        sb.append("        this.key = key;\n");
        sb.append("    }\n\n");
        sb.append("    @JsonCreator\n");
        sb.append("    public static ProcessorParameterKey fromKey(String key) {\n");
        sb.append("        ProcessorParameterKey resolved = KEY_INDEX.get(key);\n");
        sb.append("        if (resolved == null) {\n");
        sb.append("            throw new IllegalArgumentException(\"Unknown processor parameter key [\" + key\n");
        sb.append("                    + \"]. Add it to processors.yaml and rebuild; or fix the processors.yaml entry.\");\n");
        sb.append("        }\n");
        sb.append("        return resolved;\n");
        sb.append("    }\n\n");
        sb.append("    @JsonValue\n");
        sb.append("    @Override\n");
        sb.append("    public String toString() {\n");
        sb.append("        return key;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void writeIfChanged(Path outputPath, String content) throws IOException {
        Files.createDirectories(outputPath.getParent());
        if (Files.exists(outputPath)) {
            String existing = Files.readString(outputPath);
            if (existing.equals(content)) {
                return;
            }
        }
        Files.writeString(outputPath, content);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Catalog {
        public List<CatalogProcessor> processors;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CatalogProcessor {
        public String id;
        public List<CatalogParameter> parameters;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CatalogParameter {
        public String key;
    }
}
