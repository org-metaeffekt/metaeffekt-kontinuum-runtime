package org.metaeffekt.kontinuum.runtime.models.shared;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class YamlProcessorCatalog implements ProcessorCatalog {

    public final List<ProcessorDefinitions.MavenProcessor> catalog;

    public YamlProcessorCatalog() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("processors.yaml");
        if (is == null) {
            throw new IllegalStateException("processors.yaml not found on classpath");
        }
        this.catalog = load(is);
    }

    @Override
    public List<ProcessorDefinitions.MavenProcessor> getProcessors() {
        return catalog;
    }

    @Override
    public ProcessorDefinitions.MavenProcessor getProcessorById(String processorId) {
        List<ProcessorDefinitions.MavenProcessor> foundMavenProcessors = catalog.stream()
                .filter(p -> p.id.equals(processorId))
                .toList();

        if (foundMavenProcessors.size() > 1) {
            log.error("More than one processor found with id {}, continuing with processor {}", processorId, foundMavenProcessors.get(0).id);
        }

        ProcessorDefinitions.MavenProcessor original = foundMavenProcessors.get(0);
        ProcessorDefinitions.MavenProcessor copy = new ProcessorDefinitions.MavenProcessor();
        copy.setId(original.getId());
        copy.setName(original.getName());
        copy.setPomLocation(original.getPomLocation());
        copy.setGoal(original.getGoal());
        copy.setStage(original.getStage());
        List<ProcessorDefinitions.ProcessorParameter> copiedParameters = new ArrayList<>();
        for (ProcessorDefinitions.ProcessorParameter parameter : original.getParameters()) {
            ProcessorDefinitions.ProcessorParameter parameterCopy = new ProcessorDefinitions.ProcessorParameter();
            parameterCopy.setKey(parameter.getKey());
            parameterCopy.setDescription(parameter.getDescription());
            parameterCopy.setRequired(parameter.getRequired());
            parameterCopy.setValue(parameter.getValue());
            copiedParameters.add(parameterCopy);
        }
        copy.setParameters(copiedParameters);
        return copy;
    }

    /**
     * Jackson mixin applied to {@link ProcessorDefinitions.Processor} so that the
     * {@code Processor} / {@code MavenProcessor} / {@code StandaloneProcessor} hierarchy is
     * deserialized by deduction: Jackson inspects the fields present on each YAML entry and
     * picks the matching subtype (entries with {@code pomLocation}/{@code goal}/{@code parameters}
     * become {@link ProcessorDefinitions.MavenProcessor}; entries with {@code script} become
     * {@link ProcessorDefinitions.StandaloneProcessor}).
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
            @JsonSubTypes.Type(ProcessorDefinitions.MavenProcessor.class),
            @JsonSubTypes.Type(ProcessorDefinitions.StandaloneProcessor.class)
    })
    private interface ProcessorMixin {
    }

    private List<ProcessorDefinitions.MavenProcessor> load(InputStream inputStream) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.addMixIn(ProcessorDefinitions.Processor.class, ProcessorMixin.class);
        try {
            ProcessorDefinitions processorDefinitions = objectMapper.readValue(inputStream, ProcessorDefinitions.class);
            return processorDefinitions.processors.stream()
                    .filter(ProcessorDefinitions.MavenProcessor.class::isInstance)
                    .map(ProcessorDefinitions.MavenProcessor.class::cast)
                    .sorted(Comparator.comparing(p -> p.name)).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
