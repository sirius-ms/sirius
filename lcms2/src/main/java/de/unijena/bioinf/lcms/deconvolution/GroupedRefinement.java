package de.unijena.bioinf.lcms.deconvolution;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage two with the precursors of a channel first grouped into the sets no sample can separate.
 * <p>
 * The same change as {@link DiaPrecursorGroups} makes to stage one, made where it decides the final spectra. Both are
 * needed or neither: this stage rebuilds the assignment over the final feature boundaries and rewrites every spectrum
 * stage one wrote, so grouping applied only to stage one survives in which spectra exist and in nothing else. Its
 * peaks would still come from an ungrouped competition between precursors that were called inseparable.
 * <p>
 * Nothing else about the refinement changes. The graph, the calibration against decoys, the resolver and the write
 * back are the base class's, and a group appears to them as one precursor - which is the point: the reduction is a
 * claim about what the data can distinguish, not a different way of scoring what it can.
 *
 * @see DiaAlignedPrecursorGroups
 * @see GroupedAssignmentStrategy
 */
public class GroupedRefinement extends DiaRefinement {

    /**
     * How intense a member of a group has to be, relative to its most intense member, to receive the group's spectrum
     * as well.
     * <p>
     * At one, only the representative is reported, which is what a data dependent acquisition does with an isolation
     * window. Below it, a member the channel cannot separate from the dominant one and which carries a comparable
     * share of the signal is reported with the same peaks rather than silently dropped in the dominant one's favour -
     * the choice between them would be a coin toss, and withholding the spectrum makes that choice invisible.
     */
    @Getter @Setter
    private double minReportedShareOfGroup = DiaPrecursorGroups.DEFAULT_MIN_REPORTED_SHARE;

    /** which quantile of known same elutions the grouping cutoff is read at, see {@link DiaPrecursorGroups} */
    @Getter @Setter
    private double quantile = DiaPrecursorGroups.DEFAULT_QUANTILE;

    /**
     * How far apart two apexes may be and still be compared at all, in seconds.
     * <p>
     * Stage one measures this in channel scan points, which it can because every feature of a channel lives on that
     * channel's axis. Here a precursor is one feature across samples and its retention time is the aligned one, so
     * the bound has to be in seconds. A channel samples a peak every two seconds or so, which is what makes the two
     * comparable.
     */
    private static final double DEFAULT_SECONDS_PER_APEX_STEP = 2.5d;

    /** how far apart two apexes may be and still be compared at all, in seconds */
    @Getter @Setter
    private double maxApexDistance = DiaAssignmentGraph.DEFAULT_APEX_TOLERANCE * DEFAULT_SECONDS_PER_APEX_STEP;

    @Getter
    private int groupsFormed, precursorsAbsorbed, membersCoReported;

    @Override
    protected Nodes precursorNodesOf(List<DiaFinalPrecursors.Aligned> precursors, PrecursorFeature[][] views,
                                     List<DiaAlignedFragments.SampleFragments> samples, double[] intensities) {
        final List<DiaAlignedPrecursorGroups.Group> groups = DiaAlignedPrecursorGroups.of(precursors, views, samples,
                intensities, maxApexDistance, quantile);
        final List<DiaFinalPrecursors.Aligned> representatives = new ArrayList<>(groups.size());
        final List<List<Member>> coReported = new ArrayList<>(groups.size());
        for (DiaAlignedPrecursorGroups.Group group : groups) {
            representatives.add(precursors.get(group.getRepresentative()));
            final int[] reported = group.reportedMembers(intensities, minReportedShareOfGroup);
            final List<Member> others = new ArrayList<>(reported.length - 1);
            for (int member : reported) {
                if (member == group.getRepresentative()) continue;
                others.add(new Member(precursors.get(member), views[member]));
            }
            coReported.add(others);
            ++groupsFormed;
            precursorsAbsorbed += group.getMembers().length - 1;
            membersCoReported += others.size();
        }
        return new Nodes(representatives, coReported);
    }

    @Override
    public String describeStrategy() {
        return String.format(java.util.Locale.US, "  precursors grouped into %d groups, absorbing %d of them, of "
                        + "which %d are reported alongside their group's representative%n",
                groupsFormed, precursorsAbsorbed, membersCoReported);
    }
}
