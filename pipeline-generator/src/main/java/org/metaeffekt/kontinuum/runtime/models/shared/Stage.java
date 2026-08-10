package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Getter;

/**
 * The available stages a pipelines can run.
 * The stages listed below must be in the correct order.
 */
public enum Stage {
    PRE(".pre"),
    FETCH("00_fetched"),
    EXTRACT("01_extracted"),
    PREPARE("02_prepared"),
    AGGREGATE("03_aggregated"),
    RESOLVE("04_resolved"),
    SCAN("05_scanned"),
    ADVISE("06_advised"),
    GROUP("07_grouped"),
    REPORT("08_reported"),
    SUMMARIZE("09_summarized"),
    UTIL("xx_additional"),
    POST(".post");

    @Getter
    private final String stageDirectory;

    Stage(String stageDirectory) {
        this.stageDirectory = stageDirectory;
    }
}
