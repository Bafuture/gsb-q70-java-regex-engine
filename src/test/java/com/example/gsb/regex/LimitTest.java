package com.example.gsb.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LimitTest {

    @Test
    void stateLimitExceededAtCompileTime() {
        RegexOptions options = new RegexOptions(10, 1000);
        assertThatThrownBy(() -> Regex.compile("a".repeat(100), options))
                .isInstanceOf(RegexLimitExceededException.class)
                .hasMessageContaining("状态数");
    }

    @Test
    void stateLimitAllowsSmallPatterns() {
        RegexOptions options = new RegexOptions(10, 1000);
        assertThat(Regex.compile("ab", options).matches("ab")).isTrue();
    }

    @Test
    void inputLengthLimitExceededAtMatchTime() {
        Regex.Pattern p = Regex.compile("a*", new RegexOptions(1000, 10));
        assertThatThrownBy(() -> p.matches("a".repeat(11)))
                .isInstanceOf(RegexLimitExceededException.class)
                .hasMessageContaining("输入长度");
        assertThatThrownBy(() -> p.find("a".repeat(11)))
                .isInstanceOf(RegexLimitExceededException.class)
                .hasMessageContaining("输入长度");
    }

    @Test
    void inputAtLimitIsAccepted() {
        Regex.Pattern p = Regex.compile("a*", new RegexOptions(1000, 10));
        assertThat(p.matches("a".repeat(10))).isTrue();
    }

    @Test
    void invalidOptionsRejected() {
        assertThatThrownBy(() -> new RegexOptions(0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegexOptions(10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
