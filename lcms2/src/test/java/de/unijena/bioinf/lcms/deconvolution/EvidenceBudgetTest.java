package de.unijena.bioinf.lcms.deconvolution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The fixed budget of evidence a spectrum is summarised by keeps its <em>best</em> p-values.
 * <p>
 * A log p-value is negative and more negative is more significant, so the best values are the smallest ones and an
 * implementation that kept the largest would be quietly measuring the opposite of what it claims. Nothing in an
 * aggregate over thousands of spectra would show that up - the signal would simply look weak - so the order is worth
 * pinning down here.
 */
public class EvidenceBudgetTest {

    private static double[] budget(int slots, double... logPValues) {
        final double[] best = new double[slots];
        for (double logP : logPValues) DiaDeconvolution.keepBest(best, logP);
        return best;
    }

    @Test
    public void theBudgetKeepsTheMostSignificantValues() {
        // offered out of order, and more than the budget holds
        final double[] best = budget(5, -1, -9, -3, -7, -5, -2, -8);
        assertArrayEquals(new double[]{-9, -8, -7, -5, -3}, best, 1e-12,
                "the five most negative, ascending; -1 and -2 are the least significant and drop out");
        double sum = 0d;
        for (double value : best) sum += value;
        assertEquals(-32d, sum, 1e-12);
    }

    /**
     * An unfilled slot has to contribute nothing, which is what zero means for a log p-value: a spectrum with two
     * well supported peaks may not score like one with five.
     */
    @Test
    public void unfilledSlotsContributeNoEvidence() {
        assertArrayEquals(new double[]{-6, -4, 0, 0, 0}, budget(5, -4, -6), 1e-12);
    }

    /** a value worse than everything already held changes nothing */
    @Test
    public void aWeakerValueIsIgnoredOnceTheBudgetIsFull() {
        final double[] full = budget(3, -5, -6, -7);
        DiaDeconvolution.keepBest(full, -1);
        assertArrayEquals(new double[]{-7, -6, -5}, full, 1e-12);
    }

    /** and a positive log p-value, which is no evidence at all, never displaces a real one */
    @Test
    public void noEvidenceNeverDisplacesEvidence() {
        final double[] best = budget(3, -5, 1.5, -2);
        assertArrayEquals(new double[]{-5, -2, 0}, best, 1e-12);
    }
}
