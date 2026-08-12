package de.unijena.bioinf.lcms.deconvolution;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Groups the precursor features of one channel into sets that its scans cannot tell apart.
 * <p>
 * Two compounds that co-elute inside one isolation window produce fragments in exactly the same scans, and no amount
 * of correlation against a fragment can say which of them a fragment came from - the two candidate edges are
 * identical in everything the channel measured. Naming that set explicitly is worth more than pretending to resolve
 * it: the fragments go to the member that dominates the window, exactly as a DDA acquisition assigns the whole
 * spectrum to the most intense peak of its isolation window, and how much of the window's signal the other members
 * account for is reported as {@link Group#getPollutionRatio() chimeric pollution} rather than silently dropped.
 * <p>
 * <b>Why there is no decoy calibration here.</b> Everywhere else in the deconvolution a correlation is turned into a
 * p-value against decoys, because a correlation over four to nine points means nothing on its own. That cannot be
 * done for a pair of precursors, and the reason is structural: what would make a pair a decoy is that the two are not
 * the same elution, and two precursors that do not elute together have no window in which both were measured, so
 * their correlation is not low - it is undefined, and the null distribution comes out empty. The threshold here is
 * therefore an explicit statement about resolution rather than a calibrated one, and it is a knob the comparison
 * against a DDA measurement can settle.
 * <p>
 * Correlation is measured over the <em>union</em> of the two features' windows, not their overlap: if one elutes a
 * little later than the other, that difference is exactly what the union shows and the overlap hides.
 * <p>
 * Grouping is by complete linkage, seeded from the most intense feature. A candidate joins a group only if it
 * correlates above the threshold with <em>every</em> member, which is what stops a chain of pairwise similar features
 * from growing into one group whose ends have nothing to do with each other - the same trap that made retention time
 * overlap useless as a clustering criterion. Seeding from the most intense feature also makes the representative the
 * seed by construction, and the whole procedure deterministic.
 */
public final class DiaPrecursorGroups {

    /**
     * How well two features have to correlate, at the channel's resolution, to count as indistinguishable.
     * <p>
     * Deliberately high. Being in one group means giving up on separating the members at all, so the cost of grouping
     * two features that could have been told apart is losing a compound's spectrum entirely, while the cost of
     * leaving two indistinguishable ones apart is what the pipeline already does today.
     */
    public static final double DEFAULT_MIN_CORRELATION = 0.95;

    /**
     * Only features whose apexes are within this many scan points of each other are compared at all.
     * <p>
     * Not an optimisation but the right scope: a fragment is a candidate of a feature when their apexes are within
     * {@link DiaAssignmentGraph#DEFAULT_APEX_TOLERANCE}, so two features can compete for one fragment only if their
     * own apexes are within twice that. Beyond it there is nothing to be indistinguishable about.
     */
    public static final int DEFAULT_MAX_APEX_DISTANCE = 2 * DiaAssignmentGraph.DEFAULT_APEX_TOLERANCE;

    /** one set of precursor features that the channel cannot separate */
    @Getter
    public static final class Group {
        /** indices into the feature list this was built from, the representative first */
        private final int[] members;
        /** the most intense member, which is the one the fragments are assigned to */
        private final int representative;
        /**
         * The share of the group's MS1 intensity that does <em>not</em> belong to the representative. Zero for a
         * group of one; approaching one half for two members of equal intensity, where the assignment is a coin
         * toss that the channel cannot decide.
         */
        private final double pollutionRatio;

        private Group(int[] members, int representative, double pollutionRatio) {
            this.members = members;
            this.representative = representative;
            this.pollutionRatio = pollutionRatio;
        }

        public int size() {
            return members.length;
        }

        public boolean isAmbiguous() {
            return members.length > 1;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "Group(%d members, representative %d, pollution %.2f)",
                    members.length, representative, pollutionRatio);
        }
    }

    private DiaPrecursorGroups() {
    }

    public static List<Group> of(DiaChannel channel, List<PrecursorFeature> features) {
        return of(channel, features, DEFAULT_MIN_CORRELATION, DEFAULT_MAX_APEX_DISTANCE);
    }

    /**
     * @param features      the precursor features of this channel; the groups index into this list
     * @param minCorrelation see {@link #DEFAULT_MIN_CORRELATION}
     * @param maxApexDistance see {@link #DEFAULT_MAX_APEX_DISTANCE}
     * @return one group per set of indistinguishable features, covering every feature exactly once
     */
    public static List<Group> of(DiaChannel channel, List<PrecursorFeature> features, double minCorrelation,
                                 int maxApexDistance) {
        final int n = features.size();
        final List<Group> groups = new ArrayList<>();
        if (n == 0) return groups;

        // most intense first, so that a group's seed is its representative and the result does not depend on the
        // order the features happened to arrive in
        final Integer[] byIntensity = new Integer[n];
        for (int k = 0; k < n; ++k) byIntensity[k] = k;
        java.util.Arrays.sort(byIntensity,
                Comparator.comparingDouble((Integer k) -> -features.get(k).getApexIntensity()).thenComparingInt(k -> k));

        // and by apex, so only the features that could compete for the same fragment are ever compared
        final Integer[] byApex = new Integer[n];
        for (int k = 0; k < n; ++k) byApex[k] = k;
        java.util.Arrays.sort(byApex,
                Comparator.comparingInt((Integer k) -> features.get(k).getApexChannelIndex()).thenComparingInt(k -> k));
        final int[] apexOrder = new int[n];
        for (int position = 0; position < n; ++position) apexOrder[byApex[position]] = position;

        final boolean[] taken = new boolean[n];
        final IntArrayList members = new IntArrayList();
        for (int seed : byIntensity) {
            if (taken[seed]) continue;
            taken[seed] = true;
            members.clear();
            members.add(seed);
            // walk outwards from the seed's position in apex order; everything further away cannot qualify
            final int from = apexOrder[seed];
            for (int direction = -1; direction <= 1; direction += 2) {
                for (int position = from + direction; position >= 0 && position < n; position += direction) {
                    final int candidate = byApex[position];
                    if (Math.abs(features.get(candidate).getApexChannelIndex()
                            - features.get(seed).getApexChannelIndex()) > maxApexDistance) break;
                    if (taken[candidate]) continue;
                    if (correlatesWithAll(channel, features, members, candidate, minCorrelation)) {
                        taken[candidate] = true;
                        members.add(candidate);
                    }
                }
            }
            groups.add(finish(features, members, seed));
        }
        return groups;
    }

    /** complete linkage: a candidate has to be indistinguishable from every member, not just from the seed */
    private static boolean correlatesWithAll(DiaChannel channel, List<PrecursorFeature> features,
                                             IntArrayList members, int candidate, double minCorrelation) {
        for (int member : members) {
            final DiaTraceCorrelation.Result correlation = correlate(channel, features.get(member),
                    features.get(candidate));
            if (!correlation.isDefined() || correlation.correlation() < minCorrelation) return false;
        }
        return true;
    }

    /**
     * @return how well the two features' elution profiles agree, at the channel's resolution, over the union of
     * their windows
     */
    static DiaTraceCorrelation.Result correlate(DiaChannel channel, PrecursorFeature left, PrecursorFeature right) {
        final ChannelWindow window = left.getWindow().union(right.getWindow());
        return DiaTraceCorrelation.correlateProfiles(
                Ms1TraceResampler.resample(left.getTrace(), channel.getAxis(), window.from(), window.to()),
                Ms1TraceResampler.resample(right.getTrace(), channel.getAxis(), window.from(), window.to()));
    }

    private static Group finish(List<PrecursorFeature> features, IntArrayList members, int representative) {
        double total = 0d;
        for (int member : members) total += Math.max(0d, features.get(member).getApexIntensity());
        final double mine = Math.max(0d, features.get(representative).getApexIntensity());
        final double pollution = total > 0d ? (total - mine) / total : 0d;
        // the representative first, the rest in the order they joined
        final int[] ordered = members.toIntArray();
        for (int k = 0; k < ordered.length; ++k) {
            if (ordered[k] == representative) {
                ordered[k] = ordered[0];
                ordered[0] = representative;
                break;
            }
        }
        return new Group(ordered, representative, pollution);
    }

    /** the numbers worth looking at when a channel behaves oddly */
    public static String describe(List<Group> groups) {
        int ambiguous = 0, largest = 0, members = 0;
        double pollution = 0d;
        for (Group group : groups) {
            members += group.size();
            largest = Math.max(largest, group.size());
            if (group.isAmbiguous()) {
                ++ambiguous;
                pollution += group.getPollutionRatio();
            }
        }
        return String.format(Locale.US, "%d features in %d groups, %d of them ambiguous (largest %d, mean "
                        + "pollution %.2f)", members, groups.size(), ambiguous, largest,
                ambiguous == 0 ? 0d : pollution / ambiguous);
    }
}
