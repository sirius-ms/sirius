package de.unijena.bioinf.ms.gui.utils.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The threshold field takes the unit from a percent sign, so that picking "% of run max" from the drop-down and
 * typing "10 %" are the same statement. Reading it has to survive the way people actually type it.
 */
public class IntensityFilterTest {

    @Test
    public void testAPercentSignNamesTheUnitWhereverItIsTyped() {
        assertTrue(IntensityFilter.isPercentTyped("10%"));
        assertTrue(IntensityFilter.isPercentTyped("10 %"), "a space before the sign is how it is written");
        assertTrue(IntensityFilter.isPercentTyped("  10 %  "), "leading and trailing space is typing, not meaning");
        assertTrue(IntensityFilter.isPercentTyped("0,5 %"), "the number is read by the field's own formatter");
    }

    @Test
    public void testWithoutTheSignTheThresholdIsAbsolute() {
        assertFalse(IntensityFilter.isPercentTyped("10"));
        assertFalse(IntensityFilter.isPercentTyped("1000000"));
        assertFalse(IntensityFilter.isPercentTyped(""));
        assertFalse(IntensityFilter.isPercentTyped(null));
    }

    @Test
    public void testStrippingLeavesTheNumberTheFormatterHasToRead() {
        assertEquals("10", IntensityFilter.stripPercent("10 %"));
        assertEquals("10", IntensityFilter.stripPercent("10%"));
        assertEquals("0,5", IntensityFilter.stripPercent("  0,5 %  "));
        assertEquals("1000", IntensityFilter.stripPercent("1000"), "nothing to strip");
        assertEquals("", IntensityFilter.stripPercent(null));
        assertEquals("", IntensityFilter.stripPercent("%"), "a sign alone is no number");
    }

    @Test
    public void testTheMeasuresAreNamedAsTheContextMenuNamesThem() {
        // the "Order by intensity" submenu offers "Peak height (apex)" and "Peak area (AUC)"
        assertEquals("peak height", IntensityFilter.Measure.APEX.toString());
        assertEquals("peak area", IntensityFilter.Measure.AUC.toString());
    }

    @Test
    public void testTheUnitsSayWhatTheyMean() {
        assertEquals("absolute", IntensityFilter.Mode.ABSOLUTE.toString());
        assertEquals("% of run max", IntensityFilter.Mode.RELATIVE_TO_RUN_MAX.toString(),
                "the drop-down is where the percentage says what it is a percentage of");
    }
}
