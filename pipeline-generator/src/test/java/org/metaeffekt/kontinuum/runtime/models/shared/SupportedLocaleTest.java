package org.metaeffekt.kontinuum.runtime.models.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.generator.shared.PipelineConfigurationLoader;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class SupportedLocaleTest {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Test
    public void testFromStringMapping() {
        assertEquals(SupportedLocale.EN_US, SupportedLocale.fromString("en_US"));
        assertEquals(SupportedLocale.EN_US, SupportedLocale.fromString("en"));
        assertEquals(SupportedLocale.EN_US, SupportedLocale.fromString("EN_US"));
        assertEquals(SupportedLocale.EN_US, SupportedLocale.fromString("EN"));

        assertEquals(SupportedLocale.DE_DE, SupportedLocale.fromString("de_DE"));
        assertEquals(SupportedLocale.DE_DE, SupportedLocale.fromString("de"));
        assertEquals(SupportedLocale.DE_DE, SupportedLocale.fromString("DE_DE"));
        assertEquals(SupportedLocale.DE_DE, SupportedLocale.fromString("DE"));

        assertNull(SupportedLocale.fromString(null));
        assertNull(SupportedLocale.fromString(""));
        assertNull(SupportedLocale.fromString("   "));

        assertThrows(IllegalArgumentException.class, () -> SupportedLocale.fromString("fr_FR"));
    }

    @Test
    public void testYamlDeserializationWithLocales() throws Exception {
        String yaml = """
            assetIds: ["sample-asset"]
            types: ["VR"]
            locales: ["en_US", "de_DE"]
            """;

        PipelineConfiguration.Report report = yamlMapper.readValue(yaml, PipelineConfiguration.Report.class);

        assertNotNull(report);
        assertNotNull(report.getLocales());
        assertEquals(2, report.getLocales().size());
        assertEquals(SupportedLocale.EN_US, report.getLocales().get(0));
        assertEquals(SupportedLocale.DE_DE, report.getLocales().get(1));
    }

    @Test
    public void testYamlDeserializationWithShortLanguageCodes() throws Exception {
        String yaml = """
            assetIds: ["sample-asset"]
            types: ["VR"]
            locales: ["en", "de"]
            """;

        PipelineConfiguration.Report report = yamlMapper.readValue(yaml, PipelineConfiguration.Report.class);

        assertNotNull(report);
        assertNotNull(report.getLocales());
        assertEquals(2, report.getLocales().size());
        assertEquals(SupportedLocale.EN_US, report.getLocales().get(0));
        assertEquals(SupportedLocale.DE_DE, report.getLocales().get(1));
    }

    @Test
    public void testPipelineConfigurationLoaderWithValidConfig() {
        PipelineConfigurationLoader loader = new PipelineConfigurationLoader();
        PipelineConfiguration config = loader.readConfig(new File("src/test/resources/valid-pipeline-config.yaml"));

        assertNotNull(config);
        assertNotNull(config.getReports());
        assertEquals(2, config.getReports().size());
        assertEquals(SupportedLocale.EN_US, config.getReports().get(0).getLocales().get(0));
        assertEquals(SupportedLocale.DE_DE, config.getReports().get(1).getLocales().get(0));
    }
}
