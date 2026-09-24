package com.example.gsb.regex;

import java.util.Objects;
import java.util.Optional;

/**
 * 自研正则表达式引擎入口。内部使用 Thompson 构造把模式编译为 NFA，
 * 匹配时做状态集模拟，不使用回溯，因此不存在灾难性回溯（ReDoS）。
 *
 * <p>支持语法：字面量、{@code .}、字符类 {@code [abc]} 与区间 {@code [a-z]}、
 * 取反字符类 {@code [^...]}、量词 {@code * + ?}、分组 {@code (...)} 与交替 {@code |}。
 *
 * <p>用法：
 * <pre>{@code
 * Regex re = Regex.compile("(a+)+b");
 * boolean ok = re.matches("aab");            // 整串匹配
 * Optional<MatchResult> m = re.find("xxaab"); // 查找，返回 [start, end)
 * }</pre>
 */
public final class Regex {

    private final String pattern;
    private final Nfa nfa;

    private Regex(String pattern, Nfa nfa) {
        this.pattern = pattern;
        this.nfa = nfa;
    }

    /** 使用默认上限（1 万状态 / 100 万字符输入）编译模式。 */
    public static Regex compile(String pattern) {
        return compile(pattern, RegexOptions.DEFAULT);
    }

    /**
     * 编译模式。
     *
     * @throws RegexSyntaxException         括号不配对、量词位置非法、字符类未闭合等，含精确位置
     * @throws RegexLimitExceededException  编译产生的 NFA 状态数超过上限
     */
    public static Regex compile(String pattern, RegexOptions options) {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(options, "options");
        Node ast = Parser.parse(pattern);
        Nfa nfa = new NfaCompiler(options.maxStates()).compile(ast, options.maxInputLength());
        return new Regex(pattern, nfa);
    }

    /**
     * 整串匹配：整个输入必须被模式完全消费。
     *
     * @throws RegexLimitExceededException 输入长度超过上限
     */
    public boolean matches(String input) {
        Objects.requireNonNull(input, "input");
        return nfa.matches(input);
    }

    /**
     * 查找：返回最左匹配（起点相同取最长）的 [start, end) 下标，无匹配返回空。
     *
     * @throws RegexLimitExceededException 输入长度超过上限
     */
    public Optional<MatchResult> find(String input) {
        Objects.requireNonNull(input, "input");
        return nfa.find(input);
    }

    public String pattern() {
        return pattern;
    }

    /** 编译后 NFA 的状态数，可用于监控规则复杂度。 */
    public int stateCount() {
        return nfa.stateCount();
    }
}
