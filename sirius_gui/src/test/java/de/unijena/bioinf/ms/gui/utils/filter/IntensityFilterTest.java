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

    @Test
    public void testTheUnitPicksTheFieldRatherThanDecoratingTheNumber() {
        for (IntensityFilter.Measure measure : IntensityFilter.Measure.values()) {
            assertEquals(measure.getField(), measure.getField(IntensityFilter.Mode.ABSOLUTE));
            assertEquals(measure.getRelativeField(), measure.getField(IntensityFilter.Mode.RELATIVE_TO_RUN_MAX),
                    "a threshold in percent is matched against the index field that holds percentages");
            assertNotEquals(measure.getField(), measure.getRelativeField());
        }
    }

    @Test
    public void testAPercentSignPicksTheRelativeUnitWhateverIsShown() {
        for (IntensityFilter.Mode shown : IntensityFilter.Mode.values()) {
            assertEquals(IntensityFilter.Mode.RELATIVE_TO_RUN_MAX, IntensityFilter.typedUnit("10 %", 10, shown));
            assertEquals(IntensityFilter.Mode.RELATIVE_TO_RUN_MAX, IntensityFilter.typedUnit("250 %", 250, shown),
                    "a percentage above the ceiling is still a percentage - it is clamped, not reinterpreted");
        }
    }

    @Test
    public void testABareNumberKeepsTheUnitThatIsShown() {
        assertEquals(IntensityFilter.Mode.RELATIVE_TO_RUN_MAX,
                IntensityFilter.typedUnit("50", 50, IntensityFilter.Mode.RELATIVE_TO_RUN_MAX),
                "typing a percentage without its sign is how people write one");
        assertEquals(IntensityFilter.Mode.RELATIVE_TO_RUN_MAX,
                IntensityFilter.typedUnit("100", IntensityFilter.MAX_PERCENT, IntensityFilter.Mode.RELATIVE_TO_RUN_MAX),
                "the whole of the maximum is still a share of it");
        assertEquals(IntensityFilter.Mode.ABSOLUTE,
                IntensityFilter.typedUnit("1000", 1000, IntensityFilter.Mode.ABSOLUTE));
    }

    @Test
    public void testANumberTooLargeForAPercentageIsAnIntensity() {
        assertEquals(IntensityFilter.Mode.ABSOLUTE,
                IntensityFilter.typedUnit("101", 101, IntensityFilter.Mode.RELATIVE_TO_RUN_MAX),
                "no share of a maximum exceeds it, so this can only be an intensity");
        assertEquals(IntensityFilter.Mode.ABSOLUTE,
                IntensityFilter.typedUnit("2000000", 2_000_000, IntensityFilter.Mode.RELATIVE_TO_RUN_MAX));
        assertEquals(IntensityFilter.Mode.ABSOLUTE,
                IntensityFilter.typedUnit("2000000", 2_000_000, IntensityFilter.Mode.ABSOLUTE),
                "and it stays one when that is what is shown");
    }

    @Test
    public void testNoUnitIsChosenWhileASwitchIsInFlight() {
        assertNull(IntensityFilter.typedUnit("50", 50, null),
                "with no unit shown there is none to keep, and none to leave");
        assertEquals(IntensityFilter.Mode.RELATIVE_TO_RUN_MAX, IntensityFilter.typedUnit("50 %", 50, null),
                "a sign says the unit outright, so it needs nothing shown");
    }
}
