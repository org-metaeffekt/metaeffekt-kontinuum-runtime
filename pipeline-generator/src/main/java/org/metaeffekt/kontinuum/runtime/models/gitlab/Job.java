package org.metaeffekt.kontinuum.runtime.models.gitlab;


import org.metaeffekt.kontinuum.runtime.models.shared.Stage;

import java.util.ArrayList;
import java.util.List;

public class Job {

    private final String name;
    private final Stage stage;
    private final String image;

    List<String> scriptBlockLines = new ArrayList<>();

    public Job(String name, Stage stage, String image) {
        this.name = name;
        this.stage = stage;
        this.image = image;
    }

    public void addScriptBlockLine(String scriptBlockLine){
        scriptBlockLines.add(scriptBlockLine);
    }

    public String toYaml() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":").append(System.lineSeparator());

        if (stage.equals(Stage.PRE)) {
            sb.append("  stage: ").append(".pre").append(System.lineSeparator());
        } else {
            sb.append("  stage: ").append(stage).append(System.lineSeparator());
        }

        sb.append("  image: ").append(image).append(System.lineSeparator());
        sb.append("  script: ").append(System.lineSeparator());
        sb.append("    - |").append(System.lineSeparator());
        for (String scriptBlockLine : scriptBlockLines) {
            sb.append("      ").append(scriptBlockLine).append(System.lineSeparator());
        }
        return sb.append(System.lineSeparator()).toString();
    }

}
