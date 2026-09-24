package com.example.gsb.regex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchTest {

    @Test
    void literalAndConcat() {
        Regex.Pattern p = Regex.compile("abc");
        assertThat(p.matches("abc")).isTrue();
        assertThat(p.matches("ab")).isFalse();
        assertThat(p.matches("abcd")).isFalse();
        assertThat(p.matches("xbc")).isFalse();
    }

    @Test
    void dotMatchesAnyChar() {
        Regex.Pattern p = Regex.compile("a.c");
        assertThat(p.matches("abc")).isTrue();
        assertThat(p.matches("a c")).isTrue();
        assertThat(p.matches("ac")).isFalse();
    }

    @Test
    void charClassAndRange() {
        assertThat(Regex.compile("[abc]+").matches("caba")).isTrue();
        assertThat(Regex.compile("[abc]+").matches("cabaX")).isFalse();
        assertThat(Regex.compile("[a-z]+").matches("hello")).isTrue();
        assertThat(Regex.compile("[a-z]+").matches("Hello")).isFalse();
        assertThat(Regex.compile("[a-zA-Z0-9_]+").matches("Var_09")).isTrue();
    }

    @Test
    void negatedCharClass() {
        Regex.Pattern p = Regex.compile("[^0-9]+");
        assertThat(p.matches("abc")).isTrue();
        assertThat(p.matches("ab1")).isFalse();
    }

    @Test
    void quantifiers() {
        assertThat(Regex.compile("ab*c").matches("ac")).isTrue();
        assertThat(Regex.compile("ab*c").matches("abbbc")).isTrue();
        assertThat(Regex.compile("ab+c").matches("ac")).isFalse();
        assertThat(Regex.compile("ab+c").matches("abc")).isTrue();
        assertThat(Regex.compile("ab?c").matches("ac")).isTrue();
        assertThat(Regex.compile("ab?c").matches("abc")).isTrue();
        assertThat(Regex.compile("ab?c").matches("abbc")).isFalse();
    }

    @Test
    void groupAndAlternation() {
        Regex.Pattern p = Regex.compile("(ab|cd)+e?");
        assertThat(p.matches("ab")).isTrue();
        assertThat(p.matches("cdabe")).isTrue();
        assertThat(p.matches("abcdabcd")).isTrue();
        assertThat(p.matches("ad")).isFalse();
        assertThat(Regex.compile("a|b|c").matches("b")).isTrue();
        assertThat(Regex.compile("a|b|c").matches("d")).isFalse();
    }

    @Test
    void nestedGroups() {
        Regex.Pattern p = Regex.compile("((a|b)c)+d");
        assertThat(p.matches("acbcd")).isTrue();
        assertThat(p.matches("acd")).isTrue();
        assertThat(p.matches("abd")).isFalse();
    }

    @Test
    void escapesAndPredefinedClasses() {
        assertThat(Regex.compile("a\\*b").matches("a*b")).isTrue();
        assertThat(Regex.compile("a\\.b").matches("a.b")).isTrue();
        assertThat(Regex.compile("\\d+").matches("12345")).isTrue();
        assertThat(Regex.compile("\\d+").matches("12a")).isFalse();
        assertThat(Regex.compile("\\w+").matches("abc_09")).isTrue();
        assertThat(Regex.compile("\\s+").matches(" \t\n")).isTrue();
        assertThat(Regex.compile("\\(\\)").matches("()")).isTrue();
    }

    @Test
    void emptyPatternMatchesEmptyString() {
        assertThat(Regex.compile("").matches("")).isTrue();
        assertThat(Regex.compile("").matches("a")).isFalse();
        assertThat(Regex.compile("a|").matches("")).isTrue();
        assertThat(Regex.compile("(a|)b").matches("b")).isTrue();
    }

    @Test
    void nullableLoopDoesNotHang() {
        // ()* 与 (a*)* 这类可空循环体，闭包必须能终止
        assertThat(Regex.compile("()*a").matches("a")).isTrue();
        assertThat(Regex.compile("(a*)*b").matches("aaab")).isTrue();
        assertThat(Regex.compile("(a*)*b").matches("aaa")).isFalse();
    }

    @Test
    void findReturnsFirstMatchSpan() {
        Optional<Regex.Match> m = Regex.compile("a+b").find("xxaaabyy");
        assertThat(m).isPresent();
        assertThat(m.get().start()).isEqualTo(2);
        assertThat(m.get().end()).isEqualTo(6);
        assertThat(m.get().group()).isEqualTo("aaab");
    }

    @Test
    void findIsLeftmost() {
        Optional<Regex.Match> m = Regex.compile("b+").find("abbba bb");
        assertThat(m).isPresent();
        assertThat(m.get().start()).isEqualTo(1);
        assertThat(m.get().end()).isEqualTo(4);
    }

    @Test
    void findPrefersLongerMatchAtSameStart() {
        Optional<Regex.Match> m = Regex.compile("a|ab").find("zab");
        assertThat(m).isPresent();
        assertThat(m.get().group()).isEqualTo("ab");
    }

    @Test
    void findNoMatch() {
        assertThat(Regex.compile("xyz").find("abc")).isEmpty();
    }

    @Test
    void findEmptyMatchAtStart() {
        Optional<Regex.Match> m = Regex.compile("a*").find("bbb");
        assertThat(m).isPresent();
        assertThat(m.get().start()).isEqualTo(0);
        assertThat(m.get().end()).isEqualTo(0);
    }

    @Test
    void findWithCharClass() {
        Optional<Regex.Match> m = Regex.compile("[0-9]+").find("abc123def");
        assertThat(m).isPresent();
        assertThat(m.get().start()).isEqualTo(3);
        assertThat(m.get().end()).isEqualTo(6);
        assertThat(m.get().group()).isEqualTo("123");
    }
}
