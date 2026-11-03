package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.statistics.TraceCorrelationStatistics;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.utils.Tracker;
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

    /**
     * How likely an assignment has to be to be kept at all.
     * <p>
     * Note what this is a threshold on: a share of the fragment over its candidate set, not a confidence in any one
     * candidate. The two coincide only when a fragment has few candidates. Where it has many comparably intense ones
     * the share of even a perfectly correlating candidate is bounded by one over their number, so a fixed threshold
     * rejects by how crowded the isolation window is rather than by the quality of the evidence - which the
     * significance floor already judges.
     */
    @Getter @Setter
    private double minProbability;

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
     * Whether the precursors of a channel are first grouped into the sets it cannot separate, and only one feature
     * per group competes for the fragments.
     * <p>
     * The whole assignment then works on the same nodes, edges, calibration and shares as before - a group is
     * represented by one of its own features, so nothing downstream can tell the difference - but two things change.
     * Members of a group other than its representative are no longer candidates for anything, so they receive no
     * spectrum, as a data dependent acquisition also gives none to a peak it never selected. And a precursor too weak
     * for its co-elution to be judged at all is dropped rather than competing on evidence that does not exist.
     *
     * @see DiaPrecursorGroups
     */
    @Getter @Setter
    private boolean groupIndistinguishablePrecursors = false;

    /** how likely a fragment has to be to appear in a spectrum, see {@link #getMinProbability()} */
    @Getter @Setter
    private double minProbabilityForSpectrum = DiaPseudoSpectra.DEFAULT_MIN_PROBABILITY;

    /** see {@link DiaPseudoSpectra#write}; zero keeps every candidate that cleared the significance floor */
    @Getter @Setter
    private double minRelativeProbabilityForSpectrum = 0d;

    /**
     * How intense a member of a precursor group has to be, relative to its most intense member, to receive the
     * group's spectrum as well. Only meaningful with {@link #groupIndistinguishablePrecursors}.
     */
    @Getter @Setter
    private double minReportedShareOfGroup = DiaPrecursorGroups.DEFAULT_MIN_REPORTED_SHARE;

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
        /**
         * Per precursor, the mean log p-value of the edges it was assigned, or NaN where it was assigned none. How
         * far the evidence cleared the floor is not the same question as how many fragments there were, and nothing
         * downstream could reconstruct it: the p-values exist only while the channel is being resolved.
         */
        private final double[] meanLogPValue;
        /**
         * Per precursor, the sum of the most significant log p-values of the edges it was assigned, over
         * {@link #EVIDENCE_SLOTS} slots with unfilled ones contributing nothing.
         */
        private final double[] bestLogPValueSum;
        /**
         * Per precursor of the graph, the other members of its group that are reported with it, see
         * {@link DiaPrecursorGroups.Group#reportedMembers}. Empty without grouping, where every precursor is its own
         * node and speaks only for itself.
         */
        private final List<List<PrecursorFeature>> coReported;
        private final DiaFragmentResolver.Result resolution;
        private final float noiseLevel;
        private final int fragmentSeeds;
        private final int decoyPrecursors;
        private final int decoyEdges;
        /** false if this channel had too few decoys of its own and borrowed the sample wide null distribution */
        private final boolean ownNullDistribution;

        ChannelResult(DiaChannel channel, DiaAssignmentGraph graph, DiaFragmentResolver.Result resolution,
                      float noiseLevel, int fragmentSeeds, int decoyPrecursors, int decoyEdges,
                      boolean ownNullDistribution, List<List<PrecursorFeature>> coReported,
                      double[] meanLogPValue, double[] bestLogPValueSum) {
            this.bestLogPValueSum = bestLogPValueSum;
            this.coReported = coReported;
            this.meanLogPValue = meanLogPValue;
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

    /**
     * Hands every candidate edge of a resolved channel to the tracker.
     * <p>
     * The probability is recomputed here rather than read out of the assignment, because the assignment only keeps
     * what cleared {@link #minProbability} and the question a tracker is pointed at is usually why something did not.
     * It is the same division the resolver does, and the two would have to be changed together.
     */
    private void report(ProcessedSample sample, DiaAssignmentGraph graph, Nulls nulls,
                        DiaFragmentResolver.Result resolution, Tracker tracker) {
        final List<PrecursorFeature> precursors = graph.getPrecursors();
        final List<ContiguousTrace> fragments = graph.getFragments();
        final String channel = graph.getChannel().describe();
        for (int f = 0; f < fragments.size(); ++f) {
            final List<DiaAssignmentGraph.PrecursorEdge> edges = graph.edgesTo(f);
            if (edges.isEmpty()) continue;
            final ContiguousTrace fragment = fragments.get(f);
            final FragmentAssignment assignment = resolution.getAssignments()[f];
            // the resolver drops zero weight edges before normalising the capacities, so the largest capacity is
            // taken over the candidates that survive the floor, not over all of them
            double total = Math.max(0d, unassignedWeight), largestCapacity = 0d;
            final double[] weights = new double[edges.size()];
            for (DiaAssignmentGraph.PrecursorEdge edge : edges) {
                if (DiaFragmentResolver.weightFromLogPValue(nulls.logPvalue(edge), significanceFloor) <= 0) continue;
                largestCapacity = Math.max(largestCapacity, intensityPrior
                        ? precursors.get(edge.precursorIndex()).getApexIntensity() : 1d);
            }
            for (int k = 0; k < edges.size(); ++k) {
                final DiaAssignmentGraph.PrecursorEdge edge = edges.get(k);
                final double weight = DiaFragmentResolver.weightFromLogPValue(nulls.logPvalue(edge),
                        significanceFloor);
                final double capacity = intensityPrior
                        ? precursors.get(edge.precursorIndex()).getApexIntensity() : 1d;
                weights[k] = weight * (largestCapacity > 0 ? Math.max(0d, capacity) / largestCapacity : 1d);
                total += weights[k];
            }
            // the candidate set as a whole, so a fragment lost to many weak competitors can be told from one lost
            // to a few strong ones
            double strongestIntensity = 0d;
            for (DiaAssignmentGraph.PrecursorEdge edge : edges) {
                strongestIntensity = Math.max(strongestIntensity,
                        precursors.get(edge.precursorIndex()).getApexIntensity());
            }
            double strongestWeight = 0d, strongestMz = Double.NaN;
            double aboveHalf = 0d, aboveTenth = 0d, belowTenth = 0d;
            int countAboveHalf = 0, countAboveTenth = 0;
            for (int k = 0; k < edges.size(); ++k) {
                final PrecursorFeature candidate = precursors.get(edges.get(k).precursorIndex());
                if (weights[k] > strongestWeight) {
                    strongestWeight = weights[k];
                    strongestMz = candidate.getMz();
                }
                final double relative = strongestIntensity > 0
                        ? candidate.getApexIntensity() / strongestIntensity : 0d;
                if (relative >= 0.5) {
                    aboveHalf += weights[k];
                    ++countAboveHalf;
                } else if (relative >= 0.1) {
                    aboveTenth += weights[k];
                    ++countAboveTenth;
                } else {
                    belowTenth += weights[k];
                }
            }
            tracker.diaFragmentCompetition(sample, channel, new Tracker.DiaFragmentCompetition(
                    fragment.averagedMz(), graph.getChannel().getAxis().getRetentionTimeAt(fragment.apex()),
                    fragment.endId() - fragment.startId() + 1, edges.size(), total, Math.max(0d, unassignedWeight),
                    strongestWeight, strongestMz, strongestIntensity, countAboveHalf, countAboveTenth,
                    aboveHalf, aboveTenth, belowTenth));

            for (int k = 0; k < edges.size(); ++k) {
                final DiaAssignmentGraph.PrecursorEdge edge = edges.get(k);
                final PrecursorFeature precursor = precursors.get(edge.precursorIndex());
                final double probability = total > 0 ? weights[k] / total : 0d;
                double stored = 0d;
                for (int rank = 0; rank < assignment.size(); ++rank) {
                    if (assignment.precursorIndexAt(rank) == edge.precursorIndex()) {
                        stored = assignment.probabilityAt(rank);
                    }
                }
                final String outcome;
                if (weights[k] <= 0) {
                    outcome = "no weight: below the significance floor of " + significanceFloor;
                } else if (stored <= 0) {
                    outcome = String.format(Locale.US, "dropped, %.4f is below the minimum probability of %.2f "
                            + "(%d competitors, unassigned weight %.1f)", probability, minProbability,
                            edges.size() - 1, unassignedWeight);
                } else {
                    outcome = "assigned";
                }
                tracker.diaFragmentJudged(sample, channel, new Tracker.DiaFragmentJudgement("per sample",
                        precursor.getMz(), precursor.getRetentionTime(), precursor.getApexIntensity(),
                        fragment.averagedMz(),
                        graph.getChannel().getAxis().getRetentionTimeAt(fragment.apex()), fragment.apexIntensity(),
                        edge.correlation().datapoints(), edge.correlation().correlation(), nulls.logPvalue(edge),
                        edge.specificity(), weights[k], stored, Double.NaN, edges.size(), outcome));
            }
        }
    }

    @Getter
    public static class Result {
        private final List<ChannelResult> channels;
        /** decoy edges pooled over the whole sample, the fallback for channels that have too few of their own */
        private final int pooledDecoyEdges;
        /** what the precursor grouping did, or empty if it was not used */
        private final String grouping;

        Result(List<ChannelResult> channels, int pooledDecoyEdges, String grouping) {
            this.channels = channels;
            this.pooledDecoyEdges = pooledDecoyEdges;
            this.grouping = grouping;
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
            buf.append(grouping);
            if (nullDistributionTooThin()) {
                buf.append(String.format(Locale.US, "  WARNING: only %d decoy edges in the whole sample, which is "
                        + "too few to call anything significant; no fragment can be assigned%n", pooledDecoyEdges));
            }
            for (ChannelResult c : channels) {
                buf.append(String.format(Locale.US,
                        "  channel %d [%.1f-%.1f]: noise %.0f, %d features (%d without shape), %d fragments from %d seeds, "
                                + "%d edges, %d decoy edges%n",
                        c.getChannel().getChannelId(), c.getChannel().lowestMz(), c.getChannel().highestMz(),
                        c.getNoiseLevel(), c.getGraph().getPrecursors().size(),
                        c.getGraph().getFeaturesWithoutShapeEvidence(), c.getGraph().getFragments().size(),
                        c.getFragmentSeeds(), c.getGraph().getPrecursorEdges().size(), c.getDecoyEdges()));
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
        return deconvolute(sample, index, new Tracker.NOOP());
    }

    public Result deconvolute(ProcessedSample sample, DiaScanIndex index, Tracker tracker) {
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

        final GroupingReport grouping = new GroupingReport();
        final List<DiaPrecursorSurvival.Result> survival = new ArrayList<>(channelCount);
        // Phase one: pick the fragments of every channel and build both graphs. Both are needed before anything
        // can be resolved, because a channel with too few decoys of its own has to fall back to the pool.
        final List<DiaChannel> channels = index.getChannels();
        final List<Draft> drafts = new ArrayList<>(channelCount);
        for (int c = 0; c < channelCount; ++c) {
            final DiaChannel channel = channels.get(c);
            final float noiseLevel = DiaChannelNoise.estimate(sample, channel, noiseQuantile);
            final DiaChannelFragments.Result fragments = DiaChannelFragments.pickAll(sample, channel, noiseLevel,
                    new de.unijena.bioinf.lcms.trace.segmentation.NoSegmentation(), minFragmentScanPoints, tracker);
            final List<PrecursorFeature> decoys = DiaDecoys.thin(decoyCandidates.get(c), maxDecoyPrecursors);
            // which of the channel's features actually compete for its fragments
            final Nodes nodes = groupIndistinguishablePrecursors
                    ? precursorNodesOf(channel, realFeatures.get(c), sample, grouping, tracker)
                    : Nodes.of(realFeatures.get(c));
            final DiaAssignmentGraph real = DiaAssignmentGraph.build(channel, nodes.precursors(), fragments.traces());
            DiaAssignmentGraph decoyGraph = DiaAssignmentGraph.build(channel, decoys, fragments.traces());
            int decoyPrecursors = decoys.size();
            if (decoyGraph.getPrecursorEdges().size() < MIN_DECOY_EDGES) {
                // An all ion channel isolates the whole mass range, so no precursor lies outside it and the usual
                // decoys do not exist. What is left is the pair that is wrong for one reason only: a fragment
                // heavier than its precursor cannot have come from it, and nothing else about the pair changes.
                final List<PrecursorFeature> violating =
                        DiaDecoys.thin(nodes.precursors(), maxDecoyPrecursors);
                final DiaAssignmentGraph byMass = DiaAssignmentGraph.build(channel, violating, fragments.traces(),
                        DiaAssignmentGraph.DEFAULT_APEX_TOLERANCE,
                        DiaAssignmentGraph.DEFAULT_MAX_FRAGMENT_MZ_ABOVE_PRECURSOR,
                        DiaAssignmentGraph.MassRelation.HEAVIER_THAN_PRECURSOR);
                if (byMass.getPrecursorEdges().size() > decoyGraph.getPrecursorEdges().size()) {
                    decoyGraph = byMass;
                    decoyPrecursors = violating.size();
                }
            }
            drafts.add(new Draft(channel, noiseLevel, fragments.seedsTried(), decoyPrecursors, real,
                    decoyGraph, nodes.coReported()));
            survival.add(DiaPrecursorSurvival.of(real,
                    sample.getStorage().getStatistics().getMs1MassDeviationWithinTraces()));
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
                    unassignedWeight, minProbability);
            final double[][] evidence = evidenceOf(draft.graph, resolution, nulls);
            results.add(new ChannelResult(draft.channel, draft.graph, resolution, draft.noiseLevel,
                    draft.fragmentSeeds, draft.decoyPrecursors, decoyEdges, ownNull, draft.coReported,
                    evidence[0], evidence[1]));
            if (tracker.tracksDia()) {
                report(sample, draft.graph, nulls, resolution, tracker);
            }
        }
        final StringBuilder survivalReport = new StringBuilder();
        for (int c = 0; c < survival.size(); ++c) {
            survivalReport.append(String.format(Locale.US, "  channel %s: %s%n",
                    channels.get(c).describe(), survival.get(c).describe()));
        }
        return new Result(results, pooled.decoyCount(), grouping.describe() + survivalReport);
    }

    /**
     * One feature per set the channel cannot separate, the most intense of each, plus nothing at all for the
     * features it cannot judge. The pollution of every group is recorded on the way, since the spectrum of a
     * representative is chimeric by exactly that much.
     */
    /** the precursors that compete for a channel's fragments, and who else each of them speaks for */
    record Nodes(List<PrecursorFeature> precursors, List<List<PrecursorFeature>> coReported) {
        static Nodes of(List<PrecursorFeature> precursors) {
            final List<List<PrecursorFeature>> none = new ArrayList<>(precursors.size());
            for (int k = 0; k < precursors.size(); ++k) none.add(List.of());
            return new Nodes(precursors, none);
        }
    }

    /** one group, in the terms the tracker understands: the masses, times and intensities of its members */
    private static void reportGroup(DiaChannel channel, List<PrecursorFeature> features,
                                    DiaPrecursorGroups.Group group, Tracker tracker, ProcessedSample sample) {
        final int[] members = group.getMembers();
        final double[] mz = new double[members.length];
        final double[] rt = new double[members.length];
        final double[] intensity = new double[members.length];
        int representative = 0;
        for (int k = 0; k < members.length; ++k) {
            final PrecursorFeature member = features.get(members[k]);
            mz[k] = member.getMz();
            rt[k] = member.getRetentionTime();
            intensity[k] = member.getApexIntensity();
            if (members[k] == group.getRepresentative()) representative = k;
        }
        tracker.diaPrecursorGroup(sample, new Tracker.DiaPrecursorGroup(channel.describe(), mz, rt, intensity,
                representative, group.getPollutionRatio()));
    }

    private Nodes precursorNodesOf(DiaChannel channel, List<PrecursorFeature> features,
                                   ProcessedSample sample, GroupingReport report, Tracker tracker) {
        final TraceCorrelationStatistics statistics = sample.getStorage().getStatistics().getTraceCorrelations();
        final double noiseLevel = TraceCorrelationStatistics.medianNoiseLevel(sample.getStorage().getStatistics());
        final DiaPrecursorGroups.Result grouped = DiaPrecursorGroups.of(channel, features, statistics, noiseLevel,
                DiaPrecursorGroups.DEFAULT_QUANTILE, DiaPrecursorGroups.DEFAULT_MAX_APEX_DISTANCE);
        final List<PrecursorFeature> representatives = new ArrayList<>(grouped.groups().size());
        final List<List<PrecursorFeature>> coReported = new ArrayList<>(grouped.groups().size());
        final boolean tracked = tracker.tracksDia();
        for (DiaPrecursorGroups.Group group : grouped.groups()) {
            representatives.add(features.get(group.getRepresentative()));
            // the members the group also speaks for, which receive the same spectrum
            final int[] reported = group.reportedMembers(features, minReportedShareOfGroup);
            final List<PrecursorFeature> others = new ArrayList<>(reported.length - 1);
            for (int member : reported) {
                if (member != group.getRepresentative()) others.add(features.get(member));
            }
            coReported.add(others);
            if (tracked) reportGroup(channel, features, group, tracker, sample);
        }
        report.add(features.size(), grouped);
        return new Nodes(representatives, coReported);
    }

    /** what the grouping did over all channels of a sample, for the log and for the report */
    static final class GroupingReport {
        private int features, groups, ambiguous, largest;
        private double pollution;

        void add(int featureCount, DiaPrecursorGroups.Result result) {
            features += featureCount;
            groups += result.groups().size();
            ambiguous += result.numberOfAmbiguousGroups();
            for (DiaPrecursorGroups.Group group : result.groups()) {
                largest = Math.max(largest, group.size());
                if (group.isAmbiguous()) pollution += group.getPollutionRatio();
            }
        }

        String describe() {
            if (features == 0) return "";
            return String.format(Locale.US, "  precursor grouping: %d features, %d groups "
                            + "(%d ambiguous, largest %d, mean pollution %.2f)%n", features, groups,
                    ambiguous, largest, ambiguous == 0 ? 0d : pollution / ambiguous);
        }
    }

    /**
     * How many of a spectrum's best supported peaks the evidence budget asks about.
     * <p>
     * A mean over the assignment lets one strongly supported peak speak for a whole spectrum, and the weakest edge
     * lets one poorly supported peak condemn it. Neither is what makes a spectrum usable, which is that several of its
     * peaks are well supported - so the evidence is summed over a fixed number of slots, filled with the most
     * significant peaks, and a spectrum with fewer than that many simply leaves slots empty.
     */
    static final int EVIDENCE_SLOTS = 5;

    /**
     * Per precursor, two summaries of the evidence behind the fragments it was assigned: the mean log p-value, and
     * the sum over the {@link #EVIDENCE_SLOTS} most significant of them.
     * <p>
     * Both taken over the assignment rather than over the candidates, because what a spectrum is worth is decided by
     * the evidence behind the fragments it kept, not behind the ones it lost.
     *
     * @return {@code {mean, bestSum}}, each indexed by precursor, the mean NaN where nothing was assigned
     */
    private static double[][] evidenceOf(DiaAssignmentGraph graph, DiaFragmentResolver.Result resolution,
                                         Nulls nulls) {
        final int n = graph.getPrecursors().size();
        final double[] sum = new double[n];
        final int[] counts = new int[n];
        // the most significant, i.e. most negative, log p-values seen for each precursor so far
        final double[][] best = new double[n][];
        for (DiaAssignmentGraph.PrecursorEdge edge : graph.getPrecursorEdges()) {
            final FragmentAssignment assignment = resolution.getAssignments()[edge.fragmentIndex()];
            for (int rank = 0; rank < assignment.size(); ++rank) {
                if (assignment.precursorIndexAt(rank) != edge.precursorIndex()) continue;
                final int p = edge.precursorIndex();
                final double logP = nulls.logPvalue(edge);
                sum[p] += logP;
                ++counts[p];
                if (best[p] == null) best[p] = new double[EVIDENCE_SLOTS];
                keepBest(best[p], logP);
                break;
            }
        }
        final double[] bestSum = new double[n];
        for (int p = 0; p < n; ++p) {
            sum[p] = counts[p] == 0 ? Double.NaN : sum[p] / counts[p];
            if (best[p] == null) {
                bestSum[p] = Double.NaN;
                continue;
            }
            double total = 0d;
            for (double value : best[p]) total += value;
            bestSum[p] = total;
        }
        return new double[][]{sum, bestSum};
    }

    /**
     * Inserts a log p-value into the running list of the most significant ones, which is sorted ascending and
     * initialised to zero - the value of an empty slot, since zero log p is no evidence at all.
     */
    static void keepBest(double[] best, double logP) {
        if (logP >= best[best.length - 1]) return;
        int at = best.length - 1;
        while (at > 0 && best[at - 1] > logP) {
            best[at] = best[at - 1];
            --at;
        }
        best[at] = logP;
    }

    private record Draft(DiaChannel channel, float noiseLevel, int fragmentSeeds, int decoyPrecursors,
                         DiaAssignmentGraph graph, DiaAssignmentGraph decoyGraph,
                         List<List<PrecursorFeature>> coReported) {
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
