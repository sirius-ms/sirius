package de.unijena.bioinf.svm;

import java.util.Arrays;

public class RankSVM {

    private static final int MAX_ITERATIONS = 10000;

    protected final double[][] kernel;
    protected final int[] pairs;
    protected double C;
    protected double learningRate;

    public RankSVM(double[][] kernel, int[] pairs, double C) {
        this.kernel = kernel;
        this.pairs = pairs;
        this.C = C;
        this.learningRate = 0.2d;
    }

    /**
     * @return the support vector coefficients for ranking
     */
    public double[] fit() {
        final int N = kernel.length;
        final int P = pairs.length>>1;
        final double[] alpha = new double[P];
        Arrays.fill(alpha, 1d/P);
        final double[] d = new double[N];
        final double[] x = new double[N];

        double l;

        // solve in java

        for (int k=0; k < MAX_ITERATIONS; ++k) {

            // first calculate AKA'*alpha
            // start with A'alpha
            Arrays.fill(d,0d);

            for (int p=0; p < pairs.length; p+=2) {
                final int pindex = p>>1;
                final int from = pairs[p];
                final int to = pairs[p+1];

                d[from] -= alpha[pindex];
                d[to] += alpha[pindex];
            }

            // and now K(A'alpha)
            for (int i=0; i < N; ++i) {
                x[i] = 0d;
                for (int j=0; j < N; ++j) {
                    x[i] += kernel[i][j]*d[j];
                }
            }

            // and now AKA'alpha

            double maxChange = 0d;

            l = learningRate / (1d + learningRate * C * (k - 1));
            for (int p=0; p < pairs.length; p+=2) {
                final int pindex = p>>1;
                final int from = pairs[p];
                final int to = pairs[p+1];

                final double dpindex = 1d - (x[to]-x[from]);
                final double newAlpha = alpha[pindex] + l*((dpindex>0 ? C : 0) - alpha[pindex]);
                maxChange = Math.max(maxChange, Math.abs(newAlpha-alpha[pindex]));
                alpha[pindex] = newAlpha;
            }

            if (maxChange < 1e-4 && k > 5) {
                break;
            }

        }

        final double[] supportVectors = new double[N];
        for (int k=0; k < pairs.length; k+=2) {
            final int pindex = k>>1;
            final int from = pairs[k];
            final int to = pairs[k+1];
            supportVectors[from] -= alpha[pindex];
            supportVectors[to] += alpha[pindex];
        }

        return supportVectors;
    }
}
