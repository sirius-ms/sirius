package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.utils.MaxDoubleAsInfinityTextFormatterFactory.CustomDoubleFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.SpinnerNumberModel;
import java.text.ParseException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * That the text this formatter writes is text it can read again.
 * <p>
 * A spinner commits its editor before it steps, and a commit that throws makes it beep and stay where it is -
 * so a formatter whose output it cannot parse does not merely look odd, it stops the arrows working. The max
 * m/z and max RT spinners run to 5000 and 10000, so their values sit exactly where a number becomes long
 * enough to be written with a grouping separator.
 */
public class MaxDoubleAsInfinityTextFormatterFactoryTest {

    private static final double INFINITY_VALUE = 5000d;

    private final Locale original = Locale.getDefault();

    @AfterEach
    public void restoreLocale() {
        Locale.setDefault(original);
    }

    private static CustomDoubleFormatter formatter() {
        return new CustomDoubleFormatter(new SpinnerNumberModel(INFINITY_VALUE, 0d, INFINITY_VALUE, 10d), INFINITY_VALUE);
    }

    private static void assertReadsBackWhatItWrote(double value) throws ParseException {
        CustomDoubleFormatter formatter = formatter();
        String written = formatter.valueToString(value);
        assertEquals(value, ((Number) formatter.stringToValue(written)).doubleValue(),
                "wrote " + value + " as \"" + written + "\" and read back something else");
    }

    /**
     * Where it used to break: below a thousand there is no grouping separator and everything worked, which is
     * why only the two spinners that count in thousands looked broken.
     */
    @Test
    public void testAGroupedNumberSurvivesTheRoundTrip() throws ParseException {
        Locale.setDefault(Locale.US);

        assertReadsBackWhatItWrote(999d);
        assertReadsBackWhatItWrote(1000d);
        assertReadsBackWhatItWrote(4990d);
    }

    /**
     * The same in a locale that groups with a dot and separates decimals with a comma. This one used to be the
     * worse half: "4.990" parsed as 4.99 without any error, so the filter silently became a different one.
     */
    @Test
    public void testAGroupedNumberSurvivesTheRoundTripWhereTheSeparatorsAreSwapped() throws ParseException {
        Locale.setDefault(Locale.GERMANY);

        assertReadsBackWhatItWrote(999d);
        assertReadsBackWhatItWrote(1000d);
        assertReadsBackWhatItWrote(4990d);
    }

    /**
     * The maximum is shown as a word rather than a number, since it means "no upper bound" - and that word has
     * to read back as the maximum.
     */
    @Test
    public void testTheMaximumIsWrittenAndReadAsInfinite() throws ParseException {
        CustomDoubleFormatter formatter = formatter();

        assertEquals("Infinite", formatter.valueToString(INFINITY_VALUE));
        assertEquals(INFINITY_VALUE, ((Number) formatter.stringToValue("Infinite")).doubleValue());
    }

    /**
     * Typing a number past the maximum asks for no upper bound, rather than for an error.
     */
    @Test
    public void testTypingBeyondTheMaximumMeansTheMaximum() throws ParseException {
        Locale.setDefault(Locale.US);

        assertEquals(INFINITY_VALUE, ((Number) formatter().stringToValue("9999")).doubleValue());
    }

    /**
     * Being lenient about the separators must not become being lenient about everything: a number format on
     * its own would read "12abc" as 12 and leave the rest.
     */
    @Test
    public void testTextThatIsNotJustANumberIsRejected() {
        Locale.setDefault(Locale.US);

        assertThrows(ParseException.class, () -> formatter().stringToValue("12abc"));
        assertThrows(ParseException.class, () -> formatter().stringToValue("abc"));
        assertThrows(ParseException.class, () -> formatter().stringToValue(""));
    }
}
