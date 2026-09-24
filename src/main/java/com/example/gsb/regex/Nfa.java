package com.example.gsb.regex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * Thompson 构造得到的 NFA，以及基于状态集模拟（无回溯）的执行器。
 */
final class Nfa {

    record Edge(IntPredicate test, State target) {
    }

    static final class State {
        final int id;
        final List<State> epsilon = new ArrayList<>(2);
        final List<Edge> edges = new ArrayList<>(1);

        State(int id) {
            this.id = id;
        }
    }

    private final List<State> states;
    private final State start;
    private final State accept;
    private final int maxInputLength;

    Nfa(List<State> states, State start, State accept, int maxInputLength) {
        this.states = states;
        this.start = start;
        this.accept = accept;
        this.maxInputLength = maxInputLength;
    }

    int stateCount() {
        return states.size();
    }

    /** 整串匹配：从起始状态出发，消费完全部字符后恰好处于接受状态。 */
    boolean matches(String input) {
        checkLength(input);
        boolean[] cur = new boolean[states.size()];
        boolean[] next = new boolean[states.size()];
        List<Integer> curActive = new ArrayList<>();
        List<Integer> nextActive = new ArrayList<>();
        Deque<Integer> stack = new ArrayDeque<>();
        addWithClosure(start.id, cur, curActive, stack);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            for (int id : nextActive) {
                next[id] = false;
            }
            nextActive.clear();
            for (int id : curActive) {
                for (Edge edge : states.get(id).edges) {
                    if (edge.test().test(c)) {
                        addWithClosure(edge.target().id, next, nextActive, stack);
                    }
                }
            }
            if (nextActive.isEmpty()) {
                return false;
            }
            boolean[] tmpSet = cur;
            cur = next;
            next = tmpSet;
            List<Integer> tmpList = curActive;
            curActive = nextActive;
            nextActive = tmpList;
        }
        return cur[accept.id];
    }

    /**
     * 查找：返回最左匹配；起点相同时取最长（POSIX 风格 leftmost-longest）。
     * 每个活跃状态携带其可达的最早起始下标，单次扫描完成，复杂度 O(n * m)。
     */
    Optional<MatchResult> find(String input) {
        checkLength(input);
        int n = states.size();
        boolean[] cur = new boolean[n];
        boolean[] next = new boolean[n];
        int[] curTag = new int[n];
        int[] nextTag = new int[n];
        List<Integer> curActive = new ArrayList<>();
        List<Integer> nextActive = new ArrayList<>();
        Deque<int[]> stack = new ArrayDeque<>();
        int bestStart = -1;
        int bestEnd = -1;
        for (int pos = 0; pos <= input.length(); pos++) {
            addWithClosure(start.id, pos, cur, curTag, curActive, stack);
            if (cur[accept.id]) {
                int tag = curTag[accept.id];
                if (bestStart == -1 || tag < bestStart || (tag == bestStart && pos > bestEnd)) {
                    bestStart = tag;
                    bestEnd = pos;
                }
            }
            if (pos == input.length()) {
                break;
            }
            char c = input.charAt(pos);
            for (int id : nextActive) {
                next[id] = false;
            }
            nextActive.clear();
            for (int id : curActive) {
                for (Edge edge : states.get(id).edges) {
                    if (edge.test().test(c)) {
                        addWithClosure(edge.target().id, curTag[id], next, nextTag, nextActive, stack);
                    }
                }
            }
            boolean[] tmpSet = cur;
            cur = next;
            next = tmpSet;
            int[] tmpTag = curTag;
            curTag = nextTag;
            nextTag = tmpTag;
            List<Integer> tmpList = curActive;
            curActive = nextActive;
            nextActive = tmpList;
        }
        return bestStart < 0 ? Optional.empty() : Optional.of(new MatchResult(bestStart, bestEnd));
    }

    private void checkLength(String input) {
        if (input.length() > maxInputLength) {
            throw new RegexLimitExceededException(
                    "Input length " + input.length() + " exceeds the limit of " + maxInputLength);
        }
    }

    private void addWithClosure(int id, boolean[] set, List<Integer> active, Deque<Integer> stack) {
        if (!set[id]) {
            set[id] = true;
            active.add(id);
            stack.push(id);
        }
        while (!stack.isEmpty()) {
            State s = states.get(stack.pop());
            for (State e : s.epsilon) {
                if (!set[e.id]) {
                    set[e.id] = true;
                    active.add(e.id);
                    stack.push(e.id);
                }
            }
        }
    }

    private void addWithClosure(int id, int tag, boolean[] set, int[] tags, List<Integer> active,
            Deque<int[]> stack) {
        if (!set[id] || tag < tags[id]) {
            if (!set[id]) {
                active.add(id);
            }
            set[id] = true;
            tags[id] = tag;
            stack.push(new int[] {id, tag});
        }
        while (!stack.isEmpty()) {
            int[] item = stack.pop();
            State s = states.get(item[0]);
            for (State e : s.epsilon) {
                if (!set[e.id] || item[1] < tags[e.id]) {
                    if (!set[e.id]) {
                        active.add(e.id);
                    }
                    set[e.id] = true;
                    tags[e.id] = item[1];
                    stack.push(new int[] {e.id, item[1]});
                }
            }
        }
    }
}
