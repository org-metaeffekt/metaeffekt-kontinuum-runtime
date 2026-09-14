package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;
import org.metaeffekt.kontinuum.runtime.models.shared.PipelineConfiguration;

public class ReportStageHandlerTest {

    @BeforeAll
    public static void setup() {
        AssetExecutionContext context = TestUtils.buildMinimalAssetExecutionContext();
    }
}
