package com.example.gsb.regex;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归下降解析器，把正则文本解析为 {@link Node} 语法树。
 * 所有语法错误都抛出携带精确位置的 {@link RegexSyntaxException}。
 */
final class Parser {

    private final String pattern;
    private int pos;

    private Parser(String pattern) {
        this.pattern = pattern;
    }

    static Node parse(String pattern) {
        Parser parser = new Parser(pattern);
        Node root = parser.parseAlternation();
        if (!parser.eof()) {
            throw new RegexSyntaxException(parser.pos, "存在未配对的右括号 ')'");
        }
        return root;
    }

    private boolean eof() {
        return pos >= pattern.length();
    }

    private char peek() {
        return pattern.charAt(pos);
    }

    private static boolean isQuantifier(char c) {
        return c == '*' || c == '+' || c == '?';
    }

    /** alternation := concatenation ('|' concatenation)* */
    private Node parseAlternation() {
        List<Node> branches = new ArrayList<>();
        branches.add(parseConcatenation());
        while (!eof() && peek() == '|') {
            pos++;
            branches.add(parseConcatenation());
        }
        return branches.size() == 1 ? branches.get(0) : new Node.Alt(branches);
    }

    /** concatenation := repetition* ，遇 '|' 或 ')' 停止 */
    private Node parseConcatenation() {
        List<Node> nodes = new ArrayList<>();
        while (!eof() && peek() != '|' && peek() != ')') {
            nodes.add(parseRepetition());
        }
        if (nodes.isEmpty()) {
            return new Node.Empty();
        }
        return nodes.size() == 1 ? nodes.get(0) : new Node.Concat(nodes);
    }

    /** repetition := atom quantifier? ，连续两个量词视为非法 */
    private Node parseRepetition() {
        if (isQuantifier(peek())) {
            throw new RegexSyntaxException(pos,
                    "量词 '" + peek() + "' 前面没有可修饰的表达式");
        }
        Node atom = parseAtom();
        if (!eof() && isQuantifier(peek())) {
            char q = peek();
            pos++;
            Node.Repeat.Kind kind = switch (q) {
                case '*' -> Node.Repeat.Kind.STAR;
                case '+' -> Node.Repeat.Kind.PLUS;
                default -> Node.Repeat.Kind.OPTIONAL;
            };
            atom = new Node.Repeat(atom, kind);
            if (!eof() && isQuantifier(peek())) {
                throw new RegexSyntaxException(pos,
                        "量词 '" + peek() + "' 紧跟在另一个量词之后，位置非法");
            }
        }
        return atom;
    }

    private Node parseAtom() {
        char c = peek();
        switch (c) {
            case '(' -> {
                int openPos = pos;
                pos++;
                Node inner = parseAlternation();
                if (eof() || peek() != ')') {
                    throw new RegexSyntaxException(openPos, "左括号 '(' 未闭合，缺少对应的 ')'");
                }
                pos++;
                return inner;
            }
            case '[' -> {
                return parseCharClass();
            }
            case '.' -> {
                pos++;
                return new Node.CharClassNode(CharClass.any());
            }
            case '\\' -> {
                return parseEscape();
            }
            default -> {
                pos++;
                return new Node.Literal(c);
            }
        }
    }

    /** 字符类外的转义：\d \D \w \W \s \S、\n \t \r \f，其余字符按字面量处理。 */
    private Node parseEscape() {
        int slashPos = pos;
        pos++;
        if (eof()) {
            throw new RegexSyntaxException(slashPos, "反斜杠 '\\' 出现在模式末尾，缺少被转义的字符");
        }
        char e = pattern.charAt(pos++);
        switch (e) {
            case 'd':
                return new Node.CharClassNode(CharClass.digit());
            case 'D':
                return new Node.CharClassNode(CharClass.notDigit());
            case 'w':
                return new Node.CharClassNode(CharClass.word());
            case 'W':
                return new Node.CharClassNode(CharClass.notWord());
            case 's':
                return new Node.CharClassNode(CharClass.space());
            case 'S':
                return new Node.CharClassNode(CharClass.notSpace());
            case 'n':
                return new Node.Literal('\n');
            case 't':
                return new Node.Literal('\t');
            case 'r':
                return new Node.Literal('\r');
            case 'f':
                return new Node.Literal('\f');
            default:
                return new Node.Literal(e);
        }
    }

    /** 字符类 := '[' '^'? item+ ']' ，item 为单字符、转义或 a-z 区间。 */
    private Node parseCharClass() {
        int openPos = pos;
        pos++; // 消费 '['
        boolean negated = false;
        if (!eof() && peek() == '^') {
            negated = true;
            pos++;
        }
        List<int[]> ranges = new ArrayList<>();
        boolean closed = false;
        while (!eof()) {
            char c = peek();
            if (c == ']') {
                pos++;
                closed = true;
                break;
            }
            if (c == '\\') {
                pos++;
                if (eof()) {
                    break; // 循环结束后统一报“未闭合”
                }
                char e = pattern.charAt(pos++);
                switch (e) {
                    case 'd' -> ranges.add(new int[]{'0', '9'});
                    case 'w' -> {
                        ranges.add(new int[]{'0', '9'});
                        ranges.add(new int[]{'A', 'Z'});
                        ranges.add(new int[]{'_', '_'});
                        ranges.add(new int[]{'a', 'z'});
                    }
                    case 's' -> {
                        ranges.add(new int[]{' ', ' '});
                        ranges.add(new int[]{'\t', '\n'});
                        ranges.add(new int[]{0x0B, 0x0B});
                        ranges.add(new int[]{'\f', '\f'});
                        ranges.add(new int[]{'\r', '\r'});
                    }
                    case 'n' -> addRangeOrSingle(ranges, '\n', openPos);
                    case 't' -> addRangeOrSingle(ranges, '\t', openPos);
                    case 'r' -> addRangeOrSingle(ranges, '\r', openPos);
                    case 'f' -> addRangeOrSingle(ranges, '\f', openPos);
                    default -> addRangeOrSingle(ranges, e, openPos);
                }
            } else {
                pos++;
                addRangeOrSingle(ranges, c, openPos);
            }
        }
        if (!closed) {
            throw new RegexSyntaxException(openPos, "字符类 '[' 未闭合，缺少对应的 ']'");
        }
        if (ranges.isEmpty()) {
            throw new RegexSyntaxException(openPos, "字符类为空，至少需要包含一个字符");
        }
        return new Node.CharClassNode(new CharClass(negated, ranges));
    }

    /** 读取一个字符后，若其后跟 '-' 且 '-' 后不是 ']'，则按区间解析。 */
    private void addRangeOrSingle(List<int[]> ranges, char lo, int openPos) {
        if (!eof() && peek() == '-' && pos + 1 < pattern.length() && pattern.charAt(pos + 1) != ']') {
            pos++; // 消费 '-'
            char hi;
            if (peek() == '\\') {
                pos++;
                if (eof()) {
                    throw new RegexSyntaxException(openPos, "字符类 '[' 未闭合，缺少对应的 ']'");
                }
                hi = pattern.charAt(pos++);
            } else {
                hi = pattern.charAt(pos++);
            }
            if (hi < lo) {
                throw new RegexSyntaxException(pos - 1,
                        "字符区间 '" + lo + "-" + hi + "' 非法：起点大于终点");
            }
            ranges.add(new int[]{lo, hi});
        } else {
            ranges.add(new int[]{lo, lo});
        }
    }
}
