package com.example.gsb.regex;

/**
 * Raised at compile time when a pattern is not well formed.
 * Carries the exact position (0-based index into the pattern) and the reason.
 */
public class RegexSyntaxException extends RegexException {

    private final int position;

    public RegexSyntaxException(String reason, int position) {
        super(reason + " at position " + position);
        this.position = position;
    }

    /** 0-based index into the pattern where the problem was detected. */
    public int position() {
        return position;
    }
}
