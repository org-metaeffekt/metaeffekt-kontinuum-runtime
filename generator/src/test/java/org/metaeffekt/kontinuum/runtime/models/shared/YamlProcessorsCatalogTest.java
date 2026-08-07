package org.metaeffekt.kontinuum.runtime.models.shared;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class YamlProcessorsCatalogTest {

    /**
     * If this test method fails, this means there were changes in the processors.yaml file parsed by
     * {@link YamlProcessorCatalog}. Ensure that changed processor ids are reflected in the rest of the code-base
     *  as they are usually hard coded as Strings.
     */
    @Test
    public void testExpectedProcessorsExistInYaml() {
        YamlProcessorCatalog processorCatalog = new YamlProcessorCatalog();
        List<String> expectedProcessorIds = new ArrayList<String>();

        expectedProcessorIds.add("attach-metadata");
        expectedProcessorIds.add("create-dashboard");
        expectedProcessorIds.add("enrich-inventory");
        expectedProcessorIds.add("enrich-inventory-with-reference");
        expectedProcessorIds.add("cyclonedx-to-inventory");
        expectedProcessorIds.add("inventory-to-cyclonedx");
        expectedProcessorIds.add("inventory-to-spdx");
        expectedProcessorIds.add("copy-pom-dependencies");
        expectedProcessorIds.add("save-inspect-image");
        expectedProcessorIds.add("download-data-sources");
        expectedProcessorIds.add("download-index");
        expectedProcessorIds.add("download-asset");
        expectedProcessorIds.add("update-index");
        expectedProcessorIds.add("update-index_external");
        expectedProcessorIds.add("copy-resources");
        expectedProcessorIds.add("create-overview");
        expectedProcessorIds.add("scan-directory");
        expectedProcessorIds.add("aggregate-annex-folders");
        expectedProcessorIds.add("create-annex-archive");
        expectedProcessorIds.add("create-document");
        expectedProcessorIds.add("resolve-inventory");
        expectedProcessorIds.add("scan-inventory");
        expectedProcessorIds.add("aggregate-sources");
        expectedProcessorIds.add("convert-assessments");
        expectedProcessorIds.add("copy-inventories");
        expectedProcessorIds.add("create-diff");
        expectedProcessorIds.add("enrich-advisors");
        expectedProcessorIds.add("generate-report-svg");
        expectedProcessorIds.add("merge-advisors");
        expectedProcessorIds.add("merge-assessments");
        expectedProcessorIds.add("merge-inventories");
        expectedProcessorIds.add("portfolio-download");
        expectedProcessorIds.add("portfolio-download-jars");
        expectedProcessorIds.add("portfolio-upload");
        expectedProcessorIds.add("transform-inventories");
        expectedProcessorIds.add("validate-reference-inventory");

        List<String> actualProcessorIds = processorCatalog.getProcessors().stream()
                .map(ProcessorDefinitions.MavenProcessor::getId)
                .toList();

        for (String expectedId : expectedProcessorIds) {
            assertTrue(actualProcessorIds.contains(expectedId),
                    "Expected processor id [" + expectedId + "] is not present in processorCatalog.getProcessors()");
        }
    }

    /**
     * Guards the YAML&rarr;enum contract for parameter keys in a single place. {@code ProcessorParameterKey}
     * is generated from {@code processors.yaml}, so by construction every {@code key:} entry has a constant
     * and vice-versa. This round-trip test catches a stale generated file, a generator bug, or a manual
     * edit to the generated enum: it asserts the set of keys in the YAML equals the set of {@code key}
     * fields on the enum constants, with a precise diff on failure.
     */
    @Test
    public void testAllCatalogParameterKeysRoundTripWithEnum() {
        YamlProcessorCatalog processorCatalog = new YamlProcessorCatalog();

        java.util.Set<String> yamlKeys = new java.util.TreeSet<>();
        for (ProcessorDefinitions.MavenProcessor processor : processorCatalog.getProcessors()) {
            for (ProcessorDefinitions.ProcessorParameter parameter : processor.getParameters()) {
                org.junit.jupiter.api.Assertions.assertNotNull(parameter.getKey(),
                        "Parameter key on processor [" + processor.getId() + "] resolved to null");
                yamlKeys.add(parameter.getKey().getKey());
            }
        }

        java.util.Set<String> enumKeys = new java.util.TreeSet<>();
        for (ProcessorParameterKey constant : ProcessorParameterKey.values()) {
            enumKeys.add(constant.getKey());
        }

        java.util.Set<String> onlyInYaml = new java.util.TreeSet<>(yamlKeys);
        onlyInYaml.removeAll(enumKeys);
        java.util.Set<String> onlyInEnum = new java.util.TreeSet<>(enumKeys);
        onlyInEnum.removeAll(yamlKeys);

        assertTrue(onlyInYaml.isEmpty(),
                "Keys present in processors.yaml but missing from generated ProcessorParameterKey: " + onlyInYaml
                        + ". Rebuild to regenerate the enum.");
        assertTrue(onlyInEnum.isEmpty(),
                "Constants present in ProcessorParameterKey but absent from processors.yaml: " + onlyInEnum
                        + ". The generated enum is stale; rebuild.");
    }

}
