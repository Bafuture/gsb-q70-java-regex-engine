package com.example.gsb.regex;

/**
 * 资源上限被突破时抛出，例如 NFA 状态数超限或输入长度超限。
 */
public class RegexLimitExceededException extends RuntimeException {

    public RegexLimitExceededException(String message) {
        super(message);
    }
}
