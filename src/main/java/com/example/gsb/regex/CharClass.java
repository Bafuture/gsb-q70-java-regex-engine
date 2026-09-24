package com.example.gsb.regex;

import java.util.ArrayList;
import java.util.List;

/**
 * 字符类：一组闭区间 [lo, hi] 的并集，可整体取反。
 * 单个字符表示为 lo == hi 的区间。
 */
public final class CharClass {

    private final boolean negated;
    private final int[] lo;
    private final int[] hi;

    public CharClass(boolean negated, List<int[]> ranges) {
        this.negated = negated;
        this.lo = new int[ranges.size()];
        this.hi = new int[ranges.size()];
        for (int i = 0; i < ranges.size(); i++) {
            this.lo[i] = ranges.get(i)[0];
            this.hi[i] = ranges.get(i)[1];
        }
    }

    public boolean matches(char c) {
        boolean in = false;
        for (int i = 0; i < lo.length; i++) {
            if (c >= lo[i] && c <= hi[i]) {
                in = true;
                break;
            }
        }
        return in != negated;
    }

    public boolean isNegated() {
        return negated;
    }

    private static List<int[]> ranges(int... pairs) {
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            list.add(new int[]{pairs[i], pairs[i + 1]});
        }
        return list;
    }

    public static CharClass single(char c) {
        return new CharClass(false, ranges(c, c));
    }

    /** 对应 '.'：匹配任意字符（含换行）。 */
    public static CharClass any() {
        return new CharClass(false, ranges(0, 0xFFFF));
    }

    /** \d */
    public static CharClass digit() {
        return new CharClass(false, ranges('0', '9'));
    }

    /** \w */
    public static CharClass word() {
        return new CharClass(false, ranges('0', '9', 'A', 'Z', '_', '_', 'a', 'z'));
    }

    /** \s */
    public static CharClass space() {
        List<int[]> list = ranges(' ', ' ', '\t', '\n', '\f', '\r');
        list.add(new int[]{0x0B, 0x0B});
        return new CharClass(false, list);
    }

    /** \D */
    public static CharClass notDigit() {
        return new CharClass(true, ranges('0', '9'));
    }

    /** \W */
    public static CharClass notWord() {
        return new CharClass(true, ranges('0', '9', 'A', 'Z', '_', '_', 'a', 'z'));
    }

    /** \S */
    public static CharClass notSpace() {
        List<int[]> list = ranges(' ', ' ', '\t', '\n', '\f', '\r');
        list.add(new int[]{0x0B, 0x0B});
        return new CharClass(true, list);
    }
}
