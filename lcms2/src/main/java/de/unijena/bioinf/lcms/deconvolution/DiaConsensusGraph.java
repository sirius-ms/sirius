package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * The candidate edges of one DIA channel, over features and fragments aligned across samples.
 * <p>
 * The counterpart of {@link DiaAssignmentGraph} for stage two, and deliberately built to the same rules: an edge
 * exists on structural grounds - the fragment was measured within the feature, their apexes are close and the mass
 * is plausible, all decided by {@link DiaAssignmentGraph#isCandidate} itself so the two stages cannot drift apart -
 * and carries raw signals that a calibration turns into weights later.
 * <p>
 * What differs is what a node is and therefore how much evidence an edge carries. A precursor is one feature across
 * the samples that measured it, bounded by the final feature boundaries rather than by a preliminary segment. A
 * fragment is one mass across the samples that measured it. So an edge is one decision informed by every sample,
 * rather than one decision per sample:
 * <ul>
 *     <li>the shape correlation is pooled over the samples' windows, which is the only way past the four to nine
 *     points a single channel measures a peak with,</li>
 *     <li>the specificity is averaged over them, and</li>
 *     <li>the abundance across samples becomes available at all, being the one signal that is not co-elution.</li>
 * </ul>
 * A precursor that a sample did not measure, or measured too narrowly for its window to hold three of the channel's
 * scan points, simply does not contribute; the edge is then carried by the samples that did.
 */
public class DiaConsensusGraph {

    /**
     * A candidate edge, with the three signals kept apart because they are calibrated separately and summed as
     * independent evidence, exactly as stage one sums correlation and specificity.
     *
     * @param shape       pooled agreement of the elution profiles, may be undefined
     * @param specificity mean fraction of the fragment's signal in the channel that falls inside the window
     * @param abundance   agreement of the abundances across samples, may be undefined
     * @param samples     number of samples in which both were measured
     */
    public record ConsensusEdge(int precursorIndex, int fragmentIndex, DiaTraceCorrelation.Result shape,
                                double specificity, DiaTraceCorrelation.Result abundance, int samples) {
        public boolean hasShapeEvidence() {
            return shape.isDefined();
        }
    }

    @Getter
    private final List<DiaFinalPrecursors.Aligned> precursors;

    @Getter
    private final List<DiaAlignedFragments.Aligned> fragments;

    @Getter
    private final List<ConsensusEdge> edges;

    /** number of features that got no edge with a usable elution profile */
    @Getter
    private final int featuresWithoutShapeEvidence;

    /** the feature as seen from this channel, per precursor and sample, null where that sample has no window */
    private final PrecursorFeature[][] views;

    private DiaConsensusGraph(List<DiaFinalPrecursors.Aligned> precursors,
                              List<DiaAlignedFragments.Aligned> fragments, List<ConsensusEdge> edges,
                              int featuresWithoutShapeEvidence, PrecursorFeature[][] views) {
        this.precursors = precursors;
        this.fragments = fragments;
        this.edges = edges;
        this.featuresWithoutShapeEvidence = featuresWithoutShapeEvidence;
        this.views = views;
    }

    /**
     * @return the window this feature was measured over in the given sample, or null if that sample did not
     * contribute to it. The peaks written back have to be read over the same window they were judged over, so this
     * is deliberately the graph's own view rather than something recomputed.
     */
    public PrecursorFeature viewOf(int precursorIndex, int sampleIndex) {
        final PrecursorFeature[] perSample = views[precursorIndex];
        return perSample == null ? null : perSample[sampleIndex];
    }

    public static DiaConsensusGraph build(List<DiaAlignedFragments.SampleFragments> perSample,
                                          List<DiaFinalPrecursors.Aligned> precursors,
                                          List<DiaAlignedFragments.Aligned> fragments, boolean decoy) {
        return build(perSample, precursors, fragments, decoy, DiaAssignmentGraph.DEFAULT_APEX_TOLERANCE,
                DiaAssignmentGraph.DEFAULT_MAX_FRAGMENT_MZ_ABOVE_PRECURSOR);
    }

    /**
     * @param perSample  the same channel in every sample, in the sample order the aligned fragments were grouped in
     * @param decoy      true to pair precursors with a channel that cannot have fragmented them, which is how the
     *                   null distributions are built; the m/z coverage check is then skipped, everything else stays
     */
    public static DiaConsensusGraph build(List<DiaAlignedFragments.SampleFragments> perSample,
                                          List<DiaFinalPrecursors.Aligned> precursors,
                                          List<DiaAlignedFragments.Aligned> fragments, boolean decoy,
                                          int apexTolerance, double maxFragmentMzAbovePrecursor) {
        final Int2IntOpenHashMap sampleIndex = new Int2IntOpenHashMap();
        sampleIndex.defaultReturnValue(-1);
        for (int s = 0; s < perSample.size(); ++s) sampleIndex.put(perSample.get(s).sample().getUid(), s);

        final List<ConsensusEdge> edges = new ArrayList<>();
        final PrecursorFeature[][] allViews = new PrecursorFeature[precursors.size()][];
        int withoutShape = 0;
        for (int p = 0; p < precursors.size(); ++p) {
            final PrecursorFeature[] views = viewsOf(precursors.get(p), perSample, sampleIndex, decoy);
            if (views == null) continue;
            allViews[p] = views;
            boolean anyShape = false;
            for (int f = 0; f < fragments.size(); ++f) {
                final ConsensusEdge edge = edgeOf(p, f, fragments.get(f), views, perSample, apexTolerance,
                        maxFragmentMzAbovePrecursor);
                if (edge == null) continue;
                edges.add(edge);
                anyShape |= edge.hasShapeEvidence();
            }
            if (!anyShape) ++withoutShape;
        }
        return new DiaConsensusGraph(List.copyOf(precursors), List.copyOf(fragments), List.copyOf(edges),
                withoutShape, allViews);
    }

    /**
     * @return the feature as seen from this channel in every sample, null where that sample did not measure it or
     * measured it too narrowly, or null altogether if no sample did
     */
    /**
     * The per sample views of a whole precursor list, as {@link #build} computes them internally.
     * <p>
     * Exposed because a caller that wants to decide something about the precursors themselves - which of them are
     * indistinguishable, for instance - needs their elution profiles before the graph exists, and recomputing them
     * by hand would be a second definition of what a sample's view of a final feature is.
     *
     * @return one array per precursor, null where no sample measured it, and null entries for the samples that did not
     */
    public static PrecursorFeature[][] viewsOf(List<DiaFinalPrecursors.Aligned> precursors,
                                               List<DiaAlignedFragments.SampleFragments> perSample, boolean decoy) {
        final Int2IntOpenHashMap sampleIndex = new Int2IntOpenHashMap();
        sampleIndex.defaultReturnValue(-1);
        for (int s = 0; s < perSample.size(); ++s) sampleIndex.put(perSample.get(s).sample().getUid(), s);
        final PrecursorFeature[][] views = new PrecursorFeature[precursors.size()][];
        for (int p = 0; p < precursors.size(); ++p) {
            views[p] = viewsOf(precursors.get(p), perSample, sampleIndex, decoy);
        }
        return views;
    }

    private static PrecursorFeature[] viewsOf(DiaFinalPrecursors.Aligned precursor,
                                              List<DiaAlignedFragments.SampleFragments> perSample,
                                              Int2IntOpenHashMap sampleIndex, boolean decoy) {
        final PrecursorFeature[] views = new PrecursorFeature[perSample.size()];
        boolean any = false;
        for (DiaFinalPrecursors.Instance instance : precursor.getInstances()) {
            final int s = sampleIndex.get(instance.getSample().getUid());
            if (s < 0) continue;
            final DiaChannel channel = perSample.get(s).channel();
            final var view = decoy
                    ? PrecursorFeature.decoy(channel, instance.getRawTrace(), -1, instance.getRawWindow())
                    : PrecursorFeature.of(channel, instance.getRawTrace(), -1, instance.getRawWindow());
            if (view.isPresent()) {
                views[s] = view.get();
                any = true;
            }
        }
        return any ? views : null;
    }

    /** @return the edge, or null if this pair is not a candidate in any sample */
    private static ConsensusEdge edgeOf(int precursorIndex, int fragmentIndex,
                                        DiaAlignedFragments.Aligned fragment, PrecursorFeature[] views,
                                        List<DiaAlignedFragments.SampleFragments> perSample, int apexTolerance,
                                        double maxFragmentMzAbovePrecursor) {
        final List<DiaTraceCorrelation.SampleWindow> windows = new ArrayList<>(views.length);
        // abundances over the samples that measured the feature, so that a sample which did not contribute to it
        // does not enter as a zero: an aligned feature without an instance in a sample says nothing about how much
        // of the compound was in it
        final DoubleArrayList precursorAbundance = new DoubleArrayList(views.length);
        final DoubleArrayList fragmentAbundance = new DoubleArrayList(views.length);
        double specificity = 0d;
        int samples = 0;
        boolean candidate = false;
        for (int s = 0; s < views.length; ++s) {
            final PrecursorFeature view = views[s];
            if (view == null) continue;
            final ContiguousTrace trace = fragment.traceIn(s);
            precursorAbundance.add(view.getApexIntensity());
            // absent means the fragment was below this sample's noise level while the precursor was measured, which
            // speaks against the assignment and is therefore a zero rather than a gap
            fragmentAbundance.add(trace == null ? 0d : view.getWindow().apexIntensityIn(trace));
            if (trace == null) continue;
            ++samples;
            if (DiaAssignmentGraph.isCandidate(view, trace, view.getWindow(), apexTolerance,
                    maxFragmentMzAbovePrecursor)) {
                candidate = true;
            }
            windows.add(new DiaTraceCorrelation.SampleWindow(view.getTrace(), trace, perSample.get(s).channel(),
                    view.getWindow()));
            specificity += DiaFragmentSpecificity.of(trace, view.getWindow());
        }
        // a pair that no sample considers a candidate is not one: the structural criteria are the same as stage one's
        if (!candidate) return null;
        return new ConsensusEdge(precursorIndex, fragmentIndex, DiaTraceCorrelation.pooled(windows),
                samples == 0 ? 0d : specificity / samples,
                DiaAbundanceCorrelation.of(precursorAbundance.toDoubleArray(), fragmentAbundance.toDoubleArray()),
                samples);
    }

    /** @return edges pointing at one fragment; more than one means its assignment is ambiguous */
    public List<ConsensusEdge> edgesTo(int fragmentIndex) {
        final List<ConsensusEdge> result = new ArrayList<>();
        for (ConsensusEdge edge : edges) {
            if (edge.fragmentIndex() == fragmentIndex) result.add(edge);
        }
        return result;
    }
}
