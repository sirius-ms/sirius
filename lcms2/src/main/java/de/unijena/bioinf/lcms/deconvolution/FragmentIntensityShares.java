package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.Trace;

import java.util.List;

/**
 * How much of a fragment's measured signal belongs to each of the precursors it was assigned to.
 * <p>
 * Until this existed, a fragment assigned to three precursors was written into all three spectra at its full apex
 * height, so the pipeline attributed three times the ion current the instrument recorded, and a peak's height said
 * nothing about how confidently it belonged there. The assignment probability was used only to decide whether a peak
 * appeared at all.
 * <p>
 * The model is that at every scan point the fragment's intensity is the sum of the contributions of the precursors
 * that produced it, each following that precursor's own elution profile:
 * <pre>
 *     F(t)  =  sum over p of  A(p) * P(p,t)
 * </pre>
 * with {@code P(p,·)} the precursor's MS1 profile resampled onto the channel's axis and {@code A(p) >= 0} the share
 * of it that fragments into this mass. The profiles are known, so this is a non-negative least squares problem per
 * fragment rather than a factorisation of anything - see {@link NonNegativeLeastSquares}, whose class comment
 * explains why the basis is deliberately left in absolute intensities.
 * <p>
 * The peak height reported for a precursor is its contribution at its own apex, {@code A(p) * max P(p,t)} over that
 * precursor's window, which is the same quantity the old code approximated by the fragment's apex and reduces to it
 * exactly when one precursor explains the fragment on its own.
 */
public final class FragmentIntensityShares {

    /**
     * How much of the fragment a precursor has to account for before it gets a peak at all, as a fraction of the
     * fragment's own apex.
     * <p>
     * A candidate that explains essentially none of the fragment gets a coefficient near zero, and the product is
     * then a positive number that is not a peak. It cannot simply be left in either: peak intensities are carried as
     * floats further down, a value that underflows to zero there makes the intensity weighted mass of a merged peak
     * a division of zero by zero, and the resulting NaN mass sends {@code Spectrums.extractMostIntensivePeaks} past
     * the end of its own array. A relative floor keeps this scale free and stays far away from that edge.
     */
    public static final double MIN_RELATIVE_SHARE = 1e-4;

    private FragmentIntensityShares() {
    }

    /**
     * @param channel    the channel both the fragment and the windows live on
     * @param candidates the precursors the fragment was assigned to, all of this channel
     * @param fragment   the fragment's chromatogram, on the channel's axis
     * @return the fragment's peak height in each candidate's spectrum, in the order of {@code candidates}
     */
    public static double[] of(DiaChannel channel, List<PrecursorFeature> candidates, Trace fragment) {
        final double[] shares = new double[candidates.size()];
        if (candidates.isEmpty()) return shares;
        final int from = Math.max(0, fragment.startId());
        final int to = Math.min(channel.numberOfScans() - 1, fragment.endId());
        if (to < from) return shares;

        final double[] target = new double[to - from + 1];
        double apexOfFragment = 0d;
        for (int k = from; k <= to; ++k) {
            target[k - from] = fragment.intensity(k);
            apexOfFragment = Math.max(apexOfFragment, target[k - from]);
        }
        if (!(apexOfFragment > 0d)) return shares;
        final double floor = MIN_RELATIVE_SHARE * apexOfFragment;

        final double[][] profiles = new double[candidates.size()][];
        for (int j = 0; j < candidates.size(); ++j) {
            profiles[j] = Ms1TraceResampler.resample(candidates.get(j).getTrace(), channel.getAxis(), from, to);
        }
        final double[] coefficients = NonNegativeLeastSquares.solve(profiles, target);

        for (int j = 0; j < candidates.size(); ++j) {
            final ChannelWindow window = candidates.get(j).getWindow();
            double apex = 0d;
            for (int k = Math.max(from, window.from()); k <= Math.min(to, window.to()); ++k) {
                apex = Math.max(apex, profiles[j][k - from]);
            }
            final double share = coefficients[j] * apex;
            shares[j] = Double.isFinite(share) && share >= floor ? share : 0d;
        }
        return shares;
    }
}
