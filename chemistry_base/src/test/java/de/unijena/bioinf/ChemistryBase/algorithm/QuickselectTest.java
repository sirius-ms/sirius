package de.unijena.bioinf.ChemistryBase.algorithm;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class QuickselectTest {

    /** the rank that comes back must be the one a full sort would give, ties and all */
    @Test
    public void selectsTheSameRankAsASort() {
        final Random rnd = new Random(42);
        for (int t = 0; t < 2000; ++t) {
            final int n = 1 + rnd.nextInt(200);
            final double[] xs = new double[n];
            // a small value range on purpose: intensities repeat, and a partition that mishandles
            // ties would show up here and nowhere else
            for (int i = 0; i < n; ++i) xs[i] = rnd.nextInt(20);
            final double[] sorted = xs.clone();
            Arrays.sort(sorted);
            final int k = rnd.nextInt(n);
            assertEquals("rank " + k + " of " + n, sorted[k], Quickselect.quickselectInplace(xs.clone(), 0, n, k), 0d);
        }
    }

    /**
     * Two calls on equal data must agree, whenever they are made.
     * <p>
     * The pivot used to be seeded from {@link System#nanoTime()}. The returned rank does not depend on the pivot
     * while every element is comparable, but a NaN makes {@code a[i] < v} false in both directions, so the
     * partition separates nothing and the answer follows the pivots - which made a noise level, and with it every
     * peak the trace picker accepted, depend on when the run happened to start.
     */
    @Test
    public void resultDoesNotDependOnWhenItIsCalled() {
        final Random rnd = new Random(11);
        final double[] xs = new double[200];
        for (int i = 0; i < xs.length; ++i) xs[i] = rnd.nextDouble() * 1000;
        xs[123] = Double.NaN;

        final Set<Double> results = new HashSet<>();
        for (int t = 0; t < 2000; ++t) results.add(Quickselect.quickselectInplace(xs.clone(), 0, xs.length, 50));
        assertEquals("quickselect must not depend on the wall clock: " + results, 1, results.size());
    }

    /** an all-zero array sums to zero at both ends, which must not degenerate the pivot sequence */
    @Test
    public void handlesAnArrayThatSumsToZeroAtBothEnds() {
        final double[] zeros = new double[1000];
        assertEquals(0d, Quickselect.quickselectInplace(zeros.clone(), 0, zeros.length, 500), 0d);

        final double[] symmetric = new double[999];
        for (int i = 0; i < symmetric.length; ++i) symmetric[i] = i - 499; // ends sum to zero
        final double[] sorted = symmetric.clone();
        Arrays.sort(sorted);
        assertEquals(sorted[250], Quickselect.quickselectInplace(symmetric.clone(), 0, symmetric.length, 250), 0d);
    }

    /** the float overload carries the same seeding */
    @Test
    public void floatOverloadSelectsTheSameRankAsASort() {
        final Random rnd = new Random(5);
        for (int t = 0; t < 500; ++t) {
            final int n = 1 + rnd.nextInt(150);
            final float[] xs = new float[n];
            for (int i = 0; i < n; ++i) xs[i] = rnd.nextInt(20);
            final float[] sorted = xs.clone();
            Arrays.sort(sorted);
            final int k = rnd.nextInt(n);
            assertEquals(sorted[k], Quickselect.quickselectInplace(xs.clone(), 0, n, k), 0d);
        }
    }
}
