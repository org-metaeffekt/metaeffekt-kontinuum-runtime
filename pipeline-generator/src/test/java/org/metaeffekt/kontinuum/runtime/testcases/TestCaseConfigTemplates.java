package org.metaeffekt.kontinuum.runtime.testcases;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Resolves the configuration templates of the test cases. See {@code test-cases/README.md} for the layout and the
 * placeholders of the templates.
 */
final class TestCaseConfigTemplates {

    static final Path TEST_CASES_DIR = Path.of("src/test/resources/test-cases").toAbsolutePath().normalize();

    static final Path SHARED_WORKBENCH_DIR = TEST_CASES_DIR.resolve("shared").resolve("workbench");

    private static final Path RESOLVED_CONFIG_ROOT = Path.of("target/test-cases").toAbsolutePath();

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private TestCaseConfigTemplates() {
    }

    static Path workbenchDir(String testCase) {
        return TEST_CASES_DIR.resolve(testCase).resolve("workbench");
    }

    static Path workspaceDir(String testCase) {
        return TEST_CASES_DIR.resolve(testCase).resolve("workspace");
    }

    /**
     * Replaces the placeholders of the configuration template of a test case with absolute paths and writes the
     * result to {@code target/test-cases/<test case>/}. Only this copy is to be used by the test. Fails the test if
     * a placeholder is left after the replacement.
     *
     * @param testCase       the test case, e.g. {@code tc-0001}
     * @param configFileName the file name of the template in the workbench of the test case
     * @return the resolved configuration file
     * @throws IOException if the template cannot be read or the resolved configuration cannot be written
     */
    static Path resolve(String testCase, String configFileName) throws IOException {
        Map<String, Path> placeholders = Map.of(
                "workbench.dir", workbenchDir(testCase),
                "workspace.dir", workspaceDir(testCase),
                "shared.workbench.dir", SHARED_WORKBENCH_DIR);

        String config = Files.readString(workbenchDir(testCase).resolve(configFileName));
        for (Map.Entry<String, Path> placeholder : placeholders.entrySet()) {
            config = config.replace("${" + placeholder.getKey() + "}", placeholder.getValue().toString());
        }

        Matcher unresolved = PLACEHOLDER.matcher(config);
        if (unresolved.find()) {
            fail("Unknown placeholder ${" + unresolved.group(1) + "} in " + configFileName
                 + ", known placeholders: " + placeholders.keySet());
        }

        Path resolvedConfigDir = RESOLVED_CONFIG_ROOT.resolve(testCase);
        Files.createDirectories(resolvedConfigDir);
        Path resolvedConfig = resolvedConfigDir.resolve(configFileName);
        Files.writeString(resolvedConfig, config);
        return resolvedConfig;
    }
}
