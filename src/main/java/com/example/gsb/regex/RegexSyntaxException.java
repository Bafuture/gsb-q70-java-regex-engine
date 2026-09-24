package com.example.gsb.regex;

/**
 * 编译期语法错误。携带出错位置（0 基下标）与原因说明。
 */
public class RegexSyntaxException extends RuntimeException {

    private final int position;

    public RegexSyntaxException(int position, String reason) {
        super("正则语法错误（位置 " + position + "）：" + reason);
        this.position = position;
    }

    /** 出错字符在模式串中的下标（0 基）。 */
    public int position() {
        return position;
    }
}
