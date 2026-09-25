package com.example.gsb.regex;

/**
 * Base type for all errors raised by this regex engine.
 */
public class RegexException extends RuntimeException {

    public RegexException(String message) {
        super(message);
    }
}
