package org.metaeffekt.kontinuum.runtime.generator.shared.stages;

import org.junit.jupiter.api.BeforeAll;
import org.metaeffekt.kontinuum.runtime.TestUtils;
import org.metaeffekt.kontinuum.runtime.models.shared.AssetExecutionContext;

public class ReportStageHandlerTest {

    @BeforeAll
    public static void setup() {
        AssetExecutionContext context = TestUtils.buildMinimalAssetExecutionContext();
    }
}
