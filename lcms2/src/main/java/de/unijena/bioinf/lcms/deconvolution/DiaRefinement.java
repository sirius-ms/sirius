package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stage two of the DIA assignment: the same decision as stage one, made once for all samples over the final feature
 * boundaries.
 * <p>
 * Stage one had to work per sample and with the preliminary segments of trace picking, and it has an unavoidable
 * limit: a channel samples a peak four to nine times, which cannot separate two compounds that co-elute in one
 * isolation window. This is where the two things that can help become available. The feature boundaries are the
 * consensus over the aligned samples, so the window a correlation is measured over is the one the compound really
 * occupies. And the same pair can be judged in every sample at once, which multiplies the evidence and adds a signal
 * that is not co-elution at all - whether the fragment's abundance follows its precursor's across samples.
 * <p>
 * What it may not do is invent spectra. Merging copied the MS2 references into the projected traces, so nothing
 * added now would ever be read; the peaks of a spectrum, on the other hand, are read back when features are
 * imported. Stage one is therefore the candidate generator and this revises what it wrote, which is also why the
 * segmentation is unaffected: {@code SegmentMergedFeatures} uses the positions of MS2 references as points of
 * interest, and those do not change.
 */
public class DiaRefinement {

    /** how far a pseudo spectrum's precursor mass may differ from that of the feature it is mapped to */
    private static final Deviation PRECURSOR_IDENTITY = new Deviation(20, 0.005);

    /**
     * Which quantile of the decoy edges' combined evidence an edge has to beat before it carries any weight.
     * <p>
     * Stage one applies a fixed floor of three nats to the sum of its two signals. That number cannot simply be
     * carried over here, because this sums three: a correctly calibrated p-value contributes about one nat on
     * average even when the signal behind it is pure noise, so a third term shifts every edge upwards against an
     * unchanged floor and admits edges that say nothing. Measuring the floor instead makes it independent of how
     * many signals there are - the decoys get exactly the same terms, so whatever a noise signal adds is added to
     * the floor as well.
     * <p>
     * This is the knob that trades precision against recall, and the default is where it measured best against a DDA
     * measurement of the same sample: about a fifth more of the reference fragments recovered than stage one alone,
     * at the same per peak agreement. The strict end (0.99, a floor of about 6.5 on the test file) reaches a
     * distinctly better agreement per peak but leaves a tenth of the compounds without any MS/MS at all, and the
     * permissive end (0, i.e. stage one's fixed floor) admits a large number of peaks the reference does not confirm.
     */
    public static final double DEFAULT_DECOY_QUANTILE = 0.9;

    private final double significanceFloor;

    private final double unassignedWeight;

    private final double minProbability;

    private final int maxDecoyPrecursors;

    /** see {@link DiaFragmentResolver.PrecursorCapacity} */
    @Getter @Setter
    private boolean intensityPrior = true;

    /**
     * What to do with a spectrum for which stage two finds no fragment at all. Keeping stage one's peaks would be
     * the conservative choice, but it measured worse and it also makes the result hard to read at all, since a
     * spectrum is then either stage one's or stage two's and the two cannot be told apart afterwards. Replacing it
     * acts on stage two's verdict that nothing in that window belongs to that feature, which is a verdict the
     * comparison against a DDA measurement supports: the peaks dropped this way are confirmed by the reference three
     * times less often than the ones kept.
     */
    @Getter @Setter
    private boolean keepUnrefinedSpectra = false;

    /** how likely a fragment has to be to appear in a spectrum, as in {@link DiaPseudoSpectra} */
    @Getter @Setter
    private double minProbabilityForSpectrum = DiaPseudoSpectra.DEFAULT_MIN_PROBABILITY;

    /** see {@link FragmentIntensityShares}; only useful to turn off in order to measure what it does */
    @Getter @Setter
    private boolean intensityShares = true;

    /** see {@link #DEFAULT_DECOY_QUANTILE}; set to zero to use the fixed floor of stage one instead */
    @Getter @Setter
    private double decoyQuantile = DEFAULT_DECOY_QUANTILE;

    /**
     * How many of a channel's scan intervals the same fragment's apex may differ by between samples, on top of what
     * the alignment expects.
     * <p>
     * A channel scans every couple of seconds and a fragment's apex is whichever of those scans happened to catch it
     * highest, so two samples can put the same apex a whole scan point apart for no reason but where the scan fell.
     * The alignment's own retention time deviation is measured on MS1 traces sampled ten times as densely and came
     * out at 1.6 s on the test file, which is less than a single scan point of a channel.
     */
    public static final double DEFAULT_SCAN_INTERVALS_TOLERANCE = 2d;

    @Getter @Setter
    private double scanIntervalsTolerance = DEFAULT_SCAN_INTERVALS_TOLERANCE;

    /** what the last call to {@link #refine} grouped fragments across samples with, for the report */
    private Deviation massDeviation;
    private double rtTolerance;

    public DiaRefinement() {
        this(DiaFragmentResolver.DEFAULT_SIGNIFICANCE_FLOOR, DiaFragmentResolver.DEFAULT_UNASSIGNED_WEIGHT,
                FragmentAssignment.DEFAULT_MIN_PROBABILITY, DiaDecoys.DEFAULT_MAX_DECOY_PRECURSORS);
    }

    public DiaRefinement(double significanceFloor, double unassignedWeight, double minProbability,
                         int maxDecoyPrecursors) {
        this.significanceFloor = significanceFloor;
        this.unassignedWeight = unassignedWeight;
        this.minProbability = minProbability;
        this.maxDecoyPrecursors = maxDecoyPrecursors;
    }

    @Getter
    public static class ChannelResult {
        private final DiaChannelKey key;
        private final int precursors;
        private final int fragments;
        private final int edges;
        private final int decoyEdges;
        private final int assignedFragments;
        private final int ambiguousFragments;
        private final int spectraRewritten;
        private final int spectraLeftAlone;
        /** spectra whose precursor mass does not match the feature they were mapped to, see {@code precursorMatches} */
        private final int spectraMisattributed;
        private final boolean ownNullDistribution;
        /** the combined evidence an edge had to beat, see {@link #DEFAULT_DECOY_QUANTILE} */
        private final double significanceFloor;
        /** how many fragments were seen in one sample, in two, and so on */
        private final int[] samplesPerFragment;

        ChannelResult(DiaChannelKey key, int precursors, int fragments, int edges, int decoyEdges,
                      int assignedFragments, int ambiguousFragments, int spectraRewritten, int spectraLeftAlone,
                      int spectraMisattributed, boolean ownNullDistribution, double significanceFloor,
                      int[] samplesPerFragment) {
            this.key = key;
            this.precursors = precursors;
            this.fragments = fragments;
            this.edges = edges;
            this.decoyEdges = decoyEdges;
            this.assignedFragments = assignedFragments;
            this.ambiguousFragments = ambiguousFragments;
            this.spectraRewritten = spectraRewritten;
            this.spectraLeftAlone = spectraLeftAlone;
            this.spectraMisattributed = spectraMisattributed;
            this.ownNullDistribution = ownNullDistribution;
            this.significanceFloor = significanceFloor;
            this.samplesPerFragment = samplesPerFragment;
        }

        /** @return the fragments that more than one sample measured, which are the ones with pooled evidence */
        public int fragmentsInSeveralSamples() {
            int sum = 0;
            for (int samples = 2; samples < samplesPerFragment.length; ++samples) sum += samplesPerFragment[samples];
            return sum;
        }
    }

    @Getter
    public static class Result {
        private final List<ChannelResult> channels;
        private final int alignedFeatures;
        private final int pooledDecoyEdges;

        private final String tolerances;

        Result(List<ChannelResult> channels, int alignedFeatures, int pooledDecoyEdges, String tolerances) {
            this.channels = channels;
            this.alignedFeatures = alignedFeatures;
            this.pooledDecoyEdges = pooledDecoyEdges;
            this.tolerances = tolerances;
        }

        public int spectraRewritten() {
            int sum = 0;
            for (ChannelResult c : channels) sum += c.spectraRewritten;
            return sum;
        }

        public int spectraLeftAlone() {
            int sum = 0;
            for (ChannelResult c : channels) sum += c.spectraLeftAlone;
            return sum;
        }

        public int spectraMisattributed() {
            int sum = 0;
            for (ChannelResult c : channels) sum += c.spectraMisattributed;
            return sum;
        }

        public boolean nullDistributionTooThin() {
            return pooledDecoyEdges < DiaDeconvolution.MIN_DECOY_EDGES;
        }

        public String describe() {
            final StringBuilder buf = new StringBuilder();
            buf.append(String.format(Locale.US, "DIA refinement: %d channels over %d aligned features, "
                            + "%d spectra rewritten, %d left as stage one wrote them, %d refused for a precursor "
                            + "mass that does not match their feature, fragments grouped within %s%n",
                    channels.size(), alignedFeatures, spectraRewritten(), spectraLeftAlone(), spectraMisattributed(),
                    tolerances));
            if (nullDistributionTooThin()) {
                buf.append(String.format(Locale.US, "  WARNING: only %d decoy edges in the whole experiment, which "
                        + "is too few to call anything significant%n", pooledDecoyEdges));
            }
            for (ChannelResult c : channels) {
                buf.append(String.format(Locale.US, "  channel %s: %d features, %d fragments (%d in several "
                                + "samples, %s), %d edges, %d decoy edges, floor %.1f, %d assigned (%d ambiguous), "
                                + "%d spectra rewritten%n",
                        c.key, c.precursors, c.fragments, c.fragmentsInSeveralSamples(),
                        java.util.Arrays.toString(c.samplesPerFragment), c.edges, c.decoyEdges, c.significanceFloor,
                        c.assignedFragments, c.ambiguousFragments, c.spectraRewritten));
                if (!c.ownNullDistribution) {
                    buf.append("    (too few decoys of its own, using the null distribution of the experiment)\n");
                }
            }
            return buf.toString();
        }
    }

    /**
     * @param samples       the DIA samples of the experiment, each with its acquisition scheme
     * @param features      the final features, from {@link DiaFinalPrecursors}
     * @param massDeviation how far the same mass may differ between samples
     * @param rtTolerance   how far the same apex may differ between samples, in seconds
     */
    public Result refine(List<DiaAssignmentStrategy.SampleIndex> samples, List<DiaFinalPrecursors.Aligned> features,
                         Deviation massDeviation, double rtTolerance) {
        final Map<DiaChannelKey, List<DiaAlignedFragments.SampleFragments>> channels = channelsAcrossSamples(samples);
        this.massDeviation = fragmentMassDeviation(samples, massDeviation);
        this.rtTolerance = rtTolerance;

        // the decoy graphs first: they are small, and a channel with too few decoys of its own has to be able to
        // fall back to the null distribution of the whole experiment. The real graphs are built one at a time
        // afterwards, since a single channel of a real sample holds millions of edges.
        final Map<DiaChannelKey, List<DiaAlignedFragments.Aligned>> alignedFragments = new LinkedHashMap<>();
        final Map<DiaChannelKey, DiaConsensusGraph> decoyGraphs = new LinkedHashMap<>();
        final Nulls pooled = new Nulls();
        for (Map.Entry<DiaChannelKey, List<DiaAlignedFragments.SampleFragments>> entry : channels.entrySet()) {
            final List<DiaAlignedFragments.SampleFragments> perSample = entry.getValue();
            final DiaChannel channel = perSample.get(0).channel();
            final List<DiaAlignedFragments.Aligned> fragments =
                    DiaAlignedFragments.group(perSample, this.massDeviation, fragmentRtTolerance(channel, rtTolerance));
            alignedFragments.put(entry.getKey(), fragments);
            final List<DiaFinalPrecursors.Aligned> decoys = DiaDecoys.thin(select(features, channel, true),
                    maxDecoyPrecursors, DiaFinalPrecursors.Aligned::getRetentionTime);
            final DiaConsensusGraph graph = DiaConsensusGraph.build(perSample, decoys, fragments, true);
            decoyGraphs.put(entry.getKey(), graph);
            pooled.addAll(graph);
        }
        pooled.calibrate();

        final List<ChannelResult> results = new ArrayList<>(channels.size());
        for (Map.Entry<DiaChannelKey, List<DiaAlignedFragments.SampleFragments>> entry : channels.entrySet()) {
            results.add(refineChannel(entry.getKey(), entry.getValue(), features, alignedFragments.get(entry.getKey()),
                    decoyGraphs.get(entry.getKey()), pooled));
        }
        final double reportedRt = channels.isEmpty() ? rtTolerance
                : fragmentRtTolerance(channels.values().iterator().next().get(0).channel(), rtTolerance);
        return new Result(results, features.size(), pooled.decoyCount(), String.format(Locale.US,
                "%.1f ppm / %.4f Da and %.1f s", massDeviation.getPpm(), massDeviation.getAbsolute(), reportedRt));
    }

    private ChannelResult refineChannel(DiaChannelKey key, List<DiaAlignedFragments.SampleFragments> perSample,
                                        List<DiaFinalPrecursors.Aligned> features,
                                        List<DiaAlignedFragments.Aligned> fragments, DiaConsensusGraph decoyGraph,
                                        Nulls pooled) {
        final DiaChannel channel = perSample.get(0).channel();
        final DiaConsensusGraph graph = DiaConsensusGraph.build(perSample, select(features, channel, false),
                fragments, false);

        final int decoyEdges = decoyGraph.getEdges().size();
        final boolean ownNull = decoyEdges >= DiaDeconvolution.MIN_DECOY_EDGES;
        final Nulls nulls;
        if (ownNull) {
            nulls = new Nulls();
            nulls.addAll(decoyGraph);
            nulls.calibrate();
        } else {
            nulls = pooled;
        }

        // what an edge has to beat, measured on the decoys rather than assumed
        final double floor = decoyQuantile > 0 ? floorFromDecoys(decoyGraph, nulls, decoyQuantile)
                : significanceFloor;
        final List<DiaFragmentResolver.WeightedEdge> weighted = new ArrayList<>(graph.getEdges().size());
        for (DiaConsensusGraph.ConsensusEdge edge : graph.getEdges()) {
            weighted.add(new DiaFragmentResolver.WeightedEdge(edge.precursorIndex(), edge.fragmentIndex(),
                    DiaFragmentResolver.weightFromLogPValue(nulls.logPvalue(edge), floor)));
        }
        final List<DiaFinalPrecursors.Aligned> precursors = graph.getPrecursors();
        final DiaFragmentResolver.Result resolution = DiaFragmentResolver.resolveWeighted(fragments.size(), weighted,
                intensityPrior ? index -> apexIntensityOf(precursors.get(index)) : DiaFragmentResolver.UNIFORM_CAPACITY,
                unassignedWeight, minProbability);

        final int[] written = writeBack(perSample, graph, resolution);
        return new ChannelResult(key, precursors.size(), fragments.size(), graph.getEdges().size(), decoyEdges,
                resolution.assignedFragments(), resolution.ambiguousFragments(), written[0], written[1], written[2],
                ownNull, floor, samplesPerFragment(fragments, perSample.size()));
    }

    /**
     * The combined evidence that the given fraction of decoy edges stays below. Note that the decoys serve twice
     * here, once to calibrate each signal and once to set the floor on their sum; the pseudo count in
     * {@link DiaCorrelationPValues} bounds what that can claim.
     *
     * @return the floor, or zero if there is nothing to measure it on
     */
    private static double floorFromDecoys(DiaConsensusGraph decoyGraph, Nulls nulls, double quantile) {
        // only the decoys that made a claim at all. Most decoy edges score exactly zero - no shape, no specificity,
        // no abundance worth anything - and counting them would make the floor a function of how many shapeless
        // fragments the run happens to contain rather than of how strong an accidental claim can get. Lowering the
        // trace picker's minimum from three scan points to one triples the number of such edges and dropped the
        // floor from 4.0 to 3.4 as a result, which is backwards: more junk should not make it easier to pass.
        final DoubleArrayList scores = new DoubleArrayList();
        for (DiaConsensusGraph.ConsensusEdge edge : decoyGraph.getEdges()) {
            final double score = -nulls.logPvalue(edge);
            if (score > 0) scores.add(score);
        }
        if (scores.isEmpty()) return 0d;
        scores.sort(null);
        return scores.getDouble(Math.min(scores.size() - 1, (int) (quantile * scores.size())));
    }

    /** how many fragments were seen in one sample, in two, and so on - index one is the number of singletons */
    private static int[] samplesPerFragment(List<DiaAlignedFragments.Aligned> fragments, int samples) {
        final int[] counts = new int[samples + 1];
        for (DiaAlignedFragments.Aligned fragment : fragments) {
            counts[Math.min(samples, fragment.numberOfSamples())]++;
        }
        return counts;
    }

    /**
     * How far the same fragment's mass may differ between samples.
     * <p>
     * Not the deviation the alignment expects between samples: that is measured on MS1 traces of precursors, which
     * are the strongest signals in the run, and came out at half a part per million on the test file. A fragment
     * centroid in a DIA scan is nowhere near that reproducible - the trace picker itself allows a fragment's mass to
     * wander by {@code getMs1MassDeviationWithinTraces} while following it through one sample, so two samples can
     * differ by about twice that, plus whatever systematic offset there is between them. Using the alignment's
     * number left nine out of ten fragments as singletons and the pooling that this stage exists for barely
     * happened.
     */
    private static Deviation fragmentMassDeviation(List<DiaAssignmentStrategy.SampleIndex> samples, Deviation betweenSamples) {
        double ppm = 0d, absolute = 0d;
        for (DiaAssignmentStrategy.SampleIndex sample : samples) {
            final Deviation withinTraces = sample.sample().getStorage().getStatistics()
                    .getMs1MassDeviationWithinTraces();
            ppm = Math.max(ppm, withinTraces.getPpm());
            absolute = Math.max(absolute, withinTraces.getAbsolute());
        }
        return new Deviation(2 * ppm + betweenSamples.getPpm(), 2 * absolute + betweenSamples.getAbsolute());
    }

    /** see {@link #DEFAULT_SCAN_INTERVALS_TOLERANCE} */
    private double fragmentRtTolerance(DiaChannel channel, double expectedDeviation) {
        return expectedDeviation + scanIntervalsTolerance * channel.medianScanInterval();
    }

    /** the strongest sample of a feature, which is what its ability to explain MS2 signal is measured by */
    private static double apexIntensityOf(DiaFinalPrecursors.Aligned feature) {
        double apex = 0d;
        for (DiaFinalPrecursors.Instance instance : feature.getInstances()) {
            apex = Math.max(apex, instance.getApexIntensity());
        }
        return apex;
    }

    /**
     * @param decoy true for the features that stand in for "this fragment does not belong here"
     * @return the features this channel could have fragmented, or the ones it certainly could not
     */
    private static List<DiaFinalPrecursors.Aligned> select(List<DiaFinalPrecursors.Aligned> features,
                                                           DiaChannel channel, boolean decoy) {
        final List<DiaFinalPrecursors.Aligned> selected = new ArrayList<>();
        for (DiaFinalPrecursors.Aligned feature : features) {
            if (decoy ? DiaDecoys.isEligible(channel, feature.getMz()) : channel.covers(feature.getMz())) {
                selected.add(feature);
            }
        }
        return selected;
    }

    /**
     * Replaces the peaks of the pseudo spectra with what the consensus assignment says, sample by sample. The
     * intensities are that sample's own, read over the same window the assignment was judged over, so a peak height
     * still means what it means in a measured spectrum.
     *
     * @return the number of spectra rewritten and the number left as stage one wrote them
     */
    private int[] writeBack(List<DiaAlignedFragments.SampleFragments> perSample, DiaConsensusGraph graph,
                            DiaFragmentResolver.Result resolution) {
        final List<DiaAlignedFragments.Aligned> fragments = graph.getFragments();
        // fragments per feature, exactly as the pseudo spectra of stage one are collected
        final List<IntArrayList> byPrecursor = new ArrayList<>(graph.getPrecursors().size());
        for (int p = 0; p < graph.getPrecursors().size(); ++p) byPrecursor.add(new IntArrayList());
        for (int f = 0; f < fragments.size(); ++f) {
            final FragmentAssignment assignment = resolution.getAssignments()[f];
            for (int rank = 0; rank < assignment.size(); ++rank) {
                if (assignment.probabilityAt(rank) < minProbabilityForSpectrum) continue;
                byPrecursor.get(assignment.precursorIndexAt(rank)).add(f);
            }
        }

        // which channel a pseudo spectrum came from, per sample. Its scan id is the id of an acquired scan of that
        // channel, which is the one thing that identifies the channel without comparing isolation windows.
        final List<Int2IntOpenHashMap> channelOfScan = new ArrayList<>(perSample.size());
        for (DiaAlignedFragments.SampleFragments sample : perSample) {
            channelOfScan.add(scanToChannel(sample));
        }

        // the share of each fragment that belongs to each precursor, per sample. A fragment several co-eluting
        // precursors could have produced is split between them rather than written into each at its full height,
        // so the spectra together account for the ion current that was measured rather than a multiple of it.
        final List<Long2DoubleMap> sharesPerSample = new ArrayList<>(perSample.size());
        for (int s = 0; s < perSample.size(); ++s) {
            final Long2DoubleMap shares = new Long2DoubleOpenHashMap();
            for (int f = 0; f < fragments.size(); ++f) {
                final ContiguousTrace trace = fragments.get(f).traceIn(s);
                if (trace == null) continue;
                final FragmentAssignment assignment = resolution.getAssignments()[f];
                final IntArrayList assignedTo = new IntArrayList(assignment.size());
                final List<PrecursorFeature> candidates = new ArrayList<>(assignment.size());
                for (int rank = 0; rank < assignment.size(); ++rank) {
                    if (assignment.probabilityAt(rank) < minProbabilityForSpectrum) continue;
                    final PrecursorFeature view = graph.viewOf(assignment.precursorIndexAt(rank), s);
                    if (view == null) continue;
                    assignedTo.add(assignment.precursorIndexAt(rank));
                    candidates.add(view);
                }
                if (assignedTo.isEmpty()) continue;
                final double[] split = FragmentIntensityShares.of(perSample.get(s).channel(), candidates, trace,
                        intensityShares);
                for (int k = 0; k < assignedTo.size(); ++k) {
                    if (split[k] > 0) shares.put(key(assignedTo.getInt(k), f), split[k]);
                }
            }
            sharesPerSample.add(shares);
        }

        int rewritten = 0, leftAlone = 0, misattributed = 0;
        for (int p = 0; p < graph.getPrecursors().size(); ++p) {
            final IntArrayList assigned = byPrecursor.get(p);
            for (int s = 0; s < perSample.size(); ++s) {
                final PrecursorFeature view = graph.viewOf(p, s);
                if (view == null) continue;
                final ProcessedSample sample = perSample.get(s).sample();
                final int channelId = perSample.get(s).channel().getChannelId();
                final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum(assigned.size());
                for (int fragmentIndex : assigned) {
                    final ContiguousTrace trace = fragments.get(fragmentIndex).traceIn(s);
                    if (trace == null) continue;
                    final double intensity = sharesPerSample.get(s).get(key(p, fragmentIndex));
                    if (intensity > 0) buffer.addPeak(trace.averagedMz(), intensity);
                }
                for (int uid : spectraOfChannel(graph.getPrecursors().get(p), sample, channelId, channelOfScan.get(s))) {
                    if (!precursorMatches(sample, uid, view.getMz())) {
                        ++misattributed;
                        continue;
                    }
                    if (buffer.isEmpty() && keepUnrefinedSpectra) {
                        ++leftAlone;
                        continue;
                    }
                    sample.getStorage().getSpectrumStorage().updateMs2Spectrum(uid, new SimpleSpectrum(buffer));
                    ++rewritten;
                }
            }
        }
        return new int[]{rewritten, leftAlone, misattributed};
    }

    /**
     * Whether the spectrum really belongs to the feature it was mapped to.
     * <p>
     * The mapping reproduces the rule the MS2 merge applies afterwards, and if the two ever disagree the result is
     * not a missing spectrum but a plausible spectrum on the wrong compound - the one failure that would not show up
     * as anything looking wrong. Both sides know the precursor mass independently: the header carries the mass stage
     * one measured on its MS1 trace, the feature the mass of the merged trace. They have to be the same trace.
     */
    private static boolean precursorMatches(ProcessedSample sample, int uid, double featureMz) {
        final Ms2SpectrumHeader header = sample.getStorage().getSpectrumStorage().ms2SpectrumHeader(uid);
        return header != null && PRECURSOR_IDENTITY.inErrorWindow(featureMz, header.getPrecursorMz());
    }

    /** precursor and fragment index packed into one key, so the shares of a sample fit in one map */
    private static long key(int precursorIndex, int fragmentIndex) {
        return (((long) precursorIndex) << 32) | (fragmentIndex & 0xFFFFFFFFL);
    }

    /** the pseudo spectra of one feature in one sample that belong to the given channel */
    private static IntArrayList spectraOfChannel(DiaFinalPrecursors.Aligned feature, ProcessedSample sample,
                                                 int channelId, Int2IntOpenHashMap channelOfScan) {
        final IntArrayList uids = new IntArrayList();
        for (DiaFinalPrecursors.Instance instance : feature.getInstances()) {
            if (instance.getSample() != sample) continue;
            for (int uid : instance.getPseudoSpectra()) {
                final Ms2SpectrumHeader header = sample.getStorage().getSpectrumStorage().ms2SpectrumHeader(uid);
                if (header != null && channelOfScan.get(header.getScanId()) == channelId) uids.add(uid);
            }
        }
        return uids;
    }

    /** scan id of an acquired MS2 scan to the channel that acquired it, for one sample */
    private static Int2IntOpenHashMap scanToChannel(DiaAlignedFragments.SampleFragments sample) {
        final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
        map.defaultReturnValue(-1);
        final DiaChannel channel = sample.channel();
        for (int k = 0; k < channel.numberOfScans(); ++k) {
            final Ms2SpectrumHeader header = sample.sample().getStorage().getSpectrumStorage()
                    .ms2SpectrumHeader(channel.ms2UidAt(k));
            if (header != null) map.put(header.getScanId(), channel.getChannelId());
        }
        return map;
    }

    /**
     * Groups the channels of all samples by what they measured. Channel ids are handed out per sample, so a channel
     * of one sample and the same channel of another have nothing in common but their isolation windows and collision
     * energy - which is what {@link DiaChannelKey} is. A channel that not every sample acquired is still refined,
     * over the samples that have it.
     */
    private static Map<DiaChannelKey, List<DiaAlignedFragments.SampleFragments>> channelsAcrossSamples(
            List<DiaAssignmentStrategy.SampleIndex> samples) {
        final Map<DiaChannelKey, List<DiaAlignedFragments.SampleFragments>> channels = new LinkedHashMap<>();
        for (DiaAssignmentStrategy.SampleIndex sample : samples) {
            for (DiaChannel channel : sample.index().getChannels()) {
                final List<ContiguousTrace> traces = sample.sample().getStorage().getMs2TraceStorage()
                        .forChannel(channel.getChannelId(), channel.getAxis()).all();
                channels.computeIfAbsent(channel.key(), key -> new ArrayList<>())
                        .add(new DiaAlignedFragments.SampleFragments(sample.sample(), channel, traces));
            }
        }
        return channels;
    }

    /**
     * The three null distributions of a consensus edge, calibrated against the same decoys. They are summed as
     * independent evidence, as stage one sums the two it has.
     */
    private static class Nulls {
        private final DiaCorrelationPValues shape = new DiaCorrelationPValues();
        private final DiaCorrelationPValues specificity = new DiaCorrelationPValues();
        private final DiaCorrelationPValues abundance = new DiaCorrelationPValues();

        void addAll(DiaConsensusGraph decoyGraph) {
            for (DiaConsensusGraph.ConsensusEdge edge : decoyGraph.getEdges()) {
                shape.addDecoy(edge.shape());
                specificity.addDecoy(edge.specificity(), edge.shape().datapoints());
                abundance.addDecoy(edge.abundance());
            }
        }

        void calibrate() {
            shape.calibrate();
            specificity.calibrate();
            abundance.calibrate();
        }

        int decoyCount() {
            return shape.numberOfDecoys();
        }

        double logPvalue(DiaConsensusGraph.ConsensusEdge edge) {
            return shape.logPvalue(edge.shape())
                    + specificity.logPvalue(edge.specificity(), edge.shape().datapoints())
                    + abundance.logPvalue(edge.abundance());
        }
    }
}
