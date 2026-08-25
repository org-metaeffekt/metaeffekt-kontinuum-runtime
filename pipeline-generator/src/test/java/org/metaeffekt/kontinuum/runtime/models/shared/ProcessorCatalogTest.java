package org.metaeffekt.kontinuum.runtime.models.shared;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.metaeffekt.kontinuum.runtime.models.shared.DefaultProcessorCatalog.ProcessorIds.*;

public class ProcessorCatalogTest {

    @Test
    public void testExpectedProcessorsExist() {
        ProcessorCatalog processorCatalog = new DefaultProcessorCatalog();
        List<DefaultProcessorCatalog.ProcessorIds> expectedProcessorIds = List.of(
                AGGREGATE_LICENSES,
                AGGREGATE_REFERENCE_LICENSES,
                AGGREGATE_SOURCES,
                APPLY_BUSINESS_CASE,
                ATTACH_METADATA,
                CONVERT_ASSESSMENTS,
                COPY_INVENTORIES,
                COPY_POM_DEPENDENCIES,
                COPY_RESOURCES,
                CREATE_ANNEX_ARCHIVE,
                CREATE_DASHBOARD,
                CREATE_DIFF,
                CREATE_DOCUMENT,
                CREATE_OVERVIEW,
                CYCLONEDX_TO_INVENTORY,
                DOWNLOAD_ASSET,
                DOWNLOAD_DATA_SOURCES,
                DOWNLOAD_INDEX,
                DOWNLOAD_MAVEN_ARTIFACT,
                ENRICH_ADVISORS,
                ENRICH_INVENTORY,
                ENRICH_WITH_REFERENCE,
                EXECUTE_KOTLIN_SCRIPT,
                GENERATE_REPORT_SVG,
                INVENTORY_TO_CYCLONEDX,
                INVENTORY_TO_SPDX,
                MERGE_ADVISORS,
                MERGE_ASSESSMENTS,
                MERGE_INVENTORIES,
                PORTFOLIO_DOWNLOAD,
                PORTFOLIO_DOWNLOAD_JARS,
                PORTFOLIO_UPLOAD,
                RESOLVE_INVENTORY,
                SAVE_INSPECT_IMAGE,
                SCAN_DIRECTORY,
                SCAN_INVENTORY,
                TRANSFORM_INVENTORIES,
                UPDATE_INDEX,
                UPDATE_INDEX_EXTERNAL,
                VALIDATE_REFERENCE_INVENTORY
        );

        List<String> actualProcessorIds = processorCatalog.getProcessors().stream()
                .map(ProcessorDefinitions.MavenProcessor::getId)
                .toList();

        for (DefaultProcessorCatalog.ProcessorIds expectedId : expectedProcessorIds) {
            assertTrue(actualProcessorIds.contains(expectedId.getValue()),
                    "Expected processor id [" + expectedId + "] is not present in processorCatalog.getProcessors()");
            assertNotNull(processorCatalog.getProcessorById(expectedId),
                    "Expected processor id [" + expectedId + "] could not be retrieved via getProcessorById");
        }

        // Test alias
        assertNotNull(processorCatalog.getProcessorById(ENRICH_INVENTORY_WITH_REFERENCE),
                "Expected alias enrich-inventory-with-reference to resolve to enrich-with-reference");
    }

    @Test
    public void testAllCatalogParameterKeysRoundTripWithEnum() {
        ProcessorCatalog processorCatalog = new DefaultProcessorCatalog();

        Set<String> catalogKeys = new TreeSet<>();
        for (ProcessorDefinitions.MavenProcessor processor : processorCatalog.getProcessors()) {
            for (ProcessorDefinitions.ProcessorParameter parameter : processor.getParameters()) {
                assertNotNull(parameter.getKey(),
                        "Parameter key on processor [" + processor.getId() + "] resolved to null");
                catalogKeys.add(parameter.getKey().getKey());
            }
        }

        Set<String> enumKeys = new TreeSet<>();
        for (ProcessorParameterKey constant : ProcessorParameterKey.values()) {
            enumKeys.add(constant.getKey());
        }

        Set<String> onlyInCatalog = new TreeSet<>(catalogKeys);
        onlyInCatalog.removeAll(enumKeys);
        Set<String> onlyInEnum = new TreeSet<>(enumKeys);
        onlyInEnum.removeAll(catalogKeys);

        assertTrue(onlyInCatalog.isEmpty(),
                "Keys present in catalog but missing from ProcessorParameterKey: " + onlyInCatalog);
        assertTrue(onlyInEnum.isEmpty(),
                "Constants present in ProcessorParameterKey but absent from catalog: " + onlyInEnum);
    }

    @Test
    public void testProcessorCopyIsolation() {
        ProcessorCatalog processorCatalog = new DefaultProcessorCatalog();
        ProcessorDefinitions.MavenProcessor copy1 = processorCatalog.getProcessorById(DOWNLOAD_INDEX);
        ProcessorDefinitions.MavenProcessor copy2 = processorCatalog.getProcessorById(DOWNLOAD_INDEX);

        assertNotSame(copy1, copy2);
        copy1.setProcessorParameter(ProcessorParameterKey.PARAM_MIRROR_ARCHIVE_URL, "http://example.com/test");

        assertNull(copy2.getParameters().stream()
                .filter(p -> p.getKey() == ProcessorParameterKey.PARAM_MIRROR_ARCHIVE_URL)
                .findFirst().orElseThrow().getValue());
    }
}
