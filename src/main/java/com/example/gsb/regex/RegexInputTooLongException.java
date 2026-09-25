package com.example.gsb.regex;

/**
 * Raised at match time when the input exceeds the configured maximum length.
 */
public class RegexInputTooLongException extends RegexException {

    public RegexInputTooLongException(String message) {
        super(message);
    }
}
