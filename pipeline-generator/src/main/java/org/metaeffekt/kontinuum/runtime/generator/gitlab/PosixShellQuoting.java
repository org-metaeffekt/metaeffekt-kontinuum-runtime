package org.metaeffekt.kontinuum.runtime.generator.gitlab;

import java.util.Objects;

/**
 * Quotes literal arguments for POSIX-compatible shells.
 */
final class PosixShellQuoting {

    private PosixShellQuoting() {
    }

    static String quote(String argument) {
        Objects.requireNonNull(argument, "Shell argument must not be null.");
        if (argument.indexOf('\r') >= 0 || argument.indexOf('\n') >= 0 || argument.indexOf(0) >= 0) {
            throw new IllegalArgumentException("Shell arguments must not contain CR, LF, or NUL characters.");
        }
        return "'" + argument.replace("'", "'\"'\"'") + "'";
    }
}
