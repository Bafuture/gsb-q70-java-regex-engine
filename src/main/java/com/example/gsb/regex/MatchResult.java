package com.example.gsb.regex;

/**
 * A single match located by {@link Regex#find(String)}.
 *
 * @param start 0-based inclusive start index in the input
 * @param end   0-based exclusive end index in the input
 */
public record MatchResult(int start, int end) {

    /** Extracts the matched text from the given input. */
    public String group(String input) {
        return input.substring(start, end);
    }
}
