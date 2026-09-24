package com.example.gsb.regex;

import java.util.Objects;
import java.util.Optional;

/**
 * 正则引擎入口。用法：
 *
 * <pre>{@code
 * Regex.Pattern p = Regex.compile("(a+)+b");
 * boolean ok = p.matches("aaab");
 * Optional<Regex.Match> m = p.find("xxaaabyy");
 * }</pre>
 *
 * 实现基于 Thompson 构造的 NFA 模拟，无回溯，不存在灾难性回溯问题。
 */
public final class Regex {

    private Regex() {
    }

    /** 使用默认资源上限编译。 */
    public static Pattern compile(String regex) {
        return compile(regex, RegexOptions.defaults());
    }

    /** 使用指定资源上限编译。语法错误抛 {@link RegexSyntaxException}。 */
    public static Pattern compile(String regex, RegexOptions options) {
        Objects.requireNonNull(regex, "regex");
        Objects.requireNonNull(options, "options");
        Node ast = Parser.parse(regex);
        Nfa nfa = NfaCompiler.compile(ast, options.maxStates());
        return new Pattern(regex, nfa, options);
    }

    /** 编译后的不可变模式，可安全地并发使用。 */
    public static final class Pattern {

        private final String source;
        private final Nfa nfa;
        private final RegexOptions options;

        private Pattern(String source, Nfa nfa, RegexOptions options) {
            this.source = source;
            this.nfa = nfa;
            this.options = options;
        }

        public String source() {
            return source;
        }

        public int stateCount() {
            return nfa.stateCount();
        }

        /** 整串匹配：整个输入必须被模式完整消费。 */
        public boolean matches(String input) {
            Objects.requireNonNull(input, "input");
            return new NfaMatcher(nfa, options.maxInputLength()).matches(input);
        }

        /** 查找第一个匹配（最左、同起点最长）。 */
        public Optional<Match> find(String input) {
            Objects.requireNonNull(input, "input");
            int[] span = new NfaMatcher(nfa, options.maxInputLength()).find(input);
            if (span == null) {
                return Optional.empty();
            }
            return Optional.of(new Match(span[0], span[1], input.substring(span[0], span[1])));
        }
    }

    /** 一次查找命中的结果：半开区间 [start, end) 及对应子串。 */
    public record Match(int start, int end, String group) {
    }
}
