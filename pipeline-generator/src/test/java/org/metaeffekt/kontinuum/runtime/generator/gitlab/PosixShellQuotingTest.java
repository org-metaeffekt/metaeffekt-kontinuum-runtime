package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PosixShellQuotingTest {

    @Test
    void quotesApostrophesAndPreventsExpansion() {
        assertEquals("'value with '\"'\"'quote'\"'\"' and $HOME'",
                PosixShellQuoting.quote("value with 'quote' and $HOME"));
    }

    @Test
    void rejectsShellControlCharacters() {
        for (String argument : List.of("line\rbreak", "line\nbreak", "nul\0character")) {
            assertThrows(IllegalArgumentException.class, () -> PosixShellQuoting.quote(argument));
        }
    }
}
