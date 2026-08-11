package de.unijena.bioinf.lcms.deconvolution;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Absorbing random walk on a weighted graph, used to turn candidate edges into a probability vector.
 * <p>
 * The graph has two kinds of node. <b>Absorbing</b> nodes are the things we assign to - the precursor features of
 * one channel, plus one node standing for "belongs to none of them". <b>Transient</b> nodes are the fragments.
 * A walk starting at a fragment steps along edges with probability proportional to their weight until it is
 * absorbed, and the probability of ending at each absorbing node is that fragment's assignment vector.
 * <p>
 * Edges between two fragments are what makes the walk more than a normalisation of direct edges: a fragment with
 * weak or no evidence of its own can still be assigned through the fragments it co-elutes with. The influence of
 * an absorbing node decays multiplicatively with graph distance, so a precursor many hops away contributes
 * almost nothing - which is why a large connected component is a computational problem rather than a scientific
 * one.
 * <p>
 * Two solvers, deliberately:
 * <ul>
 *     <li>{@link #solveExact()} solves {@code (I - P_QQ) X = P_QA} directly. Exact, but it materialises a dense
 *     matrix over the transient nodes, so it is only viable for small components.</li>
 *     <li>{@link #solveIterative(int)} sweeps {@code X <- P_QQ X + P_QA}. Each sweep extends the walk by one
 *     step, so after k sweeps a row holds the probability of being absorbed <em>within k steps</em> and the
 *     missing mass is the probability of still wandering. Cheap, sparse, and converges to the exact answer.</li>
 * </ul>
 * Every transient node must have at least one edge to an absorbing node. That is what keeps the system solvable:
 * the row sums of {@code P_QQ} are then strictly below one, which makes {@code I - P_QQ} diagonally dominant.
 * In the intended use this holds because every fragment is connected to the "belongs to none of them" node.
 */
public class AbsorbingWalk {

    private final int transientCount;
    private final int absorbingCount;

    /** neighbours among the transient nodes, with weights; symmetric */
    private final IntArrayList[] transientNeighbours;
    private final DoubleArrayList[] transientWeights;

    /** neighbours among the absorbing nodes, with weights */
    private final IntArrayList[] absorbingNeighbours;
    private final DoubleArrayList[] absorbingWeights;

    /** total outgoing weight per transient node */
    private final double[] outgoing;

    private AbsorbingWalk(int transientCount, int absorbingCount, IntArrayList[] transientNeighbours,
                          DoubleArrayList[] transientWeights, IntArrayList[] absorbingNeighbours,
                          DoubleArrayList[] absorbingWeights, double[] outgoing) {
        this.transientCount = transientCount;
        this.absorbingCount = absorbingCount;
        this.transientNeighbours = transientNeighbours;
        this.transientWeights = transientWeights;
        this.absorbingNeighbours = absorbingNeighbours;
        this.absorbingWeights = absorbingWeights;
        this.outgoing = outgoing;
    }

    public int getTransientCount() {
        return transientCount;
    }

    public int getAbsorbingCount() {
        return absorbingCount;
    }

    public static Builder builder(int transientCount, int absorbingCount) {
        return new Builder(transientCount, absorbingCount);
    }

    public static class Builder {
        private final int transientCount, absorbingCount;
        private final IntArrayList[] transientNeighbours;
        private final DoubleArrayList[] transientWeights;
        private final IntArrayList[] absorbingNeighbours;
        private final DoubleArrayList[] absorbingWeights;

        private Builder(int transientCount, int absorbingCount) {
            if (transientCount < 0 || absorbingCount < 0) throw new IllegalArgumentException("negative node count");
            this.transientCount = transientCount;
            this.absorbingCount = absorbingCount;
            this.transientNeighbours = new IntArrayList[transientCount];
            this.transientWeights = new DoubleArrayList[transientCount];
            this.absorbingNeighbours = new IntArrayList[transientCount];
            this.absorbingWeights = new DoubleArrayList[transientCount];
            for (int k = 0; k < transientCount; ++k) {
                transientNeighbours[k] = new IntArrayList();
                transientWeights[k] = new DoubleArrayList();
                absorbingNeighbours[k] = new IntArrayList();
                absorbingWeights[k] = new DoubleArrayList();
            }
        }

        /**
         * An undirected edge between two fragments. Weights of zero are dropped, since a zero weight edge is the
         * same as no edge at all and would only cost time.
         */
        public Builder addTransientEdge(int left, int right, double weight) {
            if (weight <= 0) return this;
            if (left == right) throw new IllegalArgumentException("a node cannot be its own neighbour: " + left);
            transientNeighbours[left].add(right);
            transientWeights[left].add(weight);
            transientNeighbours[right].add(left);
            transientWeights[right].add(weight);
            return this;
        }

        /**
         * An edge from a fragment to something it may be assigned to.
         */
        public Builder addAbsorbingEdge(int transientNode, int absorbingNode, double weight) {
            if (weight <= 0) return this;
            absorbingNeighbours[transientNode].add(absorbingNode);
            absorbingWeights[transientNode].add(weight);
            return this;
        }

        public AbsorbingWalk build() {
            final double[] outgoing = new double[transientCount];
            for (int k = 0; k < transientCount; ++k) {
                double sum = 0d;
                for (int i = 0; i < transientWeights[k].size(); ++i) sum += transientWeights[k].getDouble(i);
                for (int i = 0; i < absorbingWeights[k].size(); ++i) sum += absorbingWeights[k].getDouble(i);
                outgoing[k] = sum;
            }
            return new AbsorbingWalk(transientCount, absorbingCount, transientNeighbours, transientWeights,
                    absorbingNeighbours, absorbingWeights, outgoing);
        }
    }

    /**
     * @return for every transient node the probability of being absorbed at each absorbing node. Rows sum to one,
     * except for a node without any edge, whose row is zero because there is nothing it could be assigned to.
     */
    public double[][] solveExact() {
        final double[][] result = new double[transientCount][absorbingCount];
        if (transientCount == 0) return result;

        // (I - P_QQ) X = P_QA, solved for all absorbing columns at once
        final double[][] system = new double[transientCount][transientCount + absorbingCount];
        for (int k = 0; k < transientCount; ++k) {
            system[k][k] = 1d;
            if (outgoing[k] <= 0) continue; // isolated: leave the row as the identity with a zero right hand side
            for (int i = 0; i < transientNeighbours[k].size(); ++i) {
                system[k][transientNeighbours[k].getInt(i)] -= transientWeights[k].getDouble(i) / outgoing[k];
            }
            for (int i = 0; i < absorbingNeighbours[k].size(); ++i) {
                system[k][transientCount + absorbingNeighbours[k].getInt(i)] +=
                        absorbingWeights[k].getDouble(i) / outgoing[k];
            }
        }
        gaussianElimination(system, transientCount, absorbingCount);
        for (int k = 0; k < transientCount; ++k) {
            System.arraycopy(system[k], transientCount, result[k], 0, absorbingCount);
        }
        return result;
    }

    /**
     * @param steps how many steps of the walk to follow. More steps means more of the graph is taken into
     *              account; the result converges to {@link #solveExact()}.
     * @return for every transient node the probability of being absorbed at each absorbing node within
     * {@code steps} steps. Rows sum to at most one, and the missing mass is the probability of still wandering.
     */
    public double[][] solveIterative(int steps) {
        if (steps < 1) throw new IllegalArgumentException("a walk needs at least one step");
        double[][] current = new double[transientCount][absorbingCount];
        double[][] next = new double[transientCount][absorbingCount];
        for (int step = 0; step < steps; ++step) {
            for (int k = 0; k < transientCount; ++k) {
                final double[] row = next[k];
                java.util.Arrays.fill(row, 0d);
                if (outgoing[k] <= 0) continue;
                for (int i = 0; i < absorbingNeighbours[k].size(); ++i) {
                    row[absorbingNeighbours[k].getInt(i)] += absorbingWeights[k].getDouble(i) / outgoing[k];
                }
                for (int i = 0; i < transientNeighbours[k].size(); ++i) {
                    final double p = transientWeights[k].getDouble(i) / outgoing[k];
                    final double[] neighbour = current[transientNeighbours[k].getInt(i)];
                    for (int a = 0; a < absorbingCount; ++a) row[a] += p * neighbour[a];
                }
            }
            final double[][] swap = current;
            current = next;
            next = swap;
        }
        return current;
    }

    /**
     * Gaussian elimination with partial pivoting, in place. The left {@code n} columns hold the matrix, the
     * remaining {@code m} columns the right hand sides; on return those columns hold the solutions.
     */
    private static void gaussianElimination(double[][] system, int n, int m) {
        for (int column = 0; column < n; ++column) {
            int pivot = column;
            for (int row = column + 1; row < n; ++row) {
                if (Math.abs(system[row][column]) > Math.abs(system[pivot][column])) pivot = row;
            }
            if (Math.abs(system[pivot][column]) < 1e-12) continue; // nothing to eliminate in this column
            final double[] swap = system[column];
            system[column] = system[pivot];
            system[pivot] = swap;

            final double diagonal = system[column][column];
            for (int j = column; j < n + m; ++j) system[column][j] /= diagonal;
            for (int row = 0; row < n; ++row) {
                if (row == column) continue;
                final double factor = system[row][column];
                if (factor == 0d) continue;
                for (int j = column; j < n + m; ++j) system[row][j] -= factor * system[column][j];
            }
        }
    }
}
