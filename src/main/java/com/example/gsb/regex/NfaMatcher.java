package com.example.gsb.regex;

import java.util.Arrays;

/**
 * NFA 模拟执行：维护“当前可达状态集”，逐字符推进。
 * 完全不存在回溯，时间复杂度 O(状态数 × 输入长度)，因此对
 * (a+)+b 这类灾难性回溯模式是线性安全的。
 */
final class NfaMatcher {

    private final Nfa nfa;
    private final int maxInputLength;

    NfaMatcher(Nfa nfa, int maxInputLength) {
        this.nfa = nfa;
        this.maxInputLength = maxInputLength;
    }

    private void checkLength(String input) {
        if (input.length() > maxInputLength) {
            throw new RegexLimitExceededException(
                    "输入长度 " + input.length() + " 超过上限 " + maxInputLength);
        }
    }

    /** 整串匹配：输入全部消费完毕且到达接受状态。 */
    boolean matches(String input) {
        checkLength(input);
        int stateCount = nfa.stateCount();
        int[] mark = new int[stateCount];
        int[] cur = new int[stateCount];
        int[] next = new int[stateCount];
        IntStack stack = new IntStack(stateCount);
        int gen = 1;

        int curSize = closure(nfa.start, mark, gen, cur, stack);

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            gen++;
            int nextSize = 0;
            for (int j = 0; j < curSize; j++) {
                Nfa.State st = nfa.states.get(cur[j]);
                if (st.kind == Nfa.KIND_CHAR && st.test.matches(c)
                        && mark[st.out1] != gen) {
                    mark[st.out1] = gen;
                    stack.push(st.out1);
                }
            }
            while (!stack.isEmpty()) {
                int s = stack.pop();
                Nfa.State st = nfa.states.get(s);
                if (st.kind == Nfa.KIND_SPLIT) {
                    if (mark[st.out1] != gen) {
                        mark[st.out1] = gen;
                        stack.push(st.out1);
                    }
                    if (mark[st.out2] != gen) {
                        mark[st.out2] = gen;
                        stack.push(st.out2);
                    }
                } else {
                    next[nextSize++] = s;
                }
            }
            int[] tmp = cur;
            cur = next;
            next = tmp;
            curSize = nextSize;
            if (curSize == 0) {
                return false;
            }
        }

        for (int j = 0; j < curSize; j++) {
            if (nfa.states.get(cur[j]).kind == Nfa.KIND_ACCEPT) {
                return true;
            }
        }
        return false;
    }

    /** 从种子状态做 ε-闭包，非 SPLIT 状态写入 out，返回写入数量。 */
    private int closure(int seed, int[] mark, int gen, int[] out, IntStack stack) {
        mark[seed] = gen;
        stack.push(seed);
        int outSize = 0;
        while (!stack.isEmpty()) {
            int s = stack.pop();
            Nfa.State st = nfa.states.get(s);
            if (st.kind == Nfa.KIND_SPLIT) {
                if (mark[st.out1] != gen) {
                    mark[st.out1] = gen;
                    stack.push(st.out1);
                }
                if (mark[st.out2] != gen) {
                    mark[st.out2] = gen;
                    stack.push(st.out2);
                }
            } else {
                out[outSize++] = s;
            }
        }
        return outSize;
    }

    /**
     * 查找第一个匹配。语义为“最左、同起点最长”：
     * 起点最小者优先；起点相同时取更长的匹配。
     * 返回 {start, end}（半开区间），不存在返回 null。
     */
    int[] find(String input) {
        checkLength(input);
        int stateCount = nfa.stateCount();
        int[] mark = new int[stateCount];
        int[] closedMark = new int[stateCount];
        int[] minStart = new int[stateCount];
        int[] cur = new int[stateCount];
        int[] closed = new int[stateCount];
        int[] next = new int[stateCount];
        IntStack stack = new IntStack(stateCount);
        int gen = 0;
        int curSize = 0;
        int bestStart = -1;
        int bestEnd = -1;
        int len = input.length();

        for (int i = 0; i <= len; i++) {
            gen++;
            int closedSize = 0;

            // 携带上一步的状态，保留各自的最小起点
            for (int j = 0; j < curSize; j++) {
                int s = cur[j];
                if (mark[s] != gen) {
                    mark[s] = gen;
                    stack.push(s);
                }
            }
            // 每个位置都重新允许从 NFA 起点开始匹配
            mark[nfa.start] = gen;
            minStart[nfa.start] = i;
            stack.push(nfa.start);

            // ε-闭包，带“最小起点”松弛传播
            while (!stack.isEmpty()) {
                int s = stack.pop();
                Nfa.State st = nfa.states.get(s);
                if (st.kind == Nfa.KIND_SPLIT) {
                    relax(st.out1, minStart[s], gen, mark, minStart, stack);
                    relax(st.out2, minStart[s], gen, mark, minStart, stack);
                } else if (closedMark[s] != gen) {
                    closedMark[s] = gen;
                    closed[closedSize++] = s;
                }
            }

            for (int j = 0; j < closedSize; j++) {
                int s = closed[j];
                if (nfa.states.get(s).kind == Nfa.KIND_ACCEPT) {
                    int candidateStart = minStart[s];
                    if (bestStart == -1 || candidateStart < bestStart
                            || (candidateStart == bestStart && i > bestEnd)) {
                        bestStart = candidateStart;
                        bestEnd = i;
                    }
                }
            }

            if (i == len) {
                break;
            }

            // 消费当前字符，得到下一位置的种子状态集
            char c = input.charAt(i);
            gen++;
            int nextSize = 0;
            for (int j = 0; j < closedSize; j++) {
                int s = closed[j];
                Nfa.State st = nfa.states.get(s);
                if (st.kind == Nfa.KIND_CHAR && st.test.matches(c)) {
                    int o = st.out1;
                    if (mark[o] != gen) {
                        mark[o] = gen;
                        minStart[o] = minStart[s];
                        next[nextSize++] = o;
                    } else if (minStart[s] < minStart[o]) {
                        minStart[o] = minStart[s];
                    }
                }
            }
            int[] tmp = cur;
            cur = next;
            next = tmp;
            curSize = nextSize;
        }

        return bestStart == -1 ? null : new int[]{bestStart, bestEnd};
    }

    private void relax(int target, int value, int gen, int[] mark,
                       int[] minStart, IntStack stack) {
        if (mark[target] != gen) {
            mark[target] = gen;
            minStart[target] = value;
            stack.push(target);
        } else if (value < minStart[target]) {
            minStart[target] = value;
            stack.push(target);
        }
    }

    /** 自动扩容的 int 栈：松弛传播时同一状态可能被多次压栈。 */
    private static final class IntStack {
        private int[] data;
        private int size;

        IntStack(int capacity) {
            this.data = new int[Math.max(capacity, 16)];
        }

        void push(int value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = value;
        }

        int pop() {
            return data[--size];
        }

        boolean isEmpty() {
            return size == 0;
        }
    }
}
