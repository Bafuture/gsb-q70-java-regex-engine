package com.example.gsb.regex;

/**
 * Safety limits for the engine. Both limits exist so that untrusted,
 * business-authored patterns cannot exhaust CPU or memory.
 *
 * @param maxStates       maximum number of NFA states a compiled pattern may use
 * @param maxInputLength  maximum length of an input string passed to matches/find
 */
public record RegexOptions(int maxStates, int maxInputLength) {

    public static final int DEFAULT_MAX_STATES = 10_000;
    public static final int DEFAULT_MAX_INPUT_LENGTH = 1_000_000;

    public static final RegexOptions DEFAULT =
            new RegexOptions(DEFAULT_MAX_STATES, DEFAULT_MAX_INPUT_LENGTH);

    public RegexOptions {
        if (maxStates <= 0) {
            throw new IllegalArgumentException("maxStates must be positive");
        }
        if (maxInputLength <= 0) {
            throw new IllegalArgumentException("maxInputLength must be positive");
        }
    }
}
