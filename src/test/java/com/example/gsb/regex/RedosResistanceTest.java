package com.example.gsb.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Proves that patterns which cause catastrophic backtracking in
 * java.util.regex finish quickly here, because the engine simulates a
 * Thompson NFA instead of backtracking.
 */
class RedosResistanceTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void nestedQuantifiersOnLongNonMatchingInput() {
        // Classic catastrophic case: (a+)+b against a long run of 'a' with no 'b'.
        Regex regex = Regex.compile("(a+)+b");
        String input = "a".repeat(100_000);
        assertTimeoutPreemptively(TIMEOUT, () ->
                assertThat(regex.matches(input)).isFalse());
    }

    @Test
    void nestedQuantifiersOnLongMatchingInput() {
        Regex regex = Regex.compile("(a+)+b");
        String input = "a".repeat(100_000) + "b";
        assertTimeoutPreemptively(TIMEOUT, () ->
                assertThat(regex.matches(input)).isTrue());
    }

    @Test
    void ambiguousAlternationStar() {
        Regex regex = Regex.compile("(a|a)*b");
        String input = "a".repeat(100_000);
        assertTimeoutPreemptively(TIMEOUT, () ->
                assertThat(regex.matches(input)).isFalse());
    }

    @Test
    void overlappingAlternationPlus() {
        Regex regex = Regex.compile("(a|aa)+b");
        String input = "a".repeat(100_000);
        assertTimeoutPreemptively(TIMEOUT, () ->
                assertThat(regex.matches(input)).isFalse());
    }

    @Test
    void findModeIsAlsoLinear() {
        Regex regex = Regex.compile("(a+)+b");
        String input = "a".repeat(100_000);
        assertTimeoutPreemptively(TIMEOUT, () -> {
            Optional<MatchResult> result = regex.find(input);
            assertThat(result).isEmpty();
        });
    }

    @Test
    void nestedStarOfNullableGroup() {
        Regex regex = Regex.compile("(a*)*c");
        String input = "a".repeat(100_000);
        assertTimeoutPreemptively(TIMEOUT, () ->
                assertThat(regex.matches(input)).isFalse());
    }
}
