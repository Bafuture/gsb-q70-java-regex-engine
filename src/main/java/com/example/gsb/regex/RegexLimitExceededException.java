package com.example.gsb.regex;

/**
 * 超过配置的安全上限（NFA 状态数或输入长度）时抛出。
 */
public class RegexLimitExceededException extends RuntimeException {

    public RegexLimitExceededException(String message) {
        super(message);
    }
}
