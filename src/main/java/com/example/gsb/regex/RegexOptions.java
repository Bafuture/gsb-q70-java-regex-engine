package com.example.gsb.regex;

/**
 * 引擎资源上限配置。
 *
 * @param maxStates      编译期允许的 NFA 最大状态数
 * @param maxInputLength 匹配期允许的最大输入长度（字符数）
 */
public record RegexOptions(int maxStates, int maxInputLength) {

    public static final int DEFAULT_MAX_STATES = 10_000;
    public static final int DEFAULT_MAX_INPUT_LENGTH = 1_000_000;

    public RegexOptions {
        if (maxStates <= 0) {
            throw new IllegalArgumentException("maxStates 必须为正数: " + maxStates);
        }
        if (maxInputLength < 0) {
            throw new IllegalArgumentException("maxInputLength 不能为负数: " + maxInputLength);
        }
    }

    public static RegexOptions defaults() {
        return new RegexOptions(DEFAULT_MAX_STATES, DEFAULT_MAX_INPUT_LENGTH);
    }
}
