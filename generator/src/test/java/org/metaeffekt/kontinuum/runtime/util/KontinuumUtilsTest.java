package org.metaeffekt.kontinuum.runtime.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KontinuumUtilsTest {

    @Test
    void normalizeDir_singlePathWithoutTrailingSlash_appendsSlash() {
        assertEquals("usr/local/", KontinuumUtils.normalizeDir("usr/local"));
    }

    @Test
    void normalizeDir_singlePathWithTrailingSlash_preservesSlash() {
        assertEquals("usr/local/", KontinuumUtils.normalizeDir("usr/local/"));
    }

    @Test
    void normalizeDir_multipleSegments_joinsWithSlash() {
        assertEquals("usr/local/bin/", KontinuumUtils.normalizeDir("usr", "local", "bin"));
    }

    @Test
    void normalizeDir_segmentsWithLeadingSlash_noDoubleSlash() {
        assertEquals("usr/local/bin/", KontinuumUtils.normalizeDir("usr", "/local", "/bin"));
    }

    @Test
    void normalizeDir_segmentsWithTrailingSlash_noDoubleSlash() {
        assertEquals("usr/local/bin/", KontinuumUtils.normalizeDir("usr/", "local/", "bin/"));
    }

    @Test
    void normalizeDir_mixedSlashes_collapsesCorrectly() {
        assertEquals("usr/local/bin/", KontinuumUtils.normalizeDir("usr/", "/local/", "/bin/"));
    }

    @Test
    void normalizeDir_absolutePath_preservesLeadingSlash() {
        assertEquals("/usr/local/", KontinuumUtils.normalizeDir("/usr", "local"));
    }

    @Test
    void normalizeDir_nullSegment_skipped() {
        assertEquals("usr/local/", KontinuumUtils.normalizeDir("usr", null, "local"));
    }

    @Test
    void normalizeDir_emptySegment_skipped() {
        assertEquals("usr/local/", KontinuumUtils.normalizeDir("usr", "", "local"));
    }

    @Test
    void normalizeDir_allNull_returnsSlash() {
        assertEquals("/", KontinuumUtils.normalizeDir((String) null));
    }

    @Test
    void normalizeDir_noArguments_returnsSlash() {
        assertEquals("/", KontinuumUtils.normalizeDir());
    }

    @Test
    void normalizeDir_tripleSlash_collapsed() {
        assertEquals("/a/b/c/", KontinuumUtils.normalizeDir("///a///b///c///"));
    }
}
