package de.unijena.bioinf.ChemistryBase.utils;

import org.apache.commons.lang3.Range;

public class RangeUtils {
    /**
     * Returns the minimal range that encloses both {@code a} range and {@code
     * b}. For example, the span of {@code [1..3]} and {@code (5..7)} is {@code [1..7)}.
     *
     * <p><i>If</i> the input ranges are overlapping, the returned range can
     * also be called their <i>union</i>. If they are not, note that the span might contain values
     * that are not contained in either input range.
     */
    public static <C extends Comparable<C>> Range<C> span(Range<C> a, Range<C> b) {
        int lowerCmp = a.getMinimum().compareTo(b.getMinimum());
        int upperCmp = a.getMaximum().compareTo(b.getMaximum());
        if (lowerCmp <= 0 && upperCmp >= 0) {
            return a;
        } else if (lowerCmp >= 0 && upperCmp <= 0) {
            return b;
        } else {
            C newLower = (lowerCmp <= 0) ? a.getMinimum() : b.getMinimum() ;
            C newUpper = (upperCmp >= 0) ? a.getMaximum() : b.getMaximum();
            return Range.of(newLower, newUpper);
        }
    }

    /**
     * Returns {@code true} if this range is of the form {@code [v..v)} or {@code (v..v]}. (This does
     * not encompass ranges of the form {@code (v..v)}, because such ranges are <i>invalid</i> and
     * can't be constructed at all.)
     *
     * <p>Note that certain discrete ranges such as the integer range {@code (3..4)} are <b>not</b>
     * considered empty, even though they contain no actual values.
     */
    public static <C> boolean isEmpty(Range<C> range) {
        return range.getMinimum().equals(range.getMaximum());
    }
}
