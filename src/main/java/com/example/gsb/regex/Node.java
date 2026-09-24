package com.example.gsb.regex;

import java.util.List;

/**
 * 正则表达式的抽象语法树节点。
 */
public sealed interface Node {

    /** 空表达式（例如 "()" 或 "a|" 的右分支），匹配空串。 */
    record Empty() implements Node {
    }

    /** 单个字面量字符。 */
    record Literal(char ch) implements Node {
    }

    /** 字符类（含 '.' 与 \d \w \s 等预定义类）。 */
    record CharClassNode(CharClass charClass) implements Node {
    }

    /** 连接运算。 */
    record Concat(List<Node> nodes) implements Node {
    }

    /** 交替运算 a|b|c。 */
    record Alt(List<Node> branches) implements Node {
    }

    /** 量词。 */
    record Repeat(Node child, Kind kind) implements Node {

        public enum Kind {
            STAR, PLUS, OPTIONAL
        }
    }
}
