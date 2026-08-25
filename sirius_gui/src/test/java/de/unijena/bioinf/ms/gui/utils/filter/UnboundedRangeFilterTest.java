package de.unijena.bioinf.ms.gui.utils.filter;

import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What an m/z or retention-time filter with no upper end means.
 * <p>
 * Both used to stop at a made-up number - 5000 and 10000 - which the spinner showed as "Infinite" while the
 * query kept it as a bound. So a filter that said "everything above 200" quietly also said "and below 5000",
 * and on retention time that cap is under three hours, which a long gradient passes. The upper end is now
 * genuinely unbounded: no upper end at all means no filter, and an upper end that is only open upwards means
 * an open range.
 */
public class UnboundedRangeFilterTest {

    private static String queryOf(FeatureFilterModel model) {
        Optional<String> query = model.toLuceneQuery(ConfidenceDisplayMode.APPROXIMATE);
        assertTrue(query.isPresent(), "the filter should be active");
        return query.get();
    }

    /**
     * The untouched filter: from zero to unbounded is not a restriction, and must not compile to one.
     */
    @Test
    public void testAFullyOpenRangeIsNotAFilter() {
        FeatureFilterModel model = new FeatureFilterModel();

        assertFalse(model.isMzFilterActive(), "0 to infinity is no m/z filter");
        assertFalse(model.isRtFilterActive(), "0 to infinity is no retention time filter");
    }

    /**
     * The case the cap used to break: a lower bound only. Whatever is above it must stay in, however far above.
     */
    @Test
    public void testALowerBoundOnlyLeavesTheRangeOpenUpwards() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setCurrentMinMz(200);

        assertTrue(model.isMzFilterActive());
        assertTrue(queryOf(model).contains("[200 TO *]"),
                "an unbounded upper end belongs in the query as an open one: " + queryOf(model));
    }

    /**
     * Retention time is the one that mattered: the old cap of 10000 seconds is inside the length of a run
     * people actually do.
     */
    @Test
    public void testARetentionTimeFilterIsNotCappedAtTheOldMaximum() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setCurrentMinRt(60);
        String query = queryOf(model);

        assertFalse(query.contains("10000"), "no invented upper end: " + query);
        assertTrue(query.contains("[60 TO *]"), "open upwards: " + query);
    }

    /**
     * An upper bound that was really asked for is still a bound, and one beyond the old cap is now expressible
     * at all - which it was not while the maximum was 5000.
     */
    @Test
    public void testAnUpperBoundThatWasAskedForIsKept() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setCurrentMinMz(200);
        model.setCurrentMaxMz(6000);

        assertTrue(model.isMaxMzFilterActive());
        assertTrue(queryOf(model).contains("[200 TO 6000]"), queryOf(model));
    }

    /**
     * Confidence really does end at 1, so nothing about it becomes open.
     */
    @Test
    public void testConfidenceKeepsItsRealUpperEnd() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setCurrentMinConfidence(0.5);

        assertEquals(1d, model.getMaxConfidence());
        assertFalse(queryOf(model).contains("TO *]"), "confidence has a real maximum: " + queryOf(model));
    }
}
