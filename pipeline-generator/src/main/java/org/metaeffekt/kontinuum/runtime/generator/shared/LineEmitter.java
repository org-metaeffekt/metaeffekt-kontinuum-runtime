package org.metaeffekt.kontinuum.runtime.generator.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collects generic line-oriented text with scoped indentation for use by generator implementations.
 * <p>
 * This class is implementation-internal and is not a supported public API.
 */
public final class LineEmitter {

    private static final String INDENT = "  ";

    private final List<String> lines = new ArrayList<>();
    private int indentLevel;

    /**
     * Adds one line, rejecting embedded newline characters.
     *
     * @param line the line content
     * @return this emitter
     */
    public LineEmitter line(String line) {
        Objects.requireNonNull(line, "line must not be null");
        if (line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                    "line must not contain CR or LF characters; use multiline(String) instead");
        }
        appendLine(line);
        return this;
    }

    /**
     * Adds all lines from the given content after normalizing CRLF and CR line endings to LF.
     *
     * @param multiline the multiline content
     * @return this emitter
     */
    public LineEmitter multiline(String multiline) {
        Objects.requireNonNull(multiline, "multiline must not be null");
        String normalized = multiline.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalized.split("\n", -1)) {
            appendLine(line);
        }
        return this;
    }

    /**
     * Adds an empty line without indentation.
     *
     * @return this emitter
     */
    public LineEmitter blankLine() {
        lines.add("");
        return this;
    }

    /**
     * Opens a scope that adds one indentation level until it is closed.
     *
     * @return the indentation scope, intended for use with try-with-resources
     */
    public Indent indent() {
        indentLevel++;
        return new Indent();
    }

    /**
     * Renders the current content using LF line endings and omitting trailing blank lines.
     *
     * @return the rendered content, or an empty string when there is no nonempty line
     */
    public String render() {
        int end = lines.size();
        while (end > 0 && lines.get(end - 1).isEmpty()) {
            end--;
        }

        StringBuilder rendered = new StringBuilder();
        for (int index = 0; index < end; index++) {
            rendered.append(lines.get(index)).append('\n');
        }
        return rendered.toString();
    }

    private void appendLine(String line) {
        lines.add(line.isEmpty() ? "" : INDENT.repeat(indentLevel) + line);
    }

    /**
     * A scoped indentation level. Closing the scope does not throw a checked exception.
     */
    public final class Indent implements AutoCloseable {

        private boolean closed;

        private Indent() {
        }

        @Override
        public void close() {
            if (!closed) {
                indentLevel--;
                closed = true;
            }
        }
    }
}
