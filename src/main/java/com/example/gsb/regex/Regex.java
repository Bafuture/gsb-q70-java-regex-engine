package com.example.gsb.regex;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;

/**
 * A compiled regular expression backed by a Thompson NFA.
 *
 * Matching is done by simulating the whole set of reachable NFA states while
 * scanning the input once (no backtracking), so runtime is O(states x input)
 * regardless of the pattern. Pathological patterns such as "(a+)+b" therefore
 * cannot cause exponential blowups.
 *
 * Instances are immutable and thread-safe.
 */
public final class Regex {

    private final String pattern;
    private final Nfa nfa;
    private final RegexOptions options;

    private Regex(String pattern, Nfa nfa, RegexOptions options) {
        this.pattern = pattern;
        this.nfa = nfa;
        this.options = options;
    }

    /**
     * Compiles a pattern with default safety limits.
     *
     * @throws RegexSyntaxException     if the pattern is malformed (position included)
     * @throws RegexTooComplexException if the NFA would exceed the state limit
     */
    public static Regex compile(String pattern) {
        return compile(pattern, RegexOptions.DEFAULT);
    }

    /** Compiles a pattern with explicit safety limits. */
    public static Regex compile(String pattern, RegexOptions options) {
        Node ast = new Parser(pattern).parse();
        Nfa nfa = new NfaCompiler(options.maxStates()).compile(ast);
        return new Regex(pattern, nfa, options);
    }

    public String pattern() {
        return pattern;
    }

    /** Number of NFA states this pattern compiled to. */
    public int stateCount() {
        return nfa.states.size();
    }

    /**
     * Full-string match: returns true iff the entire input matches the pattern.
     *
     * @throws RegexInputTooLongException if the input exceeds the configured limit
     */
    public boolean matches(String input) {
        checkInput(input);
        int stateCount = nfa.states.size();
        boolean[] current = new boolean[stateCount];
        addClosure(current, nfa.start);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            boolean[] next = new boolean[stateCount];
            boolean any = false;
            for (int s = 0; s < stateCount; s++) {
                if (!current[s]) {
                    continue;
                }
                for (Nfa.Edge edge : nfa.states.get(s).edges) {
                    if (edge.charClass().test(c)) {
                        addClosure(next, edge.to());
                        any = true;
                    }
                }
            }
            if (!any) {
                return false;
            }
            current = next;
        }
        return current[nfa.accept.id];
    }

    /**
     * Searches for the first (leftmost) match in the input. If several matches
     * start at the same position, the longest one is returned.
     *
     * @return the start (inclusive) and end (exclusive) indices of the match
     * @throws RegexInputTooLongException if the input exceeds the configured limit
     */
    public Optional<MatchResult> find(String input) {
        checkInput(input);
        int stateCount = nfa.states.size();
        // Thread set: value >= 0 is the start index of the thread occupying the
        // state, -1 means the state is not active. A smaller start always wins,
        // which yields leftmost-longest semantics in a single left-to-right scan.
        int[] current = new int[stateCount];
        Arrays.fill(current, -1);
        int bestStart = -1;
        int bestEnd = -1;
        for (int pos = 0; pos <= input.length(); pos++) {
            addClosure(current, nfa.start, pos);
            int acceptStart = current[nfa.accept.id];
            if (acceptStart >= 0) {
                if (bestStart < 0 || acceptStart < bestStart) {
                    bestStart = acceptStart;
                    bestEnd = pos;
                } else if (acceptStart == bestStart && pos > bestEnd) {
                    bestEnd = pos;
                }
            }
            if (bestStart >= 0 && !hasThreadStartedAtOrBefore(current, bestStart)) {
                break; // no thread can still improve the leftmost-longest match
            }
            if (pos == input.length()) {
                break;
            }
            char c = input.charAt(pos);
            int[] next = new int[stateCount];
            Arrays.fill(next, -1);
            for (int s = 0; s < stateCount; s++) {
                int threadStart = current[s];
                if (threadStart < 0) {
                    continue;
                }
                for (Nfa.Edge edge : nfa.states.get(s).edges) {
                    if (edge.charClass().test(c)) {
                        addClosure(next, edge.to(), threadStart);
                    }
                }
            }
            current = next;
        }
        return bestStart < 0
                ? Optional.empty()
                : Optional.of(new MatchResult(bestStart, bestEnd));
    }

    private boolean hasThreadStartedAtOrBefore(int[] threads, int start) {
        for (int threadStart : threads) {
            if (threadStart >= 0 && threadStart <= start) {
                return true;
            }
        }
        return false;
    }

    private void addClosure(boolean[] set, Nfa.State state) {
        Deque<Nfa.State> stack = new ArrayDeque<>();
        stack.push(state);
        while (!stack.isEmpty()) {
            Nfa.State s = stack.pop();
            if (set[s.id]) {
                continue;
            }
            set[s.id] = true;
            for (Nfa.State target : s.epsilon) {
                stack.push(target);
            }
        }
    }

    private void addClosure(int[] threads, Nfa.State state, int threadStart) {
        record Item(Nfa.State state, int threadStart) {
        }
        Deque<Item> stack = new ArrayDeque<>();
        stack.push(new Item(state, threadStart));
        while (!stack.isEmpty()) {
            Item item = stack.pop();
            int existing = threads[item.state().id];
            if (existing >= 0 && existing <= item.threadStart()) {
                continue; // an equal-or-earlier thread already occupies this state
            }
            threads[item.state().id] = item.threadStart();
            for (Nfa.State target : item.state().epsilon) {
                stack.push(new Item(target, item.threadStart()));
            }
        }
    }

    private void checkInput(String input) {
        if (input.length() > options.maxInputLength()) {
            throw new RegexInputTooLongException(
                    "Input length " + input.length()
                            + " exceeds the limit of " + options.maxInputLength());
        }
    }
}
