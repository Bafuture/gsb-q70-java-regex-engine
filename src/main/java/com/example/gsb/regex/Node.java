package com.example.gsb.regex;

import java.util.List;

/**
 * 正则表达式的抽象语法树节点。
 */
sealed interface Node {

    /** 字面量字符。 */
    record Literal(char c) implements Node {
    }

    /** 任意单个字符（.）。 */
    record Any() implements Node {
    }

    /** 字符类，ranges 中每个元素为 {low, high} 闭区间。 */
    record CharClassNode(boolean negated, List<int[]> ranges) implements Node {
    }

    /** 连接运算。 */
    record Concat(List<Node> nodes) implements Node {
    }

    /** 交替运算（|）。 */
    record Alt(List<Node> branches) implements Node {
    }

    /** 量词（* + ?）。 */
    record Repeat(Node inner, Kind kind) implements Node {
        enum Kind {
            STAR, PLUS, QUESTION
        }
    }
}
