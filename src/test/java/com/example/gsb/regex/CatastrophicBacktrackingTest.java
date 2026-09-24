package com.example.gsb.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * 证明引擎对灾难性回溯模式免疫：NFA 模拟的耗时与输入长度成线性关系，
 * 而 JDK 回溯引擎在这些模式上是指数量级。
 */
class CatastrophicBacktrackingTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Test
    void nestedPlusDoesNotExplode() {
        Regex.Pattern p = Regex.compile("(a+)+b");
        String input = "a".repeat(100_000); // 故意不给结尾的 b
        assertTimeoutPreemptively(TIMEOUT,
                () -> assertThat(p.matches(input)).isFalse());
    }

    @Test
    void nestedPlusInFindMode() {
        Regex.Pattern p = Regex.compile("(a+)+b");
        String input = "a".repeat(50_000);
        assertTimeoutPreemptively(TIMEOUT,
                () -> assertThat(p.find(input)).isEmpty());
    }

    @Test
    void ambiguousAlternationDoesNotExplode() {
        Regex.Pattern p = Regex.compile("(a|a)*b");
        String input = "a".repeat(100_000);
        assertTimeoutPreemptively(TIMEOUT,
                () -> assertThat(p.matches(input)).isFalse());
    }

    @Test
    void nestedStarOfNullableDoesNotExplode() {
        Regex.Pattern p = Regex.compile("((a)*)*c");
        String input = "a".repeat(50_000);
        assertTimeoutPreemptively(TIMEOUT,
                () -> assertThat(p.matches(input)).isFalse());
    }

    @Test
    void longInputStillMatchesWhenItShould() {
        Regex.Pattern p = Regex.compile("(a+)+b");
        String input = "a".repeat(100_000) + "b";
        assertTimeoutPreemptively(TIMEOUT,
                () -> assertThat(p.matches(input)).isTrue());
    }

    @Test
    void findLocatesMatchInLongInput() {
        Regex.Pattern p = Regex.compile("(a+)+b");
        String input = "a".repeat(50_000) + "b";
        assertTimeoutPreemptively(TIMEOUT, () -> {
            var m = p.find(input);
            assertThat(m).isPresent();
            assertThat(m.get().start()).isEqualTo(0);
            assertThat(m.get().end()).isEqualTo(50_001);
        });
    }
}
