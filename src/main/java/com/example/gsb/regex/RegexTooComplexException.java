package com.example.gsb.regex;

/**
 * Raised at compile time when the compiled NFA would exceed the configured
 * maximum number of states.
 */
public class RegexTooComplexException extends RegexException {

    public RegexTooComplexException(String message) {
        super(message);
    }
}
