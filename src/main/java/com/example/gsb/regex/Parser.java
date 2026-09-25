package com.example.gsb.regex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recursive-descent parser producing a {@link Node} AST.
 *
 * Grammar:
 *   alternation := concat ('|' concat)*
 *   concat      := repeat*
 *   repeat      := atom ('*' | '+' | '?')?
 *   atom        := '(' alternation ')' | '[' class ']' | '.' | escape | literal
 *
 * All syntax errors are reported with the exact 0-based position in the pattern.
 */
final class Parser {

    private final String pattern;
    private int pos;

    Parser(String pattern) {
        this.pattern = pattern;
    }

    Node parse() {
        Node node = parseAlternation();
        if (pos < pattern.length()) {
            // Only ')' can stop the concat loop without being consumed.
            throw new RegexSyntaxException("Unmatched ')'", pos);
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
        while (pos < pattern.length()
                && pattern.charAt(pos) != '|'
                && pattern.charAt(pos) != ')') {
            nodes.add(parseRepeat());
        }
        if (nodes.isEmpty()) {
            return new Node.Concat(List.of()); // empty branch matches the empty string
        }
        return nodes.size() == 1 ? nodes.get(0) : new Node.Concat(nodes);
    }

    private Node parseRepeat() {
        Node atom = parseAtom();
        if (pos < pattern.length()) {
            Node.Quantifier quantifier = switch (pattern.charAt(pos)) {
                case '*' -> Node.Quantifier.STAR;
                case '+' -> Node.Quantifier.PLUS;
                case '?' -> Node.Quantifier.QUESTION;
                default -> null;
            };
            if (quantifier != null) {
                pos++;
                return new Node.Repeat(atom, quantifier);
            }
        }
        return atom;
    }

    private Node parseAtom() {
        char c = pattern.charAt(pos);
        switch (c) {
            case '(' -> {
                int open = pos++;
                Node inner = parseAlternation();
                if (pos >= pattern.length() || pattern.charAt(pos) != ')') {
                    throw new RegexSyntaxException("Unclosed group (missing ')')", open);
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
            case '\\' -> {
                int backslash = pos++;
                return new Node.Literal(parseEscape(backslash));
            }
            case '*', '+', '?' -> throw new RegexSyntaxException(
                    "Quantifier '" + c + "' has nothing to repeat", pos);
            default -> {
                pos++;
                return new Node.Literal(c);
            }
        }
    }

    private Node parseCharClass() {
        int open = pos++; // consume '['
        boolean negated = false;
        if (pos < pattern.length() && pattern.charAt(pos) == '^') {
            negated = true;
            pos++;
        }
        Set<Character> chars = new HashSet<>();
        boolean closed = false;
        while (pos < pattern.length()) {
            char c = pattern.charAt(pos);
            if (c == ']') {
                closed = true;
                pos++;
                break;
            }
            char lo = readClassChar();
            if (pos < pattern.length()
                    && pattern.charAt(pos) == '-'
                    && pos + 1 < pattern.length()
                    && pattern.charAt(pos + 1) != ']') {
                int dashPos = pos;
                pos++; // consume '-'
                char hi = readClassChar();
                if (lo > hi) {
                    throw new RegexSyntaxException(
                            "Invalid range '" + lo + "-" + hi + "' (start is greater than end)",
                            dashPos);
                }
                for (char ch = lo; ch <= hi; ch++) {
                    chars.add(ch);
                }
            } else {
                chars.add(lo);
            }
        }
        if (!closed) {
            throw new RegexSyntaxException("Unclosed character class (missing ']')", open);
        }
        return new Node.CharClass(negated, chars);
    }

    /** Reads one char inside a character class, resolving backslash escapes. */
    private char readClassChar() {
        if (pattern.charAt(pos) == '\\') {
            int backslash = pos++;
            return parseEscape(backslash);
        }
        return pattern.charAt(pos++);
    }

    private char parseEscape(int backslashPos) {
        if (pos >= pattern.length()) {
            throw new RegexSyntaxException("Trailing backslash", backslashPos);
        }
        char e = pattern.charAt(pos++);
        return switch (e) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            default -> e; // any other escaped char stands for itself, e.g. \. \* \[
        };
    }
}
