package com.example.gsb.regex;

/**
 * 引擎安全上限配置。
 *
 * @param maxStates      编译后 NFA 允许的最大状态数，超限在编译期报错
 * @param maxInputLength 允许匹配的最大输入长度，超限在匹配期报错
 */
public record RegexOptions(int maxStates, int maxInputLength) {

    public static final RegexOptions DEFAULT = new RegexOptions(10_000, 1_000_000);

    public RegexOptions {
        if (maxStates < 1) {
            throw new IllegalArgumentException("maxStates must be >= 1, got " + maxStates);
        }
        if (maxInputLength < 0) {
            throw new IllegalArgumentException("maxInputLength must be >= 0, got " + maxInputLength);
        }
    }
}
