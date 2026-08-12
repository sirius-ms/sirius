package de.unijena.bioinf.lcms.deconvolution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The solver behind the intensity shares. The last two tests are the ones that matter for the deconvolution: what
 * happens when two candidates elute identically, and what happens when one of them cannot have contributed.
 */
public class NonNegativeLeastSquaresTest {

    private static double[] gaussian(double centre, double height, int length) {
        final double[] profile = new double[length];
        for (int k = 0; k < length; ++k) profile[k] = height * Math.exp(-0.5 * Math.pow((k - centre) / 2d, 2));
        return profile;
    }

    private static double[] mix(double[][] columns, double[] coefficients) {
        final double[] target = new double[columns[0].length];
        for (int j = 0; j < columns.length; ++j) {
            for (int i = 0; i < target.length; ++i) target[i] += coefficients[j] * columns[j][i];
        }
        return target;
    }

    @Test
    public void oneCandidateGivesTheRatioOfTheHeights() {
        final double[][] columns = {gaussian(5, 1_000_000d, 11)};
        final double[] x = NonNegativeLeastSquares.solve(columns, gaussian(5, 20_000d, 11));
        assertEquals(0.02, x[0], 1e-4, "a fragment two per cent as high as its precursor");
    }

    @Test
    public void twoDistinctShapesAreSeparated() {
        // the whole point: a fragment shared between two precursors that do not co-elute exactly
        final double[][] columns = {gaussian(4, 1_000_000d, 15), gaussian(9, 500_000d, 15)};
        final double[] x = NonNegativeLeastSquares.solve(columns, mix(columns, new double[]{0.03, 0.08}));
        assertEquals(0.03, x[0], 1e-3);
        assertEquals(0.08, x[1], 1e-3);
    }

    @Test
    public void aCandidateThatWouldNeedANegativeCoefficientGetsNothing() {
        // the fragment is the first precursor's alone; an unconstrained fit would give the second a negative
        // coefficient to cancel the overlap, which is not a thing a fragment can do
        final double[][] columns = {gaussian(4, 1_000_000d, 15), gaussian(6, 1_000_000d, 15)};
        final double[] x = NonNegativeLeastSquares.solve(columns, gaussian(3, 30_000d, 15));
        assertTrue(x[0] > 0, "the earlier precursor explains it: " + x[0]);
        assertEquals(0d, x[1], 0d, "and the later one is not allowed to subtract");
    }

    @Test
    public void identicalShapesGoToTheMoreIntensivePrecursor() {
        // two precursors co-eluting exactly, one twenty times the other. Every split fits equally well, so the
        // question is which one the solver picks - and it has to be the intense one, because attributing the
        // fragment to the weak one would claim a fragmentation yield twenty times as large.
        final double[][] columns = {gaussian(5, 1_000_000d, 11), gaussian(5, 50_000d, 11)};
        final double[] x = NonNegativeLeastSquares.solve(columns, gaussian(5, 20_000d, 11));

        final double fromIntense = x[0] * 1_000_000d, fromWeak = x[1] * 50_000d;
        assertTrue(fromIntense > 10 * fromWeak,
                "intense precursor should take nearly all of it: " + fromIntense + " against " + fromWeak);
        assertEquals(20_000d, fromIntense + fromWeak, 200d, "and together they explain what was measured");
    }

    @Test
    public void aWeakPrecursorKeepsWhatOnlyItsShapeExplains() {
        // the counterpart: the weak precursor elutes elsewhere, so the intense one cannot explain the fragment
        // and the shapes decide rather than the magnitudes
        final double[][] columns = {gaussian(3, 1_000_000d, 15), gaussian(11, 50_000d, 15)};
        final double[] x = NonNegativeLeastSquares.solve(columns, gaussian(11, 20_000d, 15));
        assertTrue(x[1] * 50_000d > 15_000d, "the weak precursor keeps it: " + x[1] * 50_000d);
        assertTrue(x[0] * 1_000_000d < 2_000d, "the intense one gets almost none: " + x[0] * 1_000_000d);
    }

    @Test
    public void theFitConservesWhatWasMeasured() {
        final double[][] columns = {gaussian(4, 800_000d, 15), gaussian(7, 400_000d, 15), gaussian(10, 200_000d, 15)};
        final double[] target = mix(columns, new double[]{0.05, 0.02, 0.10});
        final double[] x = NonNegativeLeastSquares.solve(columns, target);
        final double[] fitted = mix(columns, x);
        for (int k = 0; k < target.length; ++k) {
            assertEquals(target[k], fitted[k], 1e-2 * Math.max(1d, target[k]), "at point " + k);
        }
    }

    @Test
    public void noiseDoesNotProduceNegativeOrAbsurdCoefficients() {
        final double[][] columns = {gaussian(4, 1_000_000d, 15), gaussian(6, 900_000d, 15)};
        final double[] target = gaussian(5, 10_000d, 15);
        for (int k = 0; k < target.length; ++k) target[k] += ((k * 37) % 11 - 5) * 100d;
        final double[] x = NonNegativeLeastSquares.solve(columns, target);
        for (double value : x) {
            assertTrue(value >= 0d, "never negative");
            assertTrue(Double.isFinite(value), "and always finite");
        }
    }

    @Test
    public void coefficientsAreAlwaysUsableAsAnIntensity() {
        // whatever the system, a coefficient that is multiplied by an intensity afterwards must not be infinite or
        // NaN: peak intensities are floats further down, and a NaN mass follows from a NaN intensity
        final double[][] identical = {gaussian(5, 1e9, 11), gaussian(5, 1e9, 11), gaussian(5, 1e-9, 11)};
        for (double[] value : new double[][]{
                NonNegativeLeastSquares.solve(identical, gaussian(5, 1e6, 11)),
                NonNegativeLeastSquares.solve(identical, new double[11]),
                NonNegativeLeastSquares.solve(new double[][]{new double[11], new double[11]}, gaussian(5, 1e3, 11))}) {
            for (double x : value) {
                assertTrue(Double.isFinite(x) && x >= 0d, "coefficient must be usable: " + x);
            }
        }
    }

    @Test
    public void degenerateInputIsNotAProblem() {
        assertEquals(0, NonNegativeLeastSquares.solve(new double[0][], new double[5]).length);
        final double[] zeroColumn = NonNegativeLeastSquares.solve(new double[][]{new double[5]}, new double[5]);
        assertEquals(1, zeroColumn.length);
        assertEquals(0d, zeroColumn[0], 0d, "a candidate that was never measured explains nothing");
        final double[] zeroTarget = NonNegativeLeastSquares.solve(
                new double[][]{gaussian(2, 1000d, 5)}, new double[5]);
        assertEquals(0d, zeroTarget[0], 0d, "and a fragment that is not there needs no explanation");
    }
}
