package org.metaeffekt.kontinuum.runtime.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility methods for normalizing file and directory paths used by the pipeline generator.
 */
@Slf4j
public class KontinuumUtils {

    /**
     * Joins the given path segments and ensures the result ends with a directory separator.
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
            if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '/') {
                sb.append('/');
            }
            if (!sb.isEmpty() && part.startsWith("/")) {
                part = part.substring(1);
            }
            sb.append(part);
        }
        return sb.toString().replaceAll("/{2,}", "/");
    }
}
