package org.metaeffekt.kontinuum.runtime.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KontinuumUtilsTest {

    @Test
    void normalizeDirAppendsSlash() {
        assertEquals("usr/local/", KontinuumUtils.normalizeDir("usr/local"));
    }

    @Test
    void normalizeDirPreservesSlash() {
        assertEquals("usr/local/", KontinuumUtils.normalizeDir("usr/local/"));
    }

    @Test
    void normalizeDirJoinsWithSlash() {
        assertEquals("usr/local/bin/", KontinuumUtils.normalizeDir("usr", "local", "bin"));
    }

    @Test
    void normalizeDirNoDoubleSlash() {
        assertEquals("usr/local/bin/", KontinuumUtils.normalizeDir("usr", "/local", "/bin"));
        assertEquals("usr/local/bin/", KontinuumUtils.normalizeDir("usr/", "local/", "bin/"));
    }

    @Test
    void normalizeDirCollapsesCorrectly() {
        assertEquals("usr/local/bin/", KontinuumUtils.normalizeDir("usr/", "/local/", "/bin/"));
    }

    @Test
    void normalizeDirPreservesLeadingSlash() {
        assertEquals("/usr/local/", KontinuumUtils.normalizeDir("/usr", "local"));
    }

    @Test
    void normalizeDirNullSegmentSkipped() {
        assertEquals("usr/local/", KontinuumUtils.normalizeDir("usr", null, "local"));
    }

    @Test
    void normalizeDirEmptySegmentSkipped() {
        assertEquals("usr/local/", KontinuumUtils.normalizeDir("usr", "", "local"));
    }

    @Test
    void normalizeDirNoInputSegments() {
        assertEquals("/", KontinuumUtils.normalizeDir((String) null));
        assertEquals("/", KontinuumUtils.normalizeDir());
    }

    @Test
    void normalizeDirTripleSlashCollapsed() {
        assertEquals("/a/b/c/", KontinuumUtils.normalizeDir("///a///b///c///"));
    }
}
