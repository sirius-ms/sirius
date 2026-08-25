package de.unijena.bioinf.lcms.trace;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class RectExpandToTest {

    private static Rect rect(double minMz, double maxMz, double minRt, double maxRt, double avgMz) {
        return new Rect((float) minMz, (float) maxMz, (float) minRt, (float) maxRt, avgMz);
    }

    @Test
    public void growsToCoverTheOther() {
        final Rect r = rect(100.0, 100.2, 10, 20, 100.1);
        assertTrue(r.expandTo(rect(99.5, 100.1, 15, 30, 99.8)));
        assertEquals(99.5f, r.minMz, 0f);
        assertEquals(100.2f, r.maxMz, 0f);
        assertEquals(10f, r.minRt, 0f);
        assertEquals(30f, r.maxRt, 0f);
    }

    @Test
    public void reportsWhetherTheBoundsMoved() {
        final Rect r = rect(100.0, 100.2, 10, 20, 100.1);
        assertFalse("a rectangle already covered must not report growth",
                r.expandTo(rect(100.05, 100.15, 12, 18, 100.1)));
        assertTrue(r.expandTo(rect(100.05, 100.15, 12, 25, 100.1)));
    }

    /**
     * The representative mass must be left alone.
     * <p>
     * It used to be updated pairwise as {@code (avgMz + other.avgMz)/2}, which is not a mean and made the
     * result depend on the order the spatial index returned the rectangles in. Callers compute the mean
     * over the whole group instead, so this method must not touch it.
     */
    @Test
    public void doesNotTouchTheRepresentativeMass() {
        final Rect r = rect(100.0, 100.2, 10, 20, 100.1);
        r.expandTo(rect(99.0, 101.0, 5, 40, 500.0));
        assertEquals(100.1, r.avgMz, 0d);
    }

    /** min and max are exactly associative, so the grown bounds cannot depend on absorption order */
    @Test
    public void boundsAreIndependentOfAbsorptionOrder() {
        final List<Rect> others = new ArrayList<>(List.of(
                rect(99.5, 100.1, 15, 30, 99.8),
                rect(100.15, 100.9, 2, 12, 100.5),
                rect(98.0, 98.5, 25, 60, 98.2)));
        Rect reference = null;
        for (int perm = 0; perm < 12; ++perm) {
            Collections.shuffle(others, new java.util.Random(perm));
            final Rect r = rect(100.0, 100.2, 10, 20, 100.1);
            for (Rect o : others) r.expandTo(o);
            if (reference == null) reference = r;
            else {
                assertEquals(reference.minMz, r.minMz, 0f);
                assertEquals(reference.maxMz, r.maxMz, 0f);
                assertEquals(reference.minRt, r.minRt, 0f);
                assertEquals(reference.maxRt, r.maxRt, 0f);
            }
        }
        assertEquals(98.0f, reference.minMz, 0f);
        assertEquals(100.9f, reference.maxMz, 0f);
        assertEquals(2f, reference.minRt, 0f);
        assertEquals(60f, reference.maxRt, 0f);
    }
}
