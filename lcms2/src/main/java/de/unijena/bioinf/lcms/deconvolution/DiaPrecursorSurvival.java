package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.util.List;
import java.util.Locale;

/**
 * How intense a precursor has to be before a channel sees anything of it, learned from the precursors whose own
 * intact ion survived into the channel.
 * <p>
 * A precursor that was fragmented sends part of its ions through the isolation window unfragmented, and that ion is
 * a peak in the channel's scans at the precursor's own m/z, traced like any other fragment. The precursors for which
 * such a trace exists are therefore a set the channel demonstrably had enough signal to see something of, and their
 * MS1 intensities say where seeing anything begins. No decoy is possible or needed for this: it is a positive
 * control, in the same way that isotope pairs are one for correlation in
 * {@link de.unijena.bioinf.lcms.statistics.TraceCorrelationStatistics}.
 * <p>
 * Two biases, in opposite directions, and only one of them can be guessed at:
 * <ul>
 *     <li>The set is selected for compounds that resist fragmentation, since one that fragments completely leaves no
 *     intact ion however intense it was. That is a bias in <em>which</em> compounds are observed, and there is no
 *     reason for it to be a bias in their intensity.</li>
 *     <li>The intact ion is usually weaker than the strongest fragment at a useful collision energy, so requiring it
 *     is stricter than requiring a fragment and a floor read straight off this set is too high. This one is
 *     measurable rather than a matter of judgement, which is what {@link Result#medianIntactToStrongestFragment} is
 *     for: it is the factor by which the floor should be relaxed, taken from the same data.</li>
 * </ul>
 * The floor is read at a quantile rather than at the minimum. The weakest survivor of a few thousand precursors is
 * a coincidence - a noise peak at the precursor's own m/z that happened to co-elute - and a bound taken from it is
 * a bound taken from the worst accident in the sample.
 */
public final class DiaPrecursorSurvival {

    /**
     * Which quantile of the survivors' intensities the floor is read at.
     * <p>
     * Low enough to keep the weak end of what was really observed, high enough that a handful of accidental intact
     * ions cannot set it. The relaxation below then does the work a lower quantile would have done, but by a measured
     * factor rather than by moving an arbitrary one.
     */
    public static final double DEFAULT_QUANTILE = 0.25;

    /**
     * How much of the measured intact-to-fragment ratio to trust as a relaxation of the floor. One takes the ratio
     * as it is; the floor is then the intensity at which a compound of median fragmentation behaviour would have
     * shown its strongest fragment, which is the quantity actually wanted.
     */
    public static final double DEFAULT_RELAXATION = 1d;

    /** below this many survivors the quantile says nothing and there is no floor */
    public static final int MIN_OBSERVATIONS = 50;

    /**
     * Where the decoy intact ion is looked for, in Da relative to the precursor.
     * <p>
     * An MS1 spectrum is crowded enough that some peak exists at almost any mass, and a channel scan holds around a
     * hundred fragments, so a co-eluting channel peak at the precursor's own m/z can be a coincidence rather than the
     * surviving precursor. Asking the same question half a Dalton away answers that: no ion of the compound can be
     * there, the crowding and the co-elution requirement are unchanged, and whatever rate comes back is the rate at
     * which this measurement finds intact ions that do not exist.
     * <p>
     * Below rather than above, so the decoy stays within
     * {@link DiaAssignmentGraph#DEFAULT_MAX_FRAGMENT_MZ_ABOVE_PRECURSOR} and is judged by exactly the same candidate
     * rule as the real one. And a whole number of Daltons rather than a half: at high resolution the masses between
     * nominal masses are nearly empty, so a decoy placed there measures the coincidence rate of a region no ion
     * occupies and flatters the result. Measured on one SWATH file, half a Dalton away found 1 decoy against 1760
     * real intact ions, two Daltons away found 156 - the honest number.
     */
    public static final double DECOY_SHIFT = -2.0;

    private DiaPrecursorSurvival() {
    }

    /**
     * @param precursors    how many precursor features the channel had at all
     * @param survived      how many of them had their own intact ion in the channel
     * @param floor         MS1 apex intensity below which a precursor is not worth treating as a candidate, or NaN
     * @param quantileIntensity the survivors' intensity at the quantile, before the relaxation
     * @param medianIntactToStrongestFragment how the intact ion compares with the strongest fragment of the same
     *                      precursor, or NaN if nothing was observed
     */
    public record Result(int precursors, int survived, int decoySurvived, double floor, double quantileIntensity,
                         double medianIntactToStrongestFragment, double medianDecoyToStrongestFragment,
                         int belowFloor, int belowQuantile) {

        /** how much of the survival is more than the same question asked where there can be no answer */
        public double excessOverDecoy() {
            return precursors == 0 ? Double.NaN : (survived - decoySurvived) / (double) precursors;
        }

        public boolean hasFloor() {
            return Double.isFinite(floor);
        }

        public String describe() {
            final String decoy = String.format(Locale.US, " (decoy %d, at %.2f of the strongest fragment, so %.1f %% "
                            + "of precursors survive beyond chance)", decoySurvived, medianDecoyToStrongestFragment,
                    100 * excessOverDecoy());
            if (!hasFloor()) {
                return String.format(Locale.US, "%d of %d precursors kept an intact ion%s, too few to read a floor "
                        + "from", survived, precursors, decoy);
            }
            return String.format(Locale.US, "%d of %d precursors kept an intact ion; their intensity at the %.0f %% "
                            + "quantile is %.0f and an intact ion is %.2f of the strongest fragment, so the floor "
                            + "is %.0f, which %d of the precursors are below (%d are below the unrelaxed "
                            + "quantile)%s", survived, precursors, 100 * DEFAULT_QUANTILE, quantileIntensity,
                    medianIntactToStrongestFragment, floor, belowFloor, belowQuantile, decoy);
        }
    }

    public static Result of(DiaAssignmentGraph graph, Deviation deviation) {
        return of(graph, deviation, DEFAULT_QUANTILE, DEFAULT_RELAXATION);
    }

    /**
     * @param deviation  how far a channel peak's mass may be from the precursor's and still be its intact ion
     * @param quantile   see {@link #DEFAULT_QUANTILE}
     * @param relaxation see {@link #DEFAULT_RELAXATION}
     */
    public static Result of(DiaAssignmentGraph graph, Deviation deviation, double quantile, double relaxation) {
        return of(graph, deviation, quantile, relaxation, MIN_OBSERVATIONS);
    }

    /**
     * @param minObservations how many survivors the quantile needs before it is read at all, see
     *                        {@link #MIN_OBSERVATIONS}
     */
    public static Result of(DiaAssignmentGraph graph, Deviation deviation, double quantile, double relaxation,
                            int minObservations) {
        final List<PrecursorFeature> precursors = graph.getPrecursors();
        final List<ContiguousTrace> fragments = graph.getFragments();
        // one pass over the edges: for every precursor the strongest fragment it could have produced, and the intact
        // ion among them. Going through the edges rather than the fragments means the same candidate rule decides
        // what counts as co-eluting here as decides it everywhere else.
        final double[] intact = new double[precursors.size()];
        final double[] decoy = new double[precursors.size()];
        final double[] strongest = new double[precursors.size()];
        for (DiaAssignmentGraph.PrecursorEdge edge : graph.getPrecursorEdges()) {
            final PrecursorFeature precursor = precursors.get(edge.precursorIndex());
            final ContiguousTrace fragment = fragments.get(edge.fragmentIndex());
            final double apex = precursor.getWindow().apexIntensityIn(fragment);
            if (!(apex > 0)) continue;
            final double allowed = deviation.absoluteFor(precursor.getMz());
            if (Math.abs(fragment.averagedMz() - precursor.getMz()) <= allowed) {
                intact[edge.precursorIndex()] = Math.max(intact[edge.precursorIndex()], apex);
            } else if (Math.abs(fragment.averagedMz() - precursor.getMz() - DECOY_SHIFT) <= allowed) {
                decoy[edge.precursorIndex()] = Math.max(decoy[edge.precursorIndex()], apex);
            } else {
                strongest[edge.precursorIndex()] = Math.max(strongest[edge.precursorIndex()], apex);
            }
        }

        final DoubleArrayList survivorIntensities = new DoubleArrayList();
        final DoubleArrayList ratios = new DoubleArrayList(), decoyRatios = new DoubleArrayList();
        int decoySurvived = 0;
        for (int p = 0; p < precursors.size(); ++p) {
            if (decoy[p] > 0) {
                ++decoySurvived;
                if (strongest[p] > 0) decoyRatios.add(decoy[p] / strongest[p]);
            }
            if (!(intact[p] > 0)) continue;
            survivorIntensities.add(precursors.get(p).getApexIntensity());
            if (strongest[p] > 0) ratios.add(intact[p] / strongest[p]);
        }
        final double decoyRatio = decoyRatios.isEmpty() ? Double.NaN : median(decoyRatios);
        if (survivorIntensities.size() < minObservations) {
            return new Result(precursors.size(), survivorIntensities.size(), decoySurvived, Double.NaN, Double.NaN,
                    ratios.isEmpty() ? Double.NaN : median(ratios), decoyRatio, 0, 0);
        }

        final double[] sorted = survivorIntensities.toDoubleArray();
        java.util.Arrays.sort(sorted);
        final double atQuantile = sorted[Math.min(sorted.length - 1,
                Math.max(0, (int) Math.floor(quantile * sorted.length)))];
        final double ratio = ratios.isEmpty() ? Double.NaN : median(ratios);
        // the relaxation only ever lowers the floor: an intact ion that is stronger than every fragment of its
        // compound says nothing about a compound whose fragments are the strong part
        final double factor = Double.isFinite(ratio) && ratio > 0 && ratio < 1
                ? Math.pow(ratio, relaxation) : 1d;
        final double floor = atQuantile * factor;
        int belowFloor = 0, belowQuantile = 0;
        for (PrecursorFeature precursor : precursors) {
            if (precursor.getApexIntensity() < floor) ++belowFloor;
            if (precursor.getApexIntensity() < atQuantile) ++belowQuantile;
        }
        return new Result(precursors.size(), sorted.length, decoySurvived, floor, atQuantile, ratio, decoyRatio,
                belowFloor, belowQuantile);
    }

    private static double median(DoubleArrayList values) {
        final double[] sorted = values.toDoubleArray();
        java.util.Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }
}
