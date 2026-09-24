package com.example.gsb.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Thompson 构造：把 AST 编译为 NFA。每个 AST 节点对应一个片段（入口态 + 出口态），
 * 通过 epsilon 边组合，构造过程不回溯、与模式长度线性相关。
 */
final class NfaCompiler {

    private final int maxStates;
    private final List<Nfa.State> states = new ArrayList<>();

    NfaCompiler(int maxStates) {
        this.maxStates = maxStates;
    }

    Nfa compile(Node node, int maxInputLength) {
        Nfa.State start = newState();
        Nfa.State accept = newState();
        build(node, start, accept);
        return new Nfa(states, start, accept, maxInputLength);
    }

    private void build(Node node, Nfa.State start, Nfa.State accept) {
        if (node instanceof Node.Literal literal) {
            start.edges.add(new Nfa.Edge(c -> c == literal.c(), accept));
        } else if (node instanceof Node.Any) {
            start.edges.add(new Nfa.Edge(c -> true, accept));
        } else if (node instanceof Node.CharClassNode charClass) {
            start.edges.add(new Nfa.Edge(classPredicate(charClass), accept));
        } else if (node instanceof Node.Concat concat) {
            if (concat.nodes().isEmpty()) {
                start.epsilon.add(accept);
                return;
            }
            Nfa.State current = start;
            for (int i = 0; i < concat.nodes().size(); i++) {
                Nfa.State next = i == concat.nodes().size() - 1 ? accept : newState();
                build(concat.nodes().get(i), current, next);
                current = next;
            }
        } else if (node instanceof Node.Alt alt) {
            for (Node branch : alt.branches()) {
                Nfa.State branchStart = newState();
                Nfa.State branchAccept = newState();
                start.epsilon.add(branchStart);
                build(branch, branchStart, branchAccept);
                branchAccept.epsilon.add(accept);
            }
        } else if (node instanceof Node.Repeat repeat) {
            Nfa.State subStart = newState();
            Nfa.State subAccept = newState();
            start.epsilon.add(subStart);
            if (repeat.kind() != Node.Repeat.Kind.PLUS) {
                start.epsilon.add(accept);
            }
            build(repeat.inner(), subStart, subAccept);
            subAccept.epsilon.add(accept);
            if (repeat.kind() != Node.Repeat.Kind.QUESTION) {
                subAccept.epsilon.add(subStart);
            }
        } else {
            throw new IllegalArgumentException("Unknown node: " + node);
        }
    }

    private Nfa.State newState() {
        if (states.size() >= maxStates) {
            throw new RegexLimitExceededException(
                    "Pattern requires more than " + maxStates + " NFA states (limit exceeded)");
        }
        Nfa.State state = new Nfa.State(states.size());
        states.add(state);
        return state;
    }

    private static IntPredicate classPredicate(Node.CharClassNode charClass) {
        List<int[]> ranges = List.copyOf(charClass.ranges());
        boolean negated = charClass.negated();
        return c -> {
            boolean in = false;
            for (int[] range : ranges) {
                if (c >= range[0] && c <= range[1]) {
                    in = true;
                    break;
                }
            }
            return negated != in;
        };
    }
}
