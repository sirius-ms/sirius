package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Assigns the fragments of a DIA sample to its precursor features, one channel at a time.
 * <p>
 * This is stage one of the assignment: it works within a single sample and knows nothing about the other runs of
 * an experiment. Two things make it deliberately permissive rather than final. The feature boundaries it uses are
 * the preliminary ones from trace picking, and the real ones only exist after alignment. And the abundance of a
 * feature across samples, which is the one piece of evidence orthogonal to co-elution, is not available until
 * features have been imported. Stage two revisits both.
 * <p>
 * Per channel:
 * <ol>
 *     <li>estimate the channel's noise level and pick its fragment chromatograms,</li>
 *     <li>collect the precursor features it could have fragmented, and a set of decoy precursors from outside its
 *     isolation windows,</li>
 *     <li>build the candidate graph twice, once with each, through the same code so that the null distribution
 *     cannot drift away from the criteria it calibrates,</li>
 *     <li>calibrate correlation and specificity against the decoy edges, and</li>
 *     <li>resolve the real graph into one assignment per fragment.</li>
 * </ol>
 * Calibration is per channel where a channel has enough decoys of its own: a crowded low mass window and a sparse
 * high mass one have genuinely different chances of producing an accidental correlation. Below
 * {@link #MIN_DECOY_EDGES} a channel borrows the null distribution pooled over the sample, because a null built
 * from a handful of decoys cannot call anything significant and would silently leave every fragment unassigned.
 * <p>
 * Channels are processed sequentially. Samples are already handled in parallel by the caller, and the fragment
 * traces of all channels of one sample share a store, so parallelising here would buy little for the concurrency
 * questions it would raise.
 */
public class DiaDeconvolution {

    /**
     * How significant an edge has to be before it carries any weight, in units of log p-value. Applied to the sum
     * of the correlation and specificity p-values, as {@code AdductNetwork} sums its independent signals.
     */
    private final double significanceFloor;

    private final double unassignedWeight;

    private final double minProbability;

    private final int maxDecoyPrecursors;

    /**
     * How many of a channel's scan points a fragment has to be followed across to be picked at all.
     * <p>
     * Three is what MS1 trace picking uses, and for a DIA channel it is too strict: a peak is about four scan points
     * wide there, so one missed scan discards the fragment. Against a DDA measurement of the same sample, three
     * points reach only about three quarters of the fragments that the raw scans contain at all.
     * <p>
     * Two still leaves a shape, weak on its own but a real one once the samples are pooled in stage two, where two
     * points per sample is exactly what the pooled correlation accepts. One leaves none, and worse, the signals that
     * remain for a single point - specificity and abundance - cannot tell two precursors of the same window apart,
     * which is the decision that has to be made.
     */
    public static final int DEFAULT_MIN_FRAGMENT_SCAN_POINTS = 2;

    @Getter @Setter
    private int minFragmentScanPoints = DEFAULT_MIN_FRAGMENT_SCAN_POINTS;

    /**
     * Whether a fragment several precursors could have produced is split between them, see
     * {@link FragmentIntensityShares}. Only useful to turn off in order to measure what it does.
     */
    @Getter @Setter
    private boolean intensityShares = true;

    /** which quantile of a channel's intensities counts as its noise floor, see {@link DiaChannelNoise} */
    @Getter @Setter
    private double noiseQuantile = DiaChannelNoise.DEFAULT_QUANTILE;

    /**
     * Whether a candidate's MS1 intensity weighs on the assignment, see
     * {@link DiaFragmentResolver.PrecursorCapacity}. Only useful to turn off in order to measure what it does.
     */
    @Getter @Setter
    private boolean intensityPrior = true;

    public DiaDeconvolution() {
        this(DiaFragmentResolver.DEFAULT_SIGNIFICANCE_FLOOR, DiaFragmentResolver.DEFAULT_UNASSIGNED_WEIGHT,
                FragmentAssignment.DEFAULT_MIN_PROBABILITY, DiaDecoys.DEFAULT_MAX_DECOY_PRECURSORS);
    }

    public DiaDeconvolution(double significanceFloor, double unassignedWeight, double minProbability,
                            int maxDecoyPrecursors) {
        this.significanceFloor = significanceFloor;
        this.unassignedWeight = unassignedWeight;
        this.minProbability = minProbability;
        this.maxDecoyPrecursors = maxDecoyPrecursors;
    }

    /**
     * What one channel yielded. The graph is kept, since the assignments index into its fragment and precursor
     * lists and stage two will want the candidate sets again.
     */
    @Getter
    public static class ChannelResult {
        private final DiaChannel channel;
        private final DiaAssignmentGraph graph;
        private final DiaFragmentResolver.Result resolution;
        private final float noiseLevel;
        private final int fragmentSeeds;
        private final int decoyPrecursors;
        private final int decoyEdges;
        /** false if this channel had too few decoys of its own and borrowed the sample wide null distribution */
        private final boolean ownNullDistribution;

        ChannelResult(DiaChannel channel, DiaAssignmentGraph graph, DiaFragmentResolver.Result resolution,
                      float noiseLevel, int fragmentSeeds, int decoyPrecursors, int decoyEdges,
                      boolean ownNullDistribution) {
            this.channel = channel;
            this.graph = graph;
            this.resolution = resolution;
            this.noiseLevel = noiseLevel;
            this.fragmentSeeds = fragmentSeeds;
            this.decoyPrecursors = decoyPrecursors;
            this.decoyEdges = decoyEdges;
            this.ownNullDistribution = ownNullDistribution;
        }

        /** @return the assignment of the fragment at this index of {@link DiaAssignmentGraph#getFragments()} */
        public FragmentAssignment assignmentOf(int fragmentIndex) {
            return resolution.getAssignments()[fragmentIndex];
        }
    }

    @Getter
    public static class Result {
        private final List<ChannelResult> channels;
        /** decoy edges pooled over the whole sample, the fallback for channels that have too few of their own */
        private final int pooledDecoyEdges;

        Result(List<ChannelResult> channels, int pooledDecoyEdges) {
            this.channels = channels;
            this.pooledDecoyEdges = pooledDecoyEdges;
        }

        /**
         * @return true if not even the pooled decoys are enough to call anything significant, in which case no
         * fragment can be assigned at all and the reason is the sample, not the data
         */
        public boolean nullDistributionTooThin() {
            return pooledDecoyEdges < MIN_DECOY_EDGES;
        }

        public int numberOfFragments() {
            int sum = 0;
            for (ChannelResult c : channels) sum += c.getGraph().getFragments().size();
            return sum;
        }

        public int numberOfAssignedFragments() {
            int sum = 0;
            for (ChannelResult c : channels) sum += c.getResolution().assignedFragments();
            return sum;
        }

        public int numberOfAmbiguousFragments() {
            int sum = 0;
            for (ChannelResult c : channels) sum += c.getResolution().ambiguousFragments();
            return sum;
        }

        public int numberOfPrecursorFeatures() {
            int sum = 0;
            for (ChannelResult c : channels) sum += c.getGraph().getPrecursors().size();
            return sum;
        }

        public int numberOfFeaturesWithoutShapeEvidence() {
            int sum = 0;
            for (ChannelResult c : channels) sum += c.getGraph().getFeaturesWithoutShapeEvidence();
            return sum;
        }

        /**
         * The numbers to look at first when a DIA sample behaves oddly, and the ones that tell us whether the
         * estimates this was designed against hold on real data.
         */
        public String describe() {
            final StringBuilder buf = new StringBuilder();
            buf.append(String.format(Locale.US,
                    "DIA deconvolution: %d channels, %d precursor features, %d fragments, %d assigned, %d ambiguous%n",
                    channels.size(), numberOfPrecursorFeatures(), numberOfFragments(), numberOfAssignedFragments(),
                    numberOfAmbiguousFragments()));
            if (nullDistributionTooThin()) {
                buf.append(String.format(Locale.US, "  WARNING: only %d decoy edges in the whole sample, which is "
                        + "too few to call anything significant; no fragment can be assigned%n", pooledDecoyEdges));
            }
            for (ChannelResult c : channels) {
                buf.append(String.format(Locale.US,
                        "  channel %d [%.1f-%.1f]: noise %.0f, %d features (%d without shape), %d fragments from %d seeds, "
                                + "%d edges, %d decoy edges, largest component %d, %d exact / %d iterative%n",
                        c.getChannel().getChannelId(), c.getChannel().lowestMz(), c.getChannel().highestMz(),
                        c.getNoiseLevel(), c.getGraph().getPrecursors().size(),
                        c.getGraph().getFeaturesWithoutShapeEvidence(), c.getGraph().getFragments().size(),
                        c.getFragmentSeeds(), c.getGraph().getPrecursorEdges().size(), c.getDecoyEdges(),
                        c.getResolution().largestComponent(), c.getResolution().getComponentsSolvedExactly(),
                        c.getResolution().getComponentsSolvedIteratively()));
                if (!c.isOwnNullDistribution()) {
                    buf.append("    (too few decoys of its own, using the null distribution of the whole sample)\n");
                }
            }
            return buf.toString();
        }
    }

    /**
     * A null distribution needs a certain number of decoys before it can call anything significant at all: with
     * {@code n} decoys the strongest possible statement is {@code log(1/(n+1))}, so below twenty decoy edges
     * nothing can clear a significance floor of three and every weight comes out zero. A channel with fewer
     * borrows the null distribution pooled over the whole sample.
     */
    public static final int MIN_DECOY_EDGES = 50;

    public Result deconvolute(ProcessedSample sample, DiaScanIndex index) {
        // one pass over the MS1 traces, dispatching each of them to the channels it is a precursor for and to the
        // channels it can serve as a decoy for. The trace store is on disc, so reading it once per channel would
        // mean deserialising every trace of the sample as many times as there are channels.
        final int channelCount = index.numberOfChannels();
        final List<List<PrecursorFeature>> realFeatures = new ArrayList<>(channelCount);
        final List<List<PrecursorFeature>> decoyCandidates = new ArrayList<>(channelCount);
        for (int c = 0; c < channelCount; ++c) {
            realFeatures.add(new ArrayList<>());
            decoyCandidates.add(new ArrayList<>());
        }
        for (ContiguousTrace trace : sample.getStorage().getTraceStorage()) {
            final TraceSegment[] segments = trace.getSegments();
            if (segments == null || segments.length == 0) continue;
            final double mz = trace.averagedMz();
            for (int c = 0; c < channelCount; ++c) {
                final DiaChannel channel = index.getChannels().get(c);
                if (channel.covers(mz)) {
                    for (TraceSegment segment : segments) {
                        PrecursorFeature.of(channel, trace, segment).ifPresent(realFeatures.get(c)::add);
                    }
                } else if (DiaDecoys.isEligible(channel, mz)) {
                    for (TraceSegment segment : segments) {
                        PrecursorFeature.decoy(channel, trace, segment).ifPresent(decoyCandidates.get(c)::add);
                    }
                }
            }
        }

        // Phase one: pick the fragments of every channel and build both graphs. Both are needed before anything
        // can be resolved, because a channel with too few decoys of its own has to fall back to the pool.
        final List<DiaChannel> channels = index.getChannels();
        final List<Draft> drafts = new ArrayList<>(channelCount);
        for (int c = 0; c < channelCount; ++c) {
            final DiaChannel channel = channels.get(c);
            final float noiseLevel = DiaChannelNoise.estimate(sample, channel, noiseQuantile);
            final DiaChannelFragments.Result fragments = DiaChannelFragments.pickAll(sample, channel, noiseLevel,
                    minFragmentScanPoints);
            final List<PrecursorFeature> decoys = DiaDecoys.thin(decoyCandidates.get(c), maxDecoyPrecursors);
            drafts.add(new Draft(channel, noiseLevel, fragments.seedsTried(), decoys.size(),
                    DiaAssignmentGraph.build(channel, realFeatures.get(c), fragments.traces()),
                    DiaAssignmentGraph.build(channel, decoys, fragments.traces())));
        }

        // Phase two: the null distributions, pooled over the sample as a fallback
        final Nulls pooled = new Nulls();
        for (Draft draft : drafts) pooled.addAll(draft.decoyGraph);
        pooled.calibrate();

        // Phase three: resolve
        final List<ChannelResult> results = new ArrayList<>(channelCount);
        for (Draft draft : drafts) {
            final int decoyEdges = draft.decoyGraph.getPrecursorEdges().size();
            final boolean ownNull = decoyEdges >= MIN_DECOY_EDGES;
            final Nulls nulls;
            if (ownNull) {
                nulls = new Nulls();
                nulls.addAll(draft.decoyGraph);
                nulls.calibrate();
            } else {
                nulls = pooled;
            }
            final List<PrecursorFeature> features = draft.graph.getPrecursors();
            final DiaFragmentResolver.Result resolution = DiaFragmentResolver.resolve(draft.graph,
                    edge -> DiaFragmentResolver.weightFromLogPValue(nulls.logPvalue(edge), significanceFloor),
                    intensityPrior ? precursorIndex -> features.get(precursorIndex).getApexIntensity()
                            : DiaFragmentResolver.UNIFORM_CAPACITY,
                    List.of(), unassignedWeight, minProbability, DiaFragmentResolver.DEFAULT_EXACT_SOLVER_LIMIT,
                    DiaFragmentResolver.DEFAULT_ITERATIVE_STEPS);
            results.add(new ChannelResult(draft.channel, draft.graph, resolution, draft.noiseLevel,
                    draft.fragmentSeeds, draft.decoyPrecursors, decoyEdges, ownNull));
        }
        return new Result(results, pooled.decoyCount());
    }

    private record Draft(DiaChannel channel, float noiseLevel, int fragmentSeeds, int decoyPrecursors,
                         DiaAssignmentGraph graph, DiaAssignmentGraph decoyGraph) {
    }

    /**
     * The two null distributions of an edge's signals. They are calibrated against the same decoys and their log
     * p-values add up, since they are independent, exactly as {@code AdductNetwork.PValueStats} sums its own.
     */
    private static class Nulls {
        private final DiaCorrelationPValues correlation = new DiaCorrelationPValues();
        private final DiaCorrelationPValues specificity = new DiaCorrelationPValues();

        void addAll(DiaAssignmentGraph decoyGraph) {
            for (DiaAssignmentGraph.PrecursorEdge edge : decoyGraph.getPrecursorEdges()) {
                correlation.addDecoy(edge.correlation());
                specificity.addDecoy(edge.specificity(), edge.correlation().datapoints());
            }
        }

        void calibrate() {
            correlation.calibrate();
            specificity.calibrate();
        }

        int decoyCount() {
            return correlation.numberOfDecoys();
        }

        double logPvalue(DiaAssignmentGraph.PrecursorEdge edge) {
            return correlation.logPvalue(edge.correlation())
                    + specificity.logPvalue(edge.specificity(), edge.correlation().datapoints());
        }
    }
}
