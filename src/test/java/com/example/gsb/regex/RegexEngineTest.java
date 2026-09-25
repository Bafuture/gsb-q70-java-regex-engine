package com.example.gsb.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RegexEngineTest {

    @Nested
    class LiteralsAndDot {

        @Test
        void literalMatchesWholeStringOnly() {
            Regex regex = Regex.compile("abc");
            assertThat(regex.matches("abc")).isTrue();
            assertThat(regex.matches("abd")).isFalse();
            assertThat(regex.matches("ab")).isFalse();
            assertThat(regex.matches("abcabc")).isFalse();
        }

        @Test
        void dotMatchesAnySingleChar() {
            Regex regex = Regex.compile("a.c");
            assertThat(regex.matches("axc")).isTrue();
            assertThat(regex.matches("a\nc")).isTrue();
            assertThat(regex.matches("ac")).isFalse();
        }

        @Test
        void escapedMetacharsAreLiterals() {
            assertThat(Regex.compile("a\\.c").matches("a.c")).isTrue();
            assertThat(Regex.compile("a\\.c").matches("axc")).isFalse();
            assertThat(Regex.compile("\\(a\\)\\*").matches("(a)*")).isTrue();
            assertThat(Regex.compile("a\\nb").matches("a\nb")).isTrue();
        }

        @Test
        void emptyPatternMatchesOnlyEmptyString() {
            Regex regex = Regex.compile("");
            assertThat(regex.matches("")).isTrue();
            assertThat(regex.matches("a")).isFalse();
        }
    }

    @Nested
    class CharClasses {

        @Test
        void simpleClass() {
            Regex regex = Regex.compile("[abc]+");
            assertThat(regex.matches("cab")).isTrue();
            assertThat(regex.matches("abcabc")).isTrue();
            assertThat(regex.matches("abcd")).isFalse();
        }

        @Test
        void rangeClass() {
            Regex regex = Regex.compile("[a-z]+");
            assertThat(regex.matches("hello")).isTrue();
            assertThat(regex.matches("Hello")).isFalse();
            assertThat(Regex.compile("[a-zA-Z0-9_]+").matches("Var_9x")).isTrue();
        }

        @Test
        void negatedClass() {
            Regex regex = Regex.compile("[^0-9]+");
            assertThat(regex.matches("abc")).isTrue();
            assertThat(regex.matches("ab1")).isFalse();
        }

        @Test
        void dashAtEdgeIsLiteral() {
            assertThat(Regex.compile("[-a]+").matches("-a-")).isTrue();
            assertThat(Regex.compile("[a-]+").matches("a-a")).isTrue();
        }

        @Test
        void escapedBracketInsideClass() {
            assertThat(Regex.compile("[\\]a]+").matches("]a]")).isTrue();
        }
    }

    @Nested
    class Quantifiers {

        @Test
        void star() {
            Regex regex = Regex.compile("ab*c");
            assertThat(regex.matches("ac")).isTrue();
            assertThat(regex.matches("abc")).isTrue();
            assertThat(regex.matches("abbbc")).isTrue();
            assertThat(regex.matches("ab")).isFalse();
        }

        @Test
        void plus() {
            Regex regex = Regex.compile("ab+c");
            assertThat(regex.matches("ac")).isFalse();
            assertThat(regex.matches("abc")).isTrue();
            assertThat(regex.matches("abbc")).isTrue();
        }

        @Test
        void question() {
            Regex regex = Regex.compile("ab?c");
            assertThat(regex.matches("ac")).isTrue();
            assertThat(regex.matches("abc")).isTrue();
            assertThat(regex.matches("abbc")).isFalse();
        }

        @Test
        void quantifierAppliesToGroup() {
            Regex regex = Regex.compile("(ab)+c");
            assertThat(regex.matches("abc")).isTrue();
            assertThat(regex.matches("abababc")).isTrue();
            assertThat(regex.matches("c")).isFalse();
        }
    }

    @Nested
    class GroupsAndAlternation {

        @Test
        void alternation() {
            Regex regex = Regex.compile("cat|dog");
            assertThat(regex.matches("cat")).isTrue();
            assertThat(regex.matches("dog")).isTrue();
            assertThat(regex.matches("cow")).isFalse();
        }

        @Test
        void groupedAlternation() {
            Regex regex = Regex.compile("a(b|c)*d");
            assertThat(regex.matches("ad")).isTrue();
            assertThat(regex.matches("abcbd")).isTrue();
            assertThat(regex.matches("abed")).isFalse();
        }

        @Test
        void nestedGroups() {
            Regex regex = Regex.compile("((a|b)+c)+d");
            assertThat(regex.matches("abacd")).isTrue();
            assertThat(regex.matches("bacbacd")).isTrue();
            assertThat(regex.matches("abd")).isFalse();
        }

        @Test
        void emptyAlternationBranchMatchesEmpty() {
            Regex regex = Regex.compile("a(b|)c");
            assertThat(regex.matches("ac")).isTrue();
            assertThat(regex.matches("abc")).isTrue();
        }
    }

    @Nested
    class FindMode {

        @Test
        void findsFirstMatchIndices() {
            Optional<MatchResult> result = Regex.compile("b+").find("aabbbab");
            assertThat(result).contains(new MatchResult(2, 5));
        }

        @Test
        void returnsEmptyWhenNoMatch() {
            assertThat(Regex.compile("xyz").find("abcdef")).isEmpty();
        }

        @Test
        void leftmostWinsOverLaterLongerMatch() {
            Optional<MatchResult> result = Regex.compile("a+").find("baaa");
            assertThat(result).contains(new MatchResult(1, 4));
        }

        @Test
        void sameStartPrefersLongest() {
            Optional<MatchResult> result = Regex.compile("a|ab").find("xab");
            assertThat(result).contains(new MatchResult(1, 3));
        }

        @Test
        void emptyMatchAtStart() {
            Optional<MatchResult> result = Regex.compile("a*").find("bbb");
            assertThat(result).contains(new MatchResult(0, 0));
        }

        @Test
        void matchCanEndAtInputEnd() {
            Optional<MatchResult> result = Regex.compile("[0-9]+").find("id=12345");
            assertThat(result).contains(new MatchResult(3, 8));
            assertThat(result.get().group("id=12345")).isEqualTo("12345");
        }
    }

    @Nested
    class CompileTimeErrors {

        @Test
        void unclosedGroupReportsOpenParenPosition() {
            assertThatThrownBy(() -> Regex.compile("ab(c|d"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e -> {
                        assertThat(e.position()).isEqualTo(2);
                        assertThat(e.getMessage()).contains("Unclosed group").contains("position 2");
                    });
        }

        @Test
        void unmatchedCloseParenReportsItsPosition() {
            assertThatThrownBy(() -> Regex.compile("abc)"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e -> {
                        assertThat(e.position()).isEqualTo(3);
                        assertThat(e.getMessage()).contains("Unmatched ')'");
                    });
        }

        @Test
        void unclosedCharClassReportsOpenBracketPosition() {
            assertThatThrownBy(() -> Regex.compile("xy[abc"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e -> {
                        assertThat(e.position()).isEqualTo(2);
                        assertThat(e.getMessage()).contains("Unclosed character class");
                    });
        }

        @Test
        void quantifierWithoutAtomReportsPosition() {
            assertThatThrownBy(() -> Regex.compile("*abc"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e ->
                            assertThat(e.position()).isEqualTo(0));
            assertThatThrownBy(() -> Regex.compile("a|*b"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e ->
                            assertThat(e.position()).isEqualTo(2));
            assertThatThrownBy(() -> Regex.compile("a(*b)"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e ->
                            assertThat(e.position()).isEqualTo(2));
            assertThatThrownBy(() -> Regex.compile("a**"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e ->
                            assertThat(e.position()).isEqualTo(2));
        }

        @Test
        void reversedRangeIsRejected() {
            assertThatThrownBy(() -> Regex.compile("[z-a]"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e ->
                            assertThat(e.getMessage()).contains("Invalid range"));
        }

        @Test
        void trailingBackslashIsRejected() {
            assertThatThrownBy(() -> Regex.compile("ab\\"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e ->
                            assertThat(e.position()).isEqualTo(2));
        }
    }

    @Nested
    class SafetyLimits {

        @Test
        void tooManyStatesIsACompileError() {
            RegexOptions tiny = new RegexOptions(10, 1_000);
            assertThatThrownBy(() -> Regex.compile("(a|b)(c|d)(e|f)(g|h)(i|j)(k|l)", tiny))
                    .isInstanceOf(RegexTooComplexException.class)
                    .hasMessageContaining("10 states");
        }

        @Test
        void tooLongInputIsARuntimeError() {
            Regex regex = Regex.compile("a+", new RegexOptions(1_000, 100));
            String input = "a".repeat(101);
            assertThatThrownBy(() -> regex.matches(input))
                    .isInstanceOf(RegexInputTooLongException.class)
                    .hasMessageContaining("101")
                    .hasMessageContaining("100");
            assertThatThrownBy(() -> regex.find(input))
                    .isInstanceOf(RegexInputTooLongException.class);
        }

        @Test
        void stateCountIsReported() {
            assertThat(Regex.compile("(a+)+b").stateCount()).isPositive();
        }
    }
}
