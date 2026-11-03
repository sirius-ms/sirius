package de.unijena.bioinf.lcms.utils;

import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Rect;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public interface Tracker {

    public class NOOP implements Tracker {

        @Override
        public void assignMs2ToMergedTrace(ProcessedSample sample, ContiguousTrace[] sourceTraces, ProcessedSample merged, ProjectedTrace projectedTrace, MsMsTraceReference[] ids) {

        }

        @Override
        public void tracePicked(double mz, double rt, ProcessedSample sample, ContiguousTrace trace) {

        }

        @Override
        public void moiAccepted(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi) {

        }

        @Override
        public void moiRejected(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi) {

        }

        @Override
        public void alignMois(ProcessedSample rightSample, MoI left, MoI right) {

        }

        @Override
        public void unalignedMoI(ProcessedSample s, MoI moI) {

        }

        @Override
        public void moiDeleted(MoI moi) {

        }

        @Override
        public void createRect(ProcessedSample sample, Rect r) {

        }

        @Override
        public void emptyRect(ProcessedSample sample, Rect r) {

        }

        @Override
        public void mergedTrace(ProcessedSample merged, ProcessedSample sample, Rect r, ProjectedTrace projectedTrace, MoI[] moisForSample) {

        }

        @Override
        public void rejectedForFeatureExtraction(Rect r, MergedTrace merged) {

        }

        @Override
        public void noFeatureFound(MergedTrace mergedTrace) {

        }

        @Override
        public void importFeatures(MergedTrace mergedTrace, AlignedFeatures[] features) {

        }

        @Override
        public void startExtractingCompounds(ProcessedSample mergedSample, MergedTrace mergedTrace) {

        }
    }

    void tracePicked(double mz, double rt, ProcessedSample sample, ContiguousTrace trace);

    /**
     * Everything one precursor/fragment pair was judged by, and what became of it.
     * <p>
     * The fields are the whole chain of evidence, because a fragment can be lost at any link of it and the
     * interesting cases are the ones where the earlier links look fine. A field that does not apply is NaN rather
     * than zero: a correlation of zero is a measurement, an absent correlation is not.
     *
     * @param stage        which pass produced this, so the two are told apart when both are tracked
     * @param datapoints   scan points the correlation could be computed over
     * @param logPValue    of correlation and specificity together, against the channel's decoys
     * @param weight       what the resolver gave the edge after the significance floor
     * @param probability  the fragment's assignment probability for this precursor
     * @param outcome      why it ended where it did, in the words of the stage that decided
     */
    record DiaFragmentJudgement(String stage, double precursorMz, double precursorRetentionTime,
                                double precursorIntensity, double fragmentMz, double fragmentRetentionTime,
                                double fragmentIntensity, int datapoints, double correlation, double logPValue,
                                double specificity, double weight, double probability, double intensityShare,
                                int competingPrecursors, String outcome) {
    }

    /**
     * What a fragment's whole candidate set looked like, for one fragment of one channel.
     * <p>
     * A probability is a share of this set, so a fragment can be lost either to a few strong competitors or to very
     * many weak ones, and the two call for opposite remedies. The intensity prior damps a weak competitor's weight in
     * proportion to its apex intensity but never removes it, so the weights are split by how intense the competitor
     * is relative to the most intense candidate: if the weight sits in the bottom band, the fragment was outvoted by
     * precursors the prior already judged to be irrelevant.
     *
     * @param scanPoints      length of the fragment chromatogram, which is what decides how many features it can
     *                        overlap at all
     * @param weightAboveHalf weight from candidates at least half as intense as the most intense one
     * @param weightBelowTenth weight from candidates below a tenth of it
     */
    record DiaFragmentCompetition(double fragmentMz, double fragmentRetentionTime, int scanPoints, int candidates,
                                  double totalWeight, double unassignedWeight, double strongestWeight,
                                  double strongestMz, double strongestIntensity, int countAboveHalf,
                                  int countAboveTenth, double weightAboveHalf, double weightAboveTenth,
                                  double weightBelowTenth) {
    }

    /**
     * Whether the DIA hooks below are worth calling.
     * <p>
     * They sit in loops over every peak of every channel scan and every candidate edge, which is millions of calls
     * per sample, so the callers ask first and skip the work of assembling the arguments. Defaults to no, which is
     * what every tracker that watches MS1 features wants.
     */
    default boolean tracksDia() {
        return false;
    }

    /** a peak of one of a DIA channel's scans, before anything has been made of it */
    default void ms2ScanPeak(ProcessedSample sample, String channel, double retentionTime, double mz,
                             float intensity, float noiseLevel) {
    }

    /** a fragment chromatogram of a DIA channel, or the absence of one if the seed could not be followed */
    default void ms2TracePicked(ProcessedSample sample, String channel, double seedMz, double seedRetentionTime,
                                ContiguousTrace trace) {
    }

    /**
     * One precursor/fragment pair of a DIA channel, see {@link DiaFragmentJudgement}.
     *
     * @param sample null for a judgement made after merging, which is a statement about every sample at once
     */
    default void diaFragmentJudged(ProcessedSample sample, String channel, DiaFragmentJudgement judgement) {
    }

    /**
     * One group of precursors a channel cannot separate, as {@code DiaPrecursorGroups} formed it.
     * <p>
     * Under the grouping strategy only the representative receives a spectrum, so what a group costs is decided by
     * which of its members is the representative. Primitive arrays rather than objects, since a sample has tens of
     * thousands of groups.
     *
     * @param representative index into the member arrays of the one that gets the fragments
     * @param pollutionRatio share of the group's MS1 intensity that is not the representative's
     */
    record DiaPrecursorGroup(String channel, double[] memberMz, double[] memberRetentionTime,
                             double[] memberIntensity, int representative, double pollutionRatio) {
    }

    /** a group of precursors that cannot be told apart, see {@link DiaPrecursorGroup} */
    default void diaPrecursorGroup(ProcessedSample sample, DiaPrecursorGroup group) {
    }

    /** the whole candidate set of one fragment, see {@link DiaFragmentCompetition} */
    default void diaFragmentCompetition(ProcessedSample sample, String channel, DiaFragmentCompetition competition) {
    }

    /**
     * What a written DIA pseudo spectrum is made of, in quantities that survive normalisation.
     * <p>
     * A spectrum's peaks are stored as absolute intensities and normalised long before anything reads them, so a
     * spectrum assembled from peaks barely above the noise is indistinguishable afterwards from one assembled from
     * strong ones - the normalisation makes tiny peaks large again and the information is gone. Everything here is
     * therefore a ratio against a quantity measured on the same data, never an absolute value: what the channel
     * cannot tell from nothing, what the precursor was worth, and what was actually measured before the assignment
     * decided how much of it this precursor may claim.
     *
     * @param attributedIonCount  the peaks this spectrum was written with, summed
     * @param measuredIonCount    what those fragments measured inside the window, before the factorisation split them
     * @param basePeak            the strongest peak of the spectrum
     * @param noiseLevel          of the channel the fragments were measured in
     * @param precursorIntensity  the precursor's MS1 apex
     * @param meanProbability     mean assignment probability of the peaks, NaN where the strategy has none
     * @param meanLogPValue       mean log p-value of the edges this spectrum's peaks were assigned on, against the
     *                            channel's own decoys; NaN where the strategy assigns without one. More negative is
     *                            better, so a spectrum whose peaks all barely cleared the floor is near it
     * @param bestLogPValueSum    the same evidence over a fixed number of slots: the most significant log p-values of
     *                            this spectrum's peaks, summed, with unfilled slots contributing nothing. A mean lets
     *                            one strongly supported peak stand for a whole spectrum and a minimum lets one weak
     *                            peak condemn it; a fixed budget asks instead whether several peaks are well
     *                            supported, which is what a usable spectrum needs
     * @param meanCandidates      mean number of precursors that competed for these fragments
     * @param pollutionRatio      share of the group's MS1 intensity that is not this precursor's, NaN without grouping
     * @param samples             how many samples contributed, one for a per sample spectrum
     */
    record DiaSpectrumQuality(String stage, double precursorMz, double retentionTime, int peaks,
                              double attributedIonCount, double measuredIonCount, double basePeak,
                              double noiseLevel, double precursorIntensity, double meanProbability,
                              double meanLogPValue, double bestLogPValueSum, double meanCandidates,
                              double pollutionRatio, int samples) {

        /** the total this spectrum claims, in units of what the channel cannot distinguish from nothing */
        public double ionCountOverNoise() {
            return noiseLevel > 0 ? attributedIonCount / noiseLevel : Double.NaN;
        }

        /** the same for the strongest peak, since a large total can be many negligible ones */
        public double basePeakOverNoise() {
            return noiseLevel > 0 ? basePeak / noiseLevel : Double.NaN;
        }

        /** how much of what was measured this precursor was given, which is low exactly when it took someone else's */
        public double claimedShare() {
            return measuredIonCount > 0 ? attributedIonCount / measuredIonCount : Double.NaN;
        }

        /**
         * The fragment current against the precursor's own, which is conserved for a singly charged precursor and
         * should therefore concentrate around one constant per experiment rather than vary by compound. A spectrum
         * far below that constant has lost its fragments to somebody.
         */
        public double conservationRatio() {
            return precursorIntensity > 0 ? attributedIonCount / precursorIntensity : Double.NaN;
        }
    }

    /** what a written DIA pseudo spectrum is made of, see {@link DiaSpectrumQuality} */
    default void diaSpectrumQuality(ProcessedSample sample, String channel, DiaSpectrumQuality quality) {
    }

    /** a peak of a DIA pseudo spectrum, written or dropped */
    default void diaPeakWritten(ProcessedSample sample, String channel, double precursorMz, double fragmentMz,
                                double intensity, double probability, boolean written, String outcome) {
    }

    void moiAccepted(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi);

    void moiRejected(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi);

    void alignMois(ProcessedSample rightSample, MoI left, MoI right);

    void unalignedMoI(ProcessedSample s, MoI moI);

    void moiDeleted(MoI moi);

    void createRect(ProcessedSample sample, Rect r);

    void emptyRect(ProcessedSample sample, Rect r);

    void mergedTrace(ProcessedSample merged, ProcessedSample sample, Rect r, ProjectedTrace projectedTrace, MoI[] moisForSample);

    void assignMs2ToMergedTrace(ProcessedSample sample, ContiguousTrace[] sourceTraces, ProcessedSample merged, ProjectedTrace projectedTrace, MsMsTraceReference[] ids);

    void rejectedForFeatureExtraction(Rect r, MergedTrace merged);

    void noFeatureFound(MergedTrace mergedTrace);

    void importFeatures(MergedTrace mergedTrace, AlignedFeatures[] features);

    void startExtractingCompounds(ProcessedSample mergedSample, MergedTrace mergedTrace);
}
