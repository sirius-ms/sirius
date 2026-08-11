package de.unijena.bioinf.lcms.deconvolution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The walk that turns candidate edges into probabilities. Every expectation here is worked out by hand, so the
 * two solvers can be checked against the arithmetic and against each other.
 */
public class AbsorbingWalkTest {

    private static final double EPS = 1e-9;

    @Test
    public void aFragmentWithASingleCandidateBelongsToIt() {
        double[][] p = AbsorbingWalk.builder(1, 1).addAbsorbingEdge(0, 0, 2.5).build().solveExact();
        assertEquals(1d, p[0][0], EPS);
    }

    @Test
    public void directEdgesAreSplitInProportionToTheirWeight() {
        double[][] p = AbsorbingWalk.builder(1, 2)
                .addAbsorbingEdge(0, 0, 3d)
                .addAbsorbingEdge(0, 1, 1d)
                .build().solveExact();
        assertEquals(0.75, p[0][0], EPS);
        assertEquals(0.25, p[0][1], EPS);
    }

    /**
     * The point of the "belongs to none of them" node: a fragment whose evidence for a precursor is no better
     * than its evidence for nothing keeps half of its mass unassigned.
     */
    @Test
    public void theUnassignedNodeCompetesWithTheCandidates() {
        final int unassigned = 1;
        double[][] p = AbsorbingWalk.builder(1, 2)
                .addAbsorbingEdge(0, 0, 1d)
                .addAbsorbingEdge(0, unassigned, 1d)
                .build().solveExact();
        assertEquals(0.5, p[0][0], EPS);
        assertEquals(0.5, p[0][unassigned], EPS);
    }

    /**
     * A fragment with no evidence of its own, assigned through the fragment it co-elutes with. Worked out by
     * hand: with A-B of weight 1, A to P of weight 3 and both fragments tied to the unassigned node with 0.1,
     * A reaches P with 0.94018 and B, which has no edge to P at all, with 0.85471.
     */
    @Test
    public void aFragmentWithoutOwnEvidenceIsAssignedThroughItsNeighbour() {
        final int a = 0, b = 1, precursor = 0, unassigned = 1;
        double[][] p = AbsorbingWalk.builder(2, 2)
                .addTransientEdge(a, b, 1d)
                .addAbsorbingEdge(a, precursor, 3d)
                .addAbsorbingEdge(a, unassigned, 0.1)
                .addAbsorbingEdge(b, unassigned, 0.1)
                .build().solveExact();

        assertEquals(0.94018, p[a][precursor], 1e-5);
        assertEquals(0.85471, p[b][precursor], 1e-5);
        assertEquals(1d, p[a][precursor] + p[a][unassigned], EPS);
        assertEquals(1d, p[b][precursor] + p[b][unassigned], EPS);
    }

    /** Every row is a probability distribution over the absorbing nodes. */
    @Test
    public void rowsOfTheExactSolutionSumToOne() {
        double[][] p = AbsorbingWalk.builder(4, 3)
                .addTransientEdge(0, 1, 2d).addTransientEdge(1, 2, 0.5).addTransientEdge(2, 3, 1.5)
                .addAbsorbingEdge(0, 0, 1d).addAbsorbingEdge(1, 1, 0.3).addAbsorbingEdge(3, 2, 4d)
                .addAbsorbingEdge(0, 2, 0.1).addAbsorbingEdge(1, 2, 0.1)
                .addAbsorbingEdge(2, 2, 0.1).addAbsorbingEdge(3, 2, 0.1)
                .build().solveExact();
        for (int k = 0; k < 4; ++k) {
            double sum = 0d;
            for (double v : p[k]) {
                assertTrue(v >= -EPS, "probabilities cannot be negative");
                sum += v;
            }
            assertEquals(1d, sum, 1e-9, "row " + k);
        }
    }

    @Test
    public void aFragmentWithoutAnyEdgeIsAssignedNowhere() {
        double[][] p = AbsorbingWalk.builder(2, 2)
                .addAbsorbingEdge(0, 0, 1d)
                .build().solveExact();
        assertEquals(1d, p[0][0], EPS);
        assertEquals(0d, p[1][0], EPS);
        assertEquals(0d, p[1][1], EPS);
    }

    /**
     * Substantiates why a large component is a computational rather than a scientific problem: influence decays
     * with graph distance, so a precursor many hops away barely matters.
     */
    @Test
    public void influenceDecaysWithGraphDistance() {
        final int n = 6, precursor = 0, unassigned = 1;
        AbsorbingWalk.Builder builder = AbsorbingWalk.builder(n, 2);
        for (int k = 0; k + 1 < n; ++k) builder.addTransientEdge(k, k + 1, 1d);
        builder.addAbsorbingEdge(0, precursor, 3d);
        for (int k = 0; k < n; ++k) builder.addAbsorbingEdge(k, unassigned, 0.5);
        double[][] p = builder.build().solveExact();

        for (int k = 0; k + 1 < n; ++k) {
            assertTrue(p[k][precursor] > p[k + 1][precursor],
                    "probability must fall with distance, at " + k + ": " + p[k][precursor] + " vs " + p[k + 1][precursor]);
        }
        assertTrue(p[n - 1][precursor] < 0.1 * p[0][precursor],
                "the far end of the chain must be almost unaffected: " + p[n - 1][precursor] + " vs " + p[0][precursor]);
    }

    @Test
    public void theIterativeSolverConvergesToTheExactOne() {
        AbsorbingWalk walk = AbsorbingWalk.builder(4, 3)
                .addTransientEdge(0, 1, 2d).addTransientEdge(1, 2, 0.5).addTransientEdge(2, 3, 1.5)
                .addAbsorbingEdge(0, 0, 1d).addAbsorbingEdge(1, 1, 0.3).addAbsorbingEdge(3, 2, 4d)
                .addAbsorbingEdge(0, 2, 0.1).addAbsorbingEdge(1, 2, 0.1)
                .addAbsorbingEdge(2, 2, 0.1).addAbsorbingEdge(3, 2, 0.1)
                .build();

        double[][] exact = walk.solveExact();
        double[][] few = walk.solveIterative(3);
        double[][] many = walk.solveIterative(200);

        double errorFew = maxDifference(exact, few);
        double errorMany = maxDifference(exact, many);
        assertTrue(errorMany < 1e-9, "200 steps should be indistinguishable from exact, off by " + errorMany);
        assertTrue(errorFew > errorMany, "three steps should be measurably worse");
    }

    /** After k steps a row holds the probability of being absorbed within k steps, so it can only grow. */
    @Test
    public void truncatingTheWalkLeavesMassUnaccountedFor() {
        AbsorbingWalk walk = AbsorbingWalk.builder(3, 2)
                .addTransientEdge(0, 1, 5d).addTransientEdge(1, 2, 5d)
                .addAbsorbingEdge(0, 0, 1d)
                .addAbsorbingEdge(0, 1, 0.01).addAbsorbingEdge(1, 1, 0.01).addAbsorbingEdge(2, 1, 0.01)
                .build();

        double previous = -1d;
        for (int steps : new int[]{1, 2, 5, 20}) {
            double[][] p = walk.solveIterative(steps);
            double sum = p[2][0] + p[2][1];
            assertTrue(sum <= 1d + EPS, "a truncated walk cannot absorb more than everything");
            assertTrue(sum > previous, "more steps must absorb more mass, at " + steps);
            previous = sum;
        }
    }

    @Test
    public void zeroWeightEdgesAreIgnored() {
        double[][] p = AbsorbingWalk.builder(1, 2)
                .addAbsorbingEdge(0, 0, 1d)
                .addAbsorbingEdge(0, 1, 0d)
                .build().solveExact();
        assertEquals(1d, p[0][0], EPS);
        assertEquals(0d, p[0][1], EPS);
    }

    @Test
    public void degenerateInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> AbsorbingWalk.builder(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> AbsorbingWalk.builder(2, 1).addTransientEdge(0, 0, 1d));
        assertThrows(IllegalArgumentException.class, () -> AbsorbingWalk.builder(1, 1).build().solveIterative(0));
    }

    @Test
    public void anEmptyGraphIsHandled() {
        assertEquals(0, AbsorbingWalk.builder(0, 2).build().solveExact().length);
        assertEquals(0, AbsorbingWalk.builder(0, 2).build().solveIterative(5).length);
    }

    private static double maxDifference(double[][] a, double[][] b) {
        double max = 0d;
        for (int k = 0; k < a.length; ++k) {
            for (int i = 0; i < a[k].length; ++i) max = Math.max(max, Math.abs(a[k][i] - b[k][i]));
        }
        return max;
    }
}
