package com.example.gsb.regex;

import java.util.List;

/**
 * Thompson 构造产生的 ε-NFA。
 * 状态只有三种：CHAR（带字符谓词，单出边）、SPLIT（两条 ε 出边）、ACCEPT。
 */
final class Nfa {

    static final int KIND_CHAR = 0;
    static final int KIND_SPLIT = 1;
    static final int KIND_ACCEPT = 2;

    static final class State {
        final int kind;
        CharClass test;
        int out1 = -1;
        int out2 = -1;

        State(int kind) {
            this.kind = kind;
        }
    }

    final List<State> states;
    final int start;

    Nfa(List<State> states, int start) {
        this.states = states;
        this.start = start;
    }

    int stateCount() {
        return states.size();
    }
}
