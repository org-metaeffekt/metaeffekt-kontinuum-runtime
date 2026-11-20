package org.metaeffekt.kontinuum.runtime.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;

import java.util.List;

/**
 * Utility methods for normalizing file and directory paths used by the pipeline generator.
 */
@Slf4j
public class KontinuumUtils {

    private static final List<String> INVENTORY_FILE_SUFFIXES = List.of(".xls", ".xlsx", ".ser");

    /**
     * Checks by file suffix whether the given path points to an inventory file. The file system is not accessed.
     *
     * @param path the path to check
     *
     * @return true if the path ends with an inventory file suffix, ignoring case
     */
    public static boolean isInventoryFile(String path) {
        return INVENTORY_FILE_SUFFIXES.stream().anyMatch(suffix -> Strings.CI.endsWith(path, suffix));
    }

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
