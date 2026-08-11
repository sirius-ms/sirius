package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.math.Statistics;

/**
 * How well a fragment's abundance across samples follows its candidate precursor's.
 * <p>
 * This is the one signal that is not co-elution. Two compounds that co-elute in one isolation window co-elute in
 * every sample, so no amount of shape evidence can separate them; how much of each was there varies from sample to
 * sample, and a fragment's abundance should follow the precursor it came from. It is the same argument
 * {@code Scorer.correlateAcrossSamples} makes for adducts, and the same normalisation - each vector by its own sum,
 * then Pearson - so that only the pattern across samples is compared, not the absolute levels.
 * <p>
 * A fragment that was not picked in a sample counts as zero rather than as missing. The precursor was measured
 * there, so the fragment being absent is a measurement, and one that speaks against the assignment.
 * <p>
 * How much this is worth is not decided here, and deliberately not by a rule about how many samples are enough.
 * With technical replicates of one sample there is nearly nothing to correlate, and with three of them a Pearson
 * correlation of one is unremarkable; in a study with varying abundances the same number means a great deal. The
 * decoy calibration settles that on its own: if a high correlation is common among precursors that cannot have
 * emitted the fragment, it earns no weight, whatever the sample count.
 */
public final class DiaAbundanceCorrelation {

    /**
     * Below this many samples there is no pattern to compare. Two points always lie on a line, so a Pearson
     * correlation over them is either plus or minus one and says only whether both went up or both went down.
     */
    public static final int MIN_SAMPLES = 3;

    private DiaAbundanceCorrelation() {
    }

    /**
     * @param precursorAbundance one entry per sample of the aligned feature
     * @param fragmentAbundance  the fragment's intensity in the same samples, in the same order, zero where it was
     *                           not measured
     * @return the correlation and the number of samples it is based on, or undefined if there are too few
     */
    public static DiaTraceCorrelation.Result of(double[] precursorAbundance, double[] fragmentAbundance) {
        final int n = Math.min(precursorAbundance.length, fragmentAbundance.length);
        if (n < MIN_SAMPLES) return DiaTraceCorrelation.Result.UNDEFINED;
        final double[] left = normalize(precursorAbundance, n);
        final double[] right = normalize(fragmentAbundance, n);
        if (left == null || right == null) return DiaTraceCorrelation.Result.UNDEFINED;
        final double pearson = Statistics.pearson(left, right);
        return Double.isFinite(pearson) ? new DiaTraceCorrelation.Result(pearson, n)
                : DiaTraceCorrelation.Result.UNDEFINED;
    }

    /** @return the vector divided by its sum, or null if there is nothing in it */
    private static double[] normalize(double[] values, int n) {
        double sum = 0d;
        for (int k = 0; k < n; ++k) sum += Math.max(0d, values[k]);
        if (!(sum > 0d)) return null;
        final double[] normalized = new double[n];
        for (int k = 0; k < n; ++k) normalized[k] = Math.max(0d, values[k]) / sum;
        return normalized;
    }
}
