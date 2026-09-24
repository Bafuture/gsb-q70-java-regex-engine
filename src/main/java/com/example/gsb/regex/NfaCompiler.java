package com.example.gsb.regex;

import java.util.ArrayList;
import java.util.List;

/**
 * Thompson 构造：把语法树编译为 ε-NFA。
 * build(node, next) 返回片段入口状态，片段的出口直接连到 next。
 */
final class NfaCompiler {

    private final List<Nfa.State> states = new ArrayList<>();
    private final int maxStates;

    private NfaCompiler(int maxStates) {
        this.maxStates = maxStates;
    }

    static Nfa compile(Node root, int maxStates) {
        NfaCompiler compiler = new NfaCompiler(maxStates);
        int accept = compiler.newState(Nfa.KIND_ACCEPT);
        int start = compiler.build(root, accept);
        return new Nfa(compiler.states, start);
    }

    private Nfa.State state(int index) {
        return states.get(index);
    }

    private int newState(int kind) {
        if (states.size() >= maxStates) {
            throw new RegexLimitExceededException(
                    "NFA 状态数超过上限 " + maxStates + "，请简化规则或提高 maxStates");
        }
        states.add(new Nfa.State(kind));
        return states.size() - 1;
    }

    private int build(Node node, int next) {
        if (node instanceof Node.Empty) {
            return next;
        }
        if (node instanceof Node.Literal lit) {
            int s = newState(Nfa.KIND_CHAR);
            state(s).test = CharClass.single(lit.ch());
            state(s).out1 = next;
            return s;
        }
        if (node instanceof Node.CharClassNode cn) {
            int s = newState(Nfa.KIND_CHAR);
            state(s).test = cn.charClass();
            state(s).out1 = next;
            return s;
        }
        if (node instanceof Node.Concat concat) {
            int entry = next;
            List<Node> nodes = concat.nodes();
            for (int i = nodes.size() - 1; i >= 0; i--) {
                entry = build(nodes.get(i), entry);
            }
            return entry;
        }
        if (node instanceof Node.Alt alt) {
            List<Node> branches = alt.branches();
            int entry = build(branches.get(branches.size() - 1), next);
            for (int i = branches.size() - 2; i >= 0; i--) {
                int branch = build(branches.get(i), next);
                int split = newState(Nfa.KIND_SPLIT);
                state(split).out1 = branch;
                state(split).out2 = entry;
                entry = split;
            }
            return entry;
        }
        if (node instanceof Node.Repeat repeat) {
            Node.Repeat.Kind kind = repeat.kind();
            if (kind == Node.Repeat.Kind.OPTIONAL) {
                int body = build(repeat.child(), next);
                int split = newState(Nfa.KIND_SPLIT);
                state(split).out1 = body;
                state(split).out2 = next;
                return split;
            }
            // STAR / PLUS：循环回边都进同一个 split
            int split = newState(Nfa.KIND_SPLIT);
            int body = build(repeat.child(), split);
            state(split).out1 = body;
            state(split).out2 = next;
            return kind == Node.Repeat.Kind.STAR ? split : body;
        }
        throw new IllegalStateException("未知语法节点: " + node);
    }
}
