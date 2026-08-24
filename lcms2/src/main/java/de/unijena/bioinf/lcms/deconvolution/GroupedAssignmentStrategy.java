package de.unijena.bioinf.lcms.deconvolution;

/**
 * The assignment with the precursors of a channel first grouped into the sets it cannot separate, and only one feature
 * of each competing for the fragments.
 * <p>
 * The alternative to {@link CorrelationAssignmentStrategy}, and the difference is what it refuses to do rather than
 * what it adds. Two compounds co-eluting inside one isolation window produce fragments in the same scans, so their
 * candidate edges are identical in everything the channel measured and no calibration of a fragment correlation can
 * separate them. That strategy tries anyway and the tie is broken by an intensity prior and a ridge, neither of which
 * records that the choice was arbitrary. This one names the set, hands the fragments to the member that dominates the
 * window, and reports what the rest account for - which is also what a data dependent acquisition does, by selecting
 * the most intense peak of a window and giving the others nothing.
 * <p>
 * Everything downstream is untouched, because a group is represented by one of its own features: the same candidate
 * graph, the same decoy calibration, the same resolver and the same intensity shares apply, over a smaller set of
 * precursor nodes. The threshold that decides what "cannot separate" means is read per pair from
 * {@link de.unijena.bioinf.lcms.statistics.TraceCorrelationStatistics}, which measures how well a genuine co-elution
 * correlates on this very sample.
 * <p>
 * Dropping precursors too weak for that question to have an answer is a second, independent reduction and is
 * <em>off</em> here. Measured against a DDA run of the same sample it costs about two hundred compounds and buys
 * nothing: with it on the strategy matches 1298 of the reference's compounds at 25.5 % confirmed and 5.5 % recall,
 * with it off 1497 at 25.7 % and 6.5 %, both corrected for chance. It remains available on the deconvolution for
 * anyone who wants to measure it again on other data.
 *
 * @see DiaPrecursorGroups
 * @see GroupedRefinement
 */
public class GroupedAssignmentStrategy extends CorrelationAssignmentStrategy {

    public GroupedAssignmentStrategy() {
        this(new DiaDeconvolution(), new GroupedRefinement());
    }

    /**
     * @param refinement has to group as well, or the grouping survives only in which spectra exist: stage two rebuilds
     *                   the assignment over the final features and rewrites every spectrum stage one wrote
     */
    public GroupedAssignmentStrategy(DiaDeconvolution deconvolution, GroupedRefinement refinement) {
        super(deconvolution, refinement);
        deconvolution.setGroupIndistinguishablePrecursors(true);
    }

    /** the share both stages use, so a group speaks for the same members before and after merging */
    public void setMinReportedShareOfGroup(double share) {
        getDeconvolution().setMinReportedShareOfGroup(share);
        ((GroupedRefinement) getRefinement()).setMinReportedShareOfGroup(share);
    }
}
