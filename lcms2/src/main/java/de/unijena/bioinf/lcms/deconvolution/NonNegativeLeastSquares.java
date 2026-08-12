package de.unijena.bioinf.lcms.deconvolution;

/**
 * Solves {@code min ||A x - b||} subject to {@code x >= 0}, for the small dense systems the DIA deconvolution
 * produces.
 * <p>
 * The active set algorithm of Lawson and Hanson: start with everything at zero, repeatedly free the variable whose
 * gradient promises the most improvement, solve the unconstrained problem over the freed variables, and if that
 * puts any of them negative, back off along the segment towards the previous point until one hits zero and gets
 * fixed there again. It terminates, and the result is the exact constrained optimum rather than an approximation to
 * it, which matters here: the coefficients are handed out as peak intensities.
 * <p>
 * The unconstrained steps go through the normal equations {@code (AᵀA + λI) x = Aᵀb} with hand-rolled Gaussian
 * elimination, as {@link AbsorbingWalk} does. Fine for the sizes involved - a handful of scan points against a
 * handful of candidates - and the ridge term is not only there for numerical safety:
 * <p>
 * <b>The ridge decides what happens when two candidates have the same shape.</b> Two precursors that co-elute
 * identically make the system singular, and every split of the fragment between them fits equally well. Because the
 * basis is the precursors' <em>absolute</em> elution profiles, explaining a fragment through an intense precursor
 * takes a small coefficient and through a weak one a large coefficient, so the minimum norm solution - which is what
 * a ridge selects among equally good fits - attributes the fragment to the intense precursor. That is the physically
 * right answer: a weak precursor's fragments are only visible where its elution profile differs from that of the
 * intense ones, and there the fit is no longer degenerate and the shapes decide.
 */
public final class NonNegativeLeastSquares {

    /**
     * Ridge weight, relative to the largest diagonal entry of {@code AᵀA}. Small enough not to shrink a well
     * determined solution noticeably, large enough to break the tie between identical columns.
     */
    public static final double DEFAULT_RIDGE = 1e-6;

    /** a gradient has to promise at least this much, relative to the problem's scale, to free a variable */
    private static final double TOLERANCE = 1e-9;

    /** an active set algorithm cannot cycle, but a guard keeps a numerical accident from looping forever */
    private static final int MAX_ITERATIONS_PER_VARIABLE = 3;

    private NonNegativeLeastSquares() {
    }

    /**
     * @param columns the basis, as {@code columns[j][i]} = row {@code i} of column {@code j}. Column oriented
     *                because the caller has one profile per candidate, not one vector per scan point.
     * @param target  the vector to explain, one entry per row
     * @return the non-negative coefficients, one per column
     */
    public static double[] solve(double[][] columns, double[] target) {
        return solve(columns, target, DEFAULT_RIDGE);
    }

    public static double[] solve(double[][] columns, double[] target, double ridge) {
        final int n = columns.length;
        final double[] x = new double[n];
        if (n == 0) return x;
        final int m = target.length;

        // AᵀA and Aᵀb, which is all the normal equations need and is cheap to keep
        final double[][] gram = new double[n][n];
        final double[] projection = new double[n];
        double largestDiagonal = 0d;
        for (int j = 0; j < n; ++j) {
            for (int k = j; k < n; ++k) {
                double sum = 0d;
                for (int i = 0; i < m; ++i) sum += columns[j][i] * columns[k][i];
                gram[j][k] = sum;
                gram[k][j] = sum;
            }
            double sum = 0d;
            for (int i = 0; i < m; ++i) sum += columns[j][i] * target[i];
            projection[j] = sum;
            largestDiagonal = Math.max(largestDiagonal, gram[j][j]);
        }
        if (largestDiagonal <= 0d) return x;
        final double lambda = ridge * largestDiagonal;
        for (int j = 0; j < n; ++j) gram[j][j] += lambda;

        final boolean[] free = new boolean[n];
        final double[] gradient = new double[n];
        final double threshold = TOLERANCE * largestDiagonal;
        int guard = MAX_ITERATIONS_PER_VARIABLE * n + n;
        while (guard-- > 0) {
            // gradient of the residual, Aᵀ(b - A x), over the variables still held at zero
            int best = -1;
            double bestGradient = threshold;
            for (int j = 0; j < n; ++j) {
                if (free[j]) continue;
                double sum = projection[j];
                for (int k = 0; k < n; ++k) sum -= gram[j][k] * x[k];
                gradient[j] = sum;
                if (sum > bestGradient) {
                    bestGradient = sum;
                    best = j;
                }
            }
            if (best < 0) break;
            free[best] = true;

            // the unconstrained optimum over the freed variables, backed off until it is non-negative
            int inner = MAX_ITERATIONS_PER_VARIABLE * n + n;
            while (inner-- > 0) {
                final double[] candidate = solveOverFreeSet(gram, projection, free, n);
                double smallest = Double.POSITIVE_INFINITY;
                for (int j = 0; j < n; ++j) if (free[j]) smallest = Math.min(smallest, candidate[j]);
                if (smallest > 0d) {
                    System.arraycopy(candidate, 0, x, 0, n);
                    break;
                }
                // step towards the candidate only as far as the first variable reaching zero
                double alpha = 1d;
                for (int j = 0; j < n; ++j) {
                    if (!free[j] || candidate[j] > 0d) continue;
                    final double denominator = x[j] - candidate[j];
                    if (denominator > 0d) alpha = Math.min(alpha, x[j] / denominator);
                }
                for (int j = 0; j < n; ++j) {
                    if (free[j]) x[j] = Math.max(0d, x[j] + alpha * (candidate[j] - x[j]));
                }
                boolean fixedAny = false;
                for (int j = 0; j < n; ++j) {
                    if (free[j] && x[j] <= 0d) {
                        free[j] = false;
                        x[j] = 0d;
                        fixedAny = true;
                    }
                }
                // nothing could be fixed although the solution was infeasible: give up on this variable rather
                // than spin, which can only happen through rounding
                if (!fixedAny) {
                    free[best] = false;
                    x[best] = 0d;
                    break;
                }
            }
        }
        // a variable that never got freed contributes nothing, and neither does one that a near singular system
        // sent to infinity: the caller multiplies these by an intensity, where a non finite value would spread
        for (int j = 0; j < n; ++j) if (!free[j] || !Double.isFinite(x[j]) || x[j] < 0d) x[j] = 0d;
        return x;
    }

    /** the unconstrained least squares solution over the free variables, zero elsewhere */
    private static double[] solveOverFreeSet(double[][] gram, double[] projection, boolean[] free, int n) {
        int size = 0;
        final int[] index = new int[n];
        for (int j = 0; j < n; ++j) if (free[j]) index[size++] = j;
        final double[][] system = new double[size][size + 1];
        for (int r = 0; r < size; ++r) {
            for (int c = 0; c < size; ++c) system[r][c] = gram[index[r]][index[c]];
            system[r][size] = projection[index[r]];
        }
        gaussianElimination(system, size);
        final double[] result = new double[n];
        for (int r = 0; r < size; ++r) result[index[r]] = system[r][size];
        return result;
    }

    /**
     * Gaussian elimination with partial pivoting, in place; the last column holds the right hand side and receives
     * the solution. A singular row leaves its variable at zero, which the ridge should make unreachable but which is
     * cheaper to handle than to argue about.
     */
    private static void gaussianElimination(double[][] system, int n) {
        for (int column = 0; column < n; ++column) {
            int pivot = column;
            for (int row = column + 1; row < n; ++row) {
                if (Math.abs(system[row][column]) > Math.abs(system[pivot][column])) pivot = row;
            }
            final double[] swap = system[column];
            system[column] = system[pivot];
            system[pivot] = swap;
            final double head = system[column][column];
            if (head == 0d) continue;
            for (int row = 0; row < n; ++row) {
                if (row == column) continue;
                final double factor = system[row][column] / head;
                if (factor == 0d) continue;
                for (int c = column; c <= n; ++c) system[row][c] -= factor * system[column][c];
            }
        }
        for (int row = 0; row < n; ++row) {
            final double head = system[row][row];
            system[row][n] = head == 0d ? 0d : system[row][n] / head;
        }
    }
}
