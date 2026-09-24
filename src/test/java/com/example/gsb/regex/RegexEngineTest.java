package com.example.gsb.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RegexEngineTest {

    @Nested
    class LiteralsAndDot {

        @Test
        void literalMatchesWholeString() {
            Regex re = Regex.compile("abc");
            assertThat(re.matches("abc")).isTrue();
            assertThat(re.matches("abd")).isFalse();
            assertThat(re.matches("ab")).isFalse();
            assertThat(re.matches("abcd")).isFalse();
        }

        @Test
        void dotMatchesAnySingleChar() {
            Regex re = Regex.compile("a.c");
            assertThat(re.matches("axc")).isTrue();
            assertThat(re.matches("a\nc")).isTrue();
            assertThat(re.matches("ac")).isFalse();
        }

        @Test
        void emptyPatternMatchesOnlyEmptyString() {
            Regex re = Regex.compile("");
            assertThat(re.matches("")).isTrue();
            assertThat(re.matches("a")).isFalse();
        }
    }

    @Nested
    class CharClasses {

        @Test
        void enumeration() {
            Regex re = Regex.compile("[abc]+");
            assertThat(re.matches("cab")).isTrue();
            assertThat(re.matches("aaa")).isTrue();
            assertThat(re.matches("abcd")).isFalse();
        }

        @Test
        void range() {
            Regex re = Regex.compile("[a-z]+");
            assertThat(re.matches("hello")).isTrue();
            assertThat(re.matches("Hello")).isFalse();
        }

        @Test
        void mixedRangesAndLiterals() {
            Regex re = Regex.compile("[a-zA-Z0-9_]+");
            assertThat(re.matches("Hello_123")).isTrue();
            assertThat(re.matches("Hello-123")).isFalse();
        }

        @Test
        void negatedClass() {
            Regex re = Regex.compile("[^0-9]+");
            assertThat(re.matches("abc")).isTrue();
            assertThat(re.matches("123")).isFalse();
        }

        @Test
        void trailingDashIsLiteral() {
            Regex re = Regex.compile("[a-]+");
            assertThat(re.matches("a-a-")).isTrue();
            assertThat(re.matches("ab")).isFalse();
        }
    }

    @Nested
    class Quantifiers {

        @Test
        void star() {
            Regex re = Regex.compile("ab*c");
            assertThat(re.matches("ac")).isTrue();
            assertThat(re.matches("abc")).isTrue();
            assertThat(re.matches("abbbc")).isTrue();
            assertThat(re.matches("ab")).isFalse();
        }

        @Test
        void plus() {
            Regex re = Regex.compile("ab+c");
            assertThat(re.matches("ac")).isFalse();
            assertThat(re.matches("abc")).isTrue();
            assertThat(re.matches("abbc")).isTrue();
        }

        @Test
        void question() {
            Regex re = Regex.compile("ab?c");
            assertThat(re.matches("ac")).isTrue();
            assertThat(re.matches("abc")).isTrue();
            assertThat(re.matches("abbc")).isFalse();
        }

        @Test
        void quantifierAppliesToGroup() {
            Regex re = Regex.compile("(ab)+");
            assertThat(re.matches("ab")).isTrue();
            assertThat(re.matches("ababab")).isTrue();
            assertThat(re.matches("aba")).isFalse();
        }
    }

    @Nested
    class GroupsAndAlternation {

        @Test
        void alternation() {
            Regex re = Regex.compile("cat|dog");
            assertThat(re.matches("cat")).isTrue();
            assertThat(re.matches("dog")).isTrue();
            assertThat(re.matches("cow")).isFalse();
        }

        @Test
        void groupedAlternationWithRepeat() {
            Regex re = Regex.compile("(ab|cd)+");
            assertThat(re.matches("abcdab")).isTrue();
            assertThat(re.matches("cdcd")).isTrue();
            assertThat(re.matches("abc")).isFalse();
        }

        @Test
        void nestedGroups() {
            Regex re = Regex.compile("a(b(c|d))?e");
            assertThat(re.matches("ae")).isTrue();
            assertThat(re.matches("abce")).isTrue();
            assertThat(re.matches("abde")).isTrue();
            assertThat(re.matches("abe")).isFalse();
        }

        @Test
        void emptyAlternative() {
            Regex re = Regex.compile("a|");
            assertThat(re.matches("a")).isTrue();
            assertThat(re.matches("")).isTrue();
            assertThat(re.matches("b")).isFalse();
        }
    }

    @Nested
    class FindMode {

        @Test
        void returnsLeftmostLongestMatch() {
            Optional<MatchResult> m = Regex.compile("a+").find("baaac");
            assertThat(m).hasValue(new MatchResult(1, 4));
        }

        @Test
        void findsMatchInMiddle() {
            Optional<MatchResult> m = Regex.compile("b+").find("aabbb");
            assertThat(m).hasValue(new MatchResult(2, 5));
        }

        @Test
        void prefersEarliestStartOverEarliestEnd() {
            Optional<MatchResult> m = Regex.compile("a+b|ab").find("xaabc");
            assertThat(m).hasValue(new MatchResult(1, 4));
        }

        @Test
        void emptyMatchAtPositionZero() {
            Optional<MatchResult> m = Regex.compile("a*").find("bbb");
            assertThat(m).hasValue(new MatchResult(0, 0));
        }

        @Test
        void noMatchReturnsEmpty() {
            assertThat(Regex.compile("xyz").find("abcdef")).isEmpty();
        }

        @Test
        void charClassSearch() {
            Optional<MatchResult> m = Regex.compile("[0-9]+").find("abc123def");
            assertThat(m).hasValue(new MatchResult(3, 6));
        }
    }

    @Nested
    class SyntaxErrors {

        @Test
        void unclosedGroupReportsOpenPosition() {
            assertThatThrownBy(() -> Regex.compile("a(b|c"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e -> {
                        assertThat(e.getPosition()).isEqualTo(1);
                        assertThat(e.getMessage()).contains("Unclosed group").contains("index 1");
                    });
        }

        @Test
        void unmatchedCloseParenReportsPosition() {
            assertThatThrownBy(() -> Regex.compile("ab)"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e -> {
                        assertThat(e.getPosition()).isEqualTo(2);
                        assertThat(e.getMessage()).contains("Unmatched ')'");
                    });
        }

        @Test
        void quantifierWithoutTargetReportsPosition() {
            assertThatThrownBy(() -> Regex.compile("*abc"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e -> {
                        assertThat(e.getPosition()).isEqualTo(0);
                        assertThat(e.getMessage()).contains("has no target");
                    });
        }

        @Test
        void consecutiveQuantifiersReportPosition() {
            assertThatThrownBy(() -> Regex.compile("ab+*"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e -> {
                        assertThat(e.getPosition()).isEqualTo(3);
                        assertThat(e.getMessage()).contains("follows another quantifier");
                    });
        }

        @Test
        void unclosedCharClassReportsOpenPosition() {
            assertThatThrownBy(() -> Regex.compile("ab[xyz"))
                    .isInstanceOfSatisfying(RegexSyntaxException.class, e -> {
                        assertThat(e.getPosition()).isEqualTo(2);
                        assertThat(e.getMessage()).contains("Unclosed character class");
                    });
        }

        @Test
        void emptyCharClassIsRejected() {
            assertThatThrownBy(() -> Regex.compile("[]"))
                    .isInstanceOf(RegexSyntaxException.class)
                    .hasMessageContaining("Empty character class");
        }

        @Test
        void reversedRangeIsRejected() {
            assertThatThrownBy(() -> Regex.compile("[z-a]"))
                    .isInstanceOf(RegexSyntaxException.class)
                    .hasMessageContaining("Invalid range");
        }
    }

    @Nested
    class Limits {

        @Test
        void stateLimitExceededAtCompileTime() {
            RegexOptions options = new RegexOptions(10, 1000);
            assertThatThrownBy(() -> Regex.compile("abcdefghijklmnop", options))
                    .isInstanceOf(RegexLimitExceededException.class)
                    .hasMessageContaining("NFA states");
        }

        @Test
        void inputLengthLimitExceededAtMatchTime() {
            Regex re = Regex.compile("[a-z]+", new RegexOptions(1000, 5));
            assertThatThrownBy(() -> re.matches("abcdef"))
                    .isInstanceOf(RegexLimitExceededException.class)
                    .hasMessageContaining("Input length 6");
            assertThatThrownBy(() -> re.find("abcdef"))
                    .isInstanceOf(RegexLimitExceededException.class);
            assertThat(re.matches("abcde")).isTrue();
        }

        @Test
        void invalidOptionsRejected() {
            assertThatThrownBy(() -> new RegexOptions(0, 10))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new RegexOptions(10, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CatastrophicBacktracking {

        @Test
        void nestedQuantifiersMatchFailsFast() {
            Regex re = Regex.compile("(a+)+b");
            String evil = "a".repeat(30_000);
            assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
                    assertThat(re.matches(evil)).isFalse());
        }

        @Test
        void nestedQuantifiersFindFailsFast() {
            Regex re = Regex.compile("(a+)+b");
            String evil = "a".repeat(30_000);
            assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
                    assertThat(re.find(evil)).isEmpty());
        }

        @Test
        void deeplyNestedQuantifiersFailFast() {
            Regex re = Regex.compile("((a+)+)+b");
            String evil = "a".repeat(30_000);
            assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
                    assertThat(re.matches(evil)).isFalse());
        }

        @Test
        void nestedQuantifiersStillMatchCorrectly() {
            Regex re = Regex.compile("(a+)+b");
            assertThat(re.matches("aab")).isTrue();
            assertThat(re.matches("b")).isFalse();
            String input = "a".repeat(1_000) + "b";
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
                assertThat(re.matches(input)).isTrue();
                assertThat(re.find("xx" + input + "yy"))
                        .hasValue(new MatchResult(2, 2 + 1_001));
            });
        }

        @Test
        void ambiguousAlternationFailsFast() {
            Regex re = Regex.compile("(a|a)*b");
            String evil = "a".repeat(30_000);
            assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
                    assertThat(re.matches(evil)).isFalse());
        }
    }
}
