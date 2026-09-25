package com.example.gsb.regex;

import java.util.List;
import java.util.Set;

/**
 * AST of a parsed regex pattern. Internal to the engine.
 */
sealed interface Node {

    record Literal(char c) implements Node {
    }

    /** The '.' metacharacter: matches any single char. */
    record Any() implements Node {
    }

    /** A character class such as [abc], [a-z] or [^0-9]. */
    record CharClass(boolean negated, Set<Character> chars) implements Node {
        boolean test(char c) {
            return negated != chars.contains(c);
        }
    }

    record Concat(List<Node> nodes) implements Node {
    }

    record Alt(List<Node> branches) implements Node {
    }

    enum Quantifier {
        STAR, PLUS, QUESTION
    }

    record Repeat(Node child, Quantifier quantifier) implements Node {
    }
}
