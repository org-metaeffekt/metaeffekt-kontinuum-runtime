package org.metaeffekt.kontinuum.runtime.generator.shared;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineEmitterTest {

    @Test
    void nestsIndentationUsingTwoSpacesPerLevel() {
        LineEmitter emitter = new LineEmitter();
        emitter.line("root");
        try (LineEmitter.Indent ignored = emitter.indent()) {
            emitter.line("child");
            emitter.blankLine();
            try (LineEmitter.Indent nested = emitter.indent()) {
                emitter.line("grandchild");
            }
            emitter.line("child again");
        }
        emitter.line("root again");

        assertEquals("root\n  child\n\n    grandchild\n  child again\nroot again\n", emitter.render());
    }

    @Test
    void multilineNormalizesAllNewlineVariants() {
        LineEmitter emitter = new LineEmitter();

        emitter.multiline("one\r\ntwo\rthree\nfour");

        assertEquals("one\ntwo\nthree\nfour\n", emitter.render());
    }

    @Test
    void preservesWhitespaceOnNonemptyLines() {
        LineEmitter emitter = new LineEmitter();

        try (LineEmitter.Indent ignored = emitter.indent()) {
            emitter.line("  leading and trailing \t ");
            emitter.multiline("\tsecond  \n  third\t");
        }

        assertEquals("    leading and trailing \t \n  \tsecond  \n    third\t\n", emitter.render());
    }

    @Test
    void preservesInternalBlankLinesAndOmitsTrailingBlankLines() {
        LineEmitter emitter = new LineEmitter();

        emitter.blankLine().line("").line("first").multiline("\nsecond\n\n").blankLine();

        assertEquals("\n\nfirst\n\nsecond\n", emitter.render());
    }

    @Test
    void rendersEmptyStringForEmptyOrBlankOnlyDocuments() {
        assertEquals("", new LineEmitter().render());

        LineEmitter blankLines = new LineEmitter();
        blankLines.blankLine().line("").multiline("\r\n\r");
        assertEquals("", blankLines.render());
    }

    @Test
    void rejectsNullLineAndMultilineContentImmediately() {
        LineEmitter emitter = new LineEmitter();

        assertThrows(NullPointerException.class, () -> emitter.line(null));
        assertThrows(NullPointerException.class, () -> emitter.multiline(null));
        assertEquals("", emitter.render());
    }

    @Test
    void lineRejectsEmbeddedNewlineCharacters() {
        LineEmitter emitter = new LineEmitter();

        for (String invalid : List.of("bad\nline", "bad\rline", "bad\r\nline")) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> emitter.line(invalid));
            assertTrue(exception.getMessage().contains("multiline(String)"));
        }
        assertEquals("", emitter.render());
    }

    @Test
    void repeatedRenderCallsAreStable() {
        LineEmitter emitter = new LineEmitter();
        emitter.multiline("first\n\nsecond\n\n");

        String rendered = emitter.render();

        assertEquals("first\n\nsecond\n", rendered);
        assertEquals(rendered, emitter.render());
        assertEquals(rendered, emitter.render());
    }
}
