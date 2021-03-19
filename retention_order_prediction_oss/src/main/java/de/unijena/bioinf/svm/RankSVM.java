/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *  
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker, 
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.svm;

import java.util.Arrays;

public class RankSVM {

    private static final int MAX_ITERATIONS = 4000;

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
        int k;
        for (k=0; k < MAX_ITERATIONS; ++k) {

            // first calculate AKA'*alpha
            // start with A'alpha)
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
        System.out.println("Steps = " + k);
        final double[] supportVectors = new double[N];
        for (int i=0; i < pairs.length; i+=2) {
            final int pindex = i>>1;
            final int from = pairs[i];
            final int to = pairs[i+1];
            supportVectors[from] -= alpha[pindex];
            supportVectors[to] += alpha[pindex];
        }

        return supportVectors;
    }
}
