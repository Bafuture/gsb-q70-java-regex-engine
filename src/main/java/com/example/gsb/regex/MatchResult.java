package com.example.gsb.regex;

/**
 * 一次查找命中的结果，[start, end) 为半开区间下标。
 */
public record MatchResult(int start, int end) {

    public int length() {
        return end - start;
    }
}
