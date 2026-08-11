package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Candidate edges between the precursor features of one DIA channel and the fragments that could belong to them.
 * <p>
 * Whether an edge exists is decided structurally - the fragment was measured within the feature's window, their
 * apexes are close, and the fragment mass is plausible. How strong it is, is a separate question: the edge
 * carries the raw signals and nothing else, exactly as {@code AdductEdge} carries raw correlations and is
 * assigned a p-value later. Two consequences worth spelling out:
 * <ul>
 *     <li>An edge whose correlation is undefined is kept, not dropped. A fragment the channel caught once or
 *     twice has no elution profile, but it still has {@link DiaFragmentSpecificity}, and after alignment it may
 *     still have abundance across samples.</li>
 *     <li>Nothing here decides what a fragment finally belongs to. A fragment within reach of several precursors
 *     gets an edge to each of them; resolving that is the resolver's job.</li>
 * </ul>
 * The graph is sparse and spans a whole channel. It deliberately does not group precursors into clusters of
 * overlapping features: in a hundred Dalton wide channel a real sample has hundreds to thousands of features
 * whose windows chain into one another, so clusters of transitively overlapping features degenerate into a
 * single cluster covering the entire run.
 */
public class DiaAssignmentGraph {

    /**
     * How far the apex of a fragment may sit from the apex of the precursor feature, in scan points of the
     * channel. One scan point is already a couple of seconds, and both apexes are themselves undersampled by up
     * to half a scan point, so this is deliberately generous - the correlation, not this criterion, is what
     * judges an edge.
     */
    public static final int DEFAULT_APEX_TOLERANCE = 2;

    /**
     * How far a fragment mass may exceed the precursor mass. Fragments are lighter than their precursor, give or
     * take an isotope peak of the precursor leaking into the MS2 scan. Note that this assumes the precursor is
     * not more highly charged than its fragments, which holds for the singly charged small molecules this is
     * aimed at but not for multiply charged precursors, where a fragment can well have a higher m/z.
     */
    public static final double DEFAULT_MAX_FRAGMENT_MZ_ABOVE_PRECURSOR = 1.5;

    /**
     * A candidate edge from a precursor feature to a fragment.
     *
     * @param precursorIndex index into {@link #getPrecursors()}
     * @param fragmentIndex  index into {@link #getFragments()}
     * @param correlation    agreement of the elution profiles over the feature's window, may be undefined
     * @param specificity    fraction of the fragment's signal in the channel that falls inside the window
     */
    public record PrecursorEdge(int precursorIndex, int fragmentIndex, DiaTraceCorrelation.Result correlation,
                                double specificity) {
        public boolean hasShapeEvidence() {
            return correlation.isDefined();
        }
    }

    @Getter
    private final DiaChannel channel;

    @Getter
    private final List<PrecursorFeature> precursors;

    @Getter
    private final List<ContiguousTrace> fragments;

    @Getter
    private final List<PrecursorEdge> precursorEdges;

    /** number of precursor features that got no edge with a usable elution profile */
    @Getter
    private final int featuresWithoutShapeEvidence;

    private DiaAssignmentGraph(DiaChannel channel, List<PrecursorFeature> precursors,
                               List<ContiguousTrace> fragments, List<PrecursorEdge> precursorEdges,
                               int featuresWithoutShapeEvidence) {
        this.channel = channel;
        this.precursors = precursors;
        this.fragments = fragments;
        this.precursorEdges = precursorEdges;
        this.featuresWithoutShapeEvidence = featuresWithoutShapeEvidence;
    }

    public static DiaAssignmentGraph build(DiaChannel channel, List<PrecursorFeature> precursors,
                                           List<ContiguousTrace> fragments) {
        return build(channel, precursors, fragments, DEFAULT_APEX_TOLERANCE, DEFAULT_MAX_FRAGMENT_MZ_ABOVE_PRECURSOR);
    }

    public static DiaAssignmentGraph build(DiaChannel channel, List<PrecursorFeature> precursors,
                                           List<ContiguousTrace> fragments, int apexTolerance,
                                           double maxFragmentMzAbovePrecursor) {
        final List<PrecursorEdge> edges = new ArrayList<>();
        int withoutShape = 0;
        for (int p = 0; p < precursors.size(); ++p) {
            final PrecursorFeature precursor = precursors.get(p);
            final ChannelWindow window = precursor.getWindow();
            boolean anyShape = false;
            for (int f = 0; f < fragments.size(); ++f) {
                final ContiguousTrace fragment = fragments.get(f);
                if (!isCandidate(precursor, fragment, window, apexTolerance, maxFragmentMzAbovePrecursor)) continue;
                final DiaTraceCorrelation.Result correlation =
                        DiaTraceCorrelation.precursorWithFragment(precursor.getTrace(), fragment, channel, window);
                final double specificity = DiaFragmentSpecificity.of(fragment, window);
                edges.add(new PrecursorEdge(p, f, correlation, specificity));
                anyShape |= correlation.isDefined();
            }
            if (!anyShape) ++withoutShape;
        }
        return new DiaAssignmentGraph(channel, List.copyOf(precursors), List.copyOf(fragments),
                List.copyOf(edges), withoutShape);
    }

    private static boolean isCandidate(PrecursorFeature precursor, ContiguousTrace fragment, ChannelWindow window,
                                       int apexTolerance, double maxFragmentMzAbovePrecursor) {
        // the fragment has to have been measured inside the feature
        if (fragment.endId() < window.from() || fragment.startId() > window.to()) return false;
        if (fragment.averagedMz() > precursor.getMz() + maxFragmentMzAbovePrecursor) return false;
        // and it has to peak roughly where the feature does
        final int fragmentApex = apexWithin(fragment, window);
        if (fragmentApex < 0) return false;
        return Math.abs(fragmentApex - precursor.getApexChannelIndex()) <= apexTolerance;
    }

    /**
     * @return the scan point of the highest intensity the fragment reaches inside the window, or -1 if it was not
     * measured there at all
     */
    private static int apexWithin(ContiguousTrace fragment, ChannelWindow window) {
        int apex = -1;
        double best = 0d;
        for (int k = Math.max(window.from(), fragment.startId()), end = Math.min(window.to(), fragment.endId()); k <= end; ++k) {
            final double intensity = fragment.intensity(k);
            if (intensity > best) {
                best = intensity;
                apex = k;
            }
        }
        return apex;
    }

    /**
     * @return edges of one precursor feature
     */
    public List<PrecursorEdge> edgesOf(int precursorIndex) {
        final List<PrecursorEdge> result = new ArrayList<>();
        for (PrecursorEdge edge : precursorEdges) {
            if (edge.precursorIndex() == precursorIndex) result.add(edge);
        }
        return result;
    }

    /**
     * @return edges pointing at one fragment. More than one means the fragment is within reach of several
     * precursor features and its assignment is ambiguous.
     */
    public List<PrecursorEdge> edgesTo(int fragmentIndex) {
        final List<PrecursorEdge> result = new ArrayList<>();
        for (PrecursorEdge edge : precursorEdges) {
            if (edge.fragmentIndex() == fragmentIndex) result.add(edge);
        }
        return result;
    }
}
