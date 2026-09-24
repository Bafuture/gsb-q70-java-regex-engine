package com.example.gsb.regex;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归下降解析器，把模式串解析为 AST。
 *
 * <pre>
 * alternation := concat ('|' concat)*
 * concat      := repeat*
 * repeat      := atom ('*' | '+' | '?')?
 * atom        := '(' alternation ')' | charClass | '.' | literal
 * </pre>
 */
final class Parser {

    private final String pattern;
    private int pos;

    private Parser(String pattern) {
        this.pattern = pattern;
    }

    static Node parse(String pattern) {
        Parser parser = new Parser(pattern);
        Node node = parser.parseAlternation();
        if (parser.pos < parser.pattern.length()) {
            throw new RegexSyntaxException("Unmatched ')'", parser.pos);
        }
        return node;
    }

    private Node parseAlternation() {
        List<Node> branches = new ArrayList<>();
        branches.add(parseConcat());
        while (pos < pattern.length() && pattern.charAt(pos) == '|') {
            pos++;
            branches.add(parseConcat());
        }
        return branches.size() == 1 ? branches.get(0) : new Node.Alt(branches);
    }

    private Node parseConcat() {
        List<Node> nodes = new ArrayList<>();
        while (pos < pattern.length() && pattern.charAt(pos) != '|' && pattern.charAt(pos) != ')') {
            nodes.add(parseRepeat());
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return new Node.Concat(nodes);
    }

    private Node parseRepeat() {
        Node atom = parseAtom();
        boolean quantified = false;
        while (pos < pattern.length() && isQuantifier(pattern.charAt(pos))) {
            char q = pattern.charAt(pos);
            if (quantified) {
                throw new RegexSyntaxException("Quantifier '" + q + "' follows another quantifier", pos);
            }
            quantified = true;
            pos++;
            atom = switch (q) {
                case '*' -> new Node.Repeat(atom, Node.Repeat.Kind.STAR);
                case '+' -> new Node.Repeat(atom, Node.Repeat.Kind.PLUS);
                default -> new Node.Repeat(atom, Node.Repeat.Kind.QUESTION);
            };
        }
        return atom;
    }

    private Node parseAtom() {
        char c = pattern.charAt(pos);
        switch (c) {
            case '(' -> {
                int open = pos;
                pos++;
                Node inner = parseAlternation();
                if (pos >= pattern.length() || pattern.charAt(pos) != ')') {
                    throw new RegexSyntaxException("Unclosed group, '(' opened here", open);
                }
                pos++;
                return inner;
            }
            case '[' -> {
                return parseCharClass();
            }
            case '.' -> {
                pos++;
                return new Node.Any();
            }
            case '*', '+', '?' -> throw new RegexSyntaxException("Quantifier '" + c + "' has no target", pos);
            default -> {
                pos++;
                return new Node.Literal(c);
            }
        }
    }

    private Node parseCharClass() {
        int open = pos;
        pos++;
        boolean negated = false;
        if (pos < pattern.length() && pattern.charAt(pos) == '^') {
            negated = true;
            pos++;
        }
        if (pos < pattern.length() && pattern.charAt(pos) == ']') {
            throw new RegexSyntaxException("Empty character class", open);
        }
        List<int[]> ranges = new ArrayList<>();
        while (true) {
            if (pos >= pattern.length()) {
                throw new RegexSyntaxException("Unclosed character class, '[' opened here", open);
            }
            char lo = pattern.charAt(pos);
            if (lo == ']') {
                pos++;
                break;
            }
            pos++;
            if (pos + 1 < pattern.length() && pattern.charAt(pos) == '-' && pattern.charAt(pos + 1) != ']') {
                char hi = pattern.charAt(pos + 1);
                pos += 2;
                if (hi < lo) {
                    throw new RegexSyntaxException(
                            "Invalid range '" + lo + "-" + hi + "' in character class", open);
                }
                ranges.add(new int[] {lo, hi});
            } else {
                ranges.add(new int[] {lo, lo});
            }
        }
        return new Node.CharClassNode(negated, ranges);
    }

    private static boolean isQuantifier(char c) {
        return c == '*' || c == '+' || c == '?';
    }
}
