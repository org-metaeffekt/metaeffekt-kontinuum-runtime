package org.metaeffekt.kontinuum.runtime.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility methods for normalizing file and directory paths used by the pipeline generator.
 */
@Slf4j
public class KontinuumUtils {

    /**
     * Joins the given path segments and ensures the result ends with a directory separator.
     * A segment starting with {@code /} is absolute and discards the segments before it.
     *
     * @param path the path segments to join
     * @return the normalized directory path
     */
    public static String normalizeDir(String... path) {
        String result = joinPath(path);
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result;
    }

    /**
     * Joins the given path segments into a normalized file path.
     * A segment starting with {@code /} is absolute and discards the segments before it.
     *
     * @param path the path segments to join
     * @return the normalized file path
     */
    public static String normalizeFilePath(String... path) {
        return joinPath(path);
    }

    private static String joinPath(String... path) {
        StringBuilder sb = new StringBuilder();
        for (String part : path) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (part.startsWith("/")) {
                // absolute segment: everything joined so far is replaced
                sb.setLength(0);
            } else if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '/') {
                sb.append('/');
            }
            sb.append(part);
        }
        return sb.toString().replaceAll("/{2,}", "/");
    }
}
