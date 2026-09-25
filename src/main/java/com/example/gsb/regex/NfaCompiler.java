package com.example.gsb.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Compiles a {@link Node} AST into an {@link Nfa} using Thompson's construction.
 * Every fragment has exactly one start and one accept state; fragments are
 * combined with epsilon transitions only, so the resulting automaton can be
 * simulated in O(states x input) time with no backtracking.
 */
final class NfaCompiler {

    /** A partially built automaton fragment with one entry and one exit. */
    private record Frag(Nfa.State start, Nfa.State accept) {
    }

    private final int maxStates;
    private final List<Nfa.State> states = new ArrayList<>();

    NfaCompiler(int maxStates) {
        this.maxStates = maxStates;
    }

    Nfa compile(Node node) {
        Frag frag = build(node);
        return new Nfa(states, frag.start(), frag.accept());
    }

    private Nfa.State newState() {
        if (states.size() >= maxStates) {
            throw new RegexTooComplexException(
                    "Pattern is too complex: NFA would exceed the limit of "
                            + maxStates + " states");
        }
        Nfa.State state = new Nfa.State(states.size());
        states.add(state);
        return state;
    }

    private Frag build(Node node) {
        if (node instanceof Node.Literal literal) {
            return charClassFrag(new Node.CharClass(false, Set.of(literal.c())));
        }
        if (node instanceof Node.Any) {
            return charClassFrag(new Node.CharClass(true, Set.of()));
        }
        if (node instanceof Node.CharClass charClass) {
            return charClassFrag(charClass);
        }
        if (node instanceof Node.Concat concat) {
            return buildConcat(concat.nodes());
        }
        if (node instanceof Node.Alt alt) {
            return buildAlt(alt.branches());
        }
        Node.Repeat repeat = (Node.Repeat) node;
        return buildRepeat(repeat.child(), repeat.quantifier());
    }

    private Frag charClassFrag(Node.CharClass charClass) {
        Nfa.State start = newState();
        Nfa.State accept = newState();
        start.edges.add(new Nfa.Edge(charClass, accept));
        return new Frag(start, accept);
    }

    private Frag epsilonFrag() {
        Nfa.State start = newState();
        Nfa.State accept = newState();
        start.epsilon.add(accept);
        return new Frag(start, accept);
    }

    private Frag buildConcat(List<Node> nodes) {
        if (nodes.isEmpty()) {
            return epsilonFrag();
        }
        Frag result = build(nodes.get(0));
        for (int i = 1; i < nodes.size(); i++) {
            Frag next = build(nodes.get(i));
            result.accept().epsilon.add(next.start());
            result = new Frag(result.start(), next.accept());
        }
        return result;
    }

    private Frag buildAlt(List<Node> branches) {
        Nfa.State start = newState();
        Nfa.State accept = newState();
        for (Node branch : branches) {
            Frag frag = build(branch);
            start.epsilon.add(frag.start());
            frag.accept().epsilon.add(accept);
        }
        return new Frag(start, accept);
    }

    private Frag buildRepeat(Node child, Node.Quantifier quantifier) {
        Frag body = build(child);
        Nfa.State start = newState();
        Nfa.State accept = newState();
        switch (quantifier) {
            case STAR -> {
                start.epsilon.add(body.start());
                start.epsilon.add(accept);
                body.accept().epsilon.add(body.start());
                body.accept().epsilon.add(accept);
            }
            case PLUS -> {
                start.epsilon.add(body.start());
                body.accept().epsilon.add(body.start());
                body.accept().epsilon.add(accept);
            }
            case QUESTION -> {
                start.epsilon.add(body.start());
                start.epsilon.add(accept);
                body.accept().epsilon.add(accept);
            }
        }
        return new Frag(start, accept);
    }
}
