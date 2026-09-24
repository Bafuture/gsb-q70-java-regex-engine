package com.example.gsb.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SyntaxErrorTest {

    private RegexSyntaxException errorOf(String pattern) {
        return org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> Regex.compile(pattern), RegexSyntaxException.class);
    }

    @Test
    void unclosedLeftParenReportsOpenPosition() {
        RegexSyntaxException e = errorOf("ab(cde");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(2);
        assertThat(e.getMessage()).contains("未闭合");
    }

    @Test
    void unmatchedRightParenReportsItsPosition() {
        RegexSyntaxException e = errorOf("abc)");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(3);
        assertThat(e.getMessage()).contains("右括号");
    }

    @Test
    void nestedUnclosedParen() {
        RegexSyntaxException e = errorOf("(a(b)c");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(0);
    }

    @Test
    void quantifierAtStartIsIllegal() {
        RegexSyntaxException e = errorOf("*abc");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(0);
        assertThat(e.getMessage()).contains("量词");
    }

    @Test
    void quantifierAfterLeftParenIsIllegal() {
        RegexSyntaxException e = errorOf("(+a)");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(1);
    }

    @Test
    void quantifierAfterAlternationIsIllegal() {
        RegexSyntaxException e = errorOf("a|?b");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(2);
    }

    @Test
    void doubleQuantifierIsIllegal() {
        RegexSyntaxException e = errorOf("a**");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(2);
        assertThat(e.getMessage()).contains("量词");
    }

    @Test
    void unclosedCharClassReportsOpenPosition() {
        RegexSyntaxException e = errorOf("ab[cd");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(2);
        assertThat(e.getMessage()).contains("字符类").contains("未闭合");
    }

    @Test
    void emptyCharClassIsIllegal() {
        RegexSyntaxException e = errorOf("[]");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(0);
        assertThat(e.getMessage()).contains("为空");
    }

    @Test
    void reversedRangeIsIllegal() {
        RegexSyntaxException e = errorOf("[z-a]");
        assertThat(e).isNotNull();
        assertThat(e.getMessage()).contains("区间");
    }

    @Test
    void trailingBackslashIsIllegal() {
        RegexSyntaxException e = errorOf("ab\\");
        assertThat(e).isNotNull();
        assertThat(e.position()).isEqualTo(2);
        assertThat(e.getMessage()).contains("反斜杠");
    }

    @Test
    void errorMessageContainsPositionAndReason() {
        assertThatThrownBy(() -> Regex.compile("(a"))
                .isInstanceOf(RegexSyntaxException.class)
                .hasMessageContaining("位置 0")
                .hasMessageContaining("未闭合");
    }
}
