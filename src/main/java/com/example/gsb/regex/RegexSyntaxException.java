package com.example.gsb.regex;

/**
 * 正则表达式在编译期发现的语法错误，携带出错位置（0 基下标）与原因。
 */
public class RegexSyntaxException extends RuntimeException {

    private final int position;

    public RegexSyntaxException(String reason, int position) {
        super(reason + " (index " + position + ")");
        this.position = position;
    }

    /** 出错字符在模式串中的下标（0 基）。 */
    public int getPosition() {
        return position;
    }
}
