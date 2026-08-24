package de.unijena.bioinf.lcms.deconvolution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Who a group of aligned precursors speaks for.
 * <p>
 * The grouping itself needs whole samples and is exercised end to end; what is worth pinning down here is the rule
 * that decides which members receive the group's spectrum, because it is the one place where a number chosen by hand
 * decides whether a compound gets an answer at all.
 */
public class DiaAlignedPrecursorGroupsTest {

    private static DiaAlignedPrecursorGroups.Group group(int[] members, int representative) {
        return new DiaAlignedPrecursorGroups.Group(members, representative, 0d);
    }

    @Test
    public void aGroupReportsEveryMemberComparableToItsDominant() {
        final double[] intensities = {100_000d, 60_000d, 10_000d};
        final DiaAlignedPrecursorGroups.Group group = group(new int[]{0, 1, 2}, 0);

        assertArrayEquals(new int[]{0, 1}, group.reportedMembers(intensities, 0.5),
                "the dominant and the member at 60 % of it");
        assertArrayEquals(new int[]{0}, group.reportedMembers(intensities, 1.0),
                "at a threshold of one only the dominant speaks");
        assertEquals(3, group.reportedMembers(intensities, 0.05).length,
                "and a low enough threshold reports everything, which is the ungrouped behaviour");
    }

    /** the representative is not always the first member, and it always comes first in the answer */
    @Test
    public void theRepresentativeIsReportedFirst() {
        final double[] intensities = {10_000d, 100_000d};
        assertArrayEquals(new int[]{1}, group(new int[]{1, 0}, 1).reportedMembers(intensities, 0.5));
    }

    @Test
    public void aGroupOfOneReportsItself() {
        assertArrayEquals(new int[]{0}, group(new int[]{0}, 0).reportedMembers(new double[]{100d}, 0.5));
    }
}
