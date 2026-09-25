package com.example.gsb.regex;

import java.util.ArrayList;
import java.util.List;

/**
 * A Thompson NFA: each state has epsilon transitions and char-consuming
 * transitions guarded by a character class. There is exactly one accept state.
 */
final class Nfa {

    static final class State {
        final int id;
        final List<State> epsilon = new ArrayList<>(2);
        final List<Edge> edges = new ArrayList<>(1);

        State(int id) {
            this.id = id;
        }
    }

    record Edge(Node.CharClass charClass, State to) {
    }

    final List<State> states;
    final State start;
    final State accept;

    Nfa(List<State> states, State start, State accept) {
        this.states = states;
        this.start = start;
        this.accept = accept;
    }
}
