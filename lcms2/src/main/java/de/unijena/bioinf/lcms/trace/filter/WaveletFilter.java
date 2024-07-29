/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package de.unijena.bioinf.lcms.trace.filter;

import java.util.Arrays;

public class WaveletFilter implements Filter {

    /**
     * number of wavelet points
     */
    private static final double NPOINTS = 60000;
    /**
     * Left effective support boundary
     */
    private static final int WAVELET_ESL = -5;
    /**
     * Right effective support boundary
     */
    private static final int WAVELET_ESR = 5;

    /**
     * Scale level. The higher the scale level, the broader peaks will appear in the resulting convolution.
     */
    private final int scaleLevel;

    /**
     * The wavelet
     */
    private final double[] W;


    public WaveletFilter(int scaleLevel) {
        this.scaleLevel = scaleLevel;

        double wstep = ((WAVELET_ESR - WAVELET_ESL) / NPOINTS);
        W = new double[(int) NPOINTS];
        double waveletIndex = WAVELET_ESL;
        for (int j = 0; j < NPOINTS; j++) {
            // Pre calculate the values of the wavelet
            W[j] = cwtMEXHATreal(waveletIndex);
            waveletIndex += wstep;
        }
    }

    /**
     * Perform the CWT over raw data points
     */
    @Override
    public double[] apply(double[] src) {
        if (src.length == 0) {
            return new double[0];
        }

        double norm = Arrays.stream(src).max().orElseThrow();
        if (norm == 0d) {
            return new double[src.length];
        }
        norm /= 1d;

        double[] cwtDataPoints = new double[src.length];

        /*
         * We only perform Translation of the wavelet in the selected scale
         */
        int d = (int) NPOINTS / (WAVELET_ESR - WAVELET_ESL);
        int a_esl = scaleLevel * WAVELET_ESL;
        int a_esr = scaleLevel * WAVELET_ESR;
        double sqrtScaleLevel = Math.sqrt(scaleLevel);
        for (int dx = 0; dx < src.length; dx++) {

            /* Compute wavelet boundaries */
            int t1 = a_esl + dx;
            if (t1 < 0) {
                t1 = 0;
            }
            int t2 = a_esr + dx;
            if (t2 >= src.length) {
                t2 = (src.length - 1);
            }

            /* Perform convolution */
            double intensity = 0.0;
            for (int i = t1; i <= t2; i++) {
                int ind = (int) (NPOINTS / 2) - ((d * (i - dx) / scaleLevel) * (-1));
                if (ind < 0) {
                    ind = 0;
                }
                if (ind >= NPOINTS) {
                    ind = (int) NPOINTS - 1;
                }
                intensity += norm * src[i] * W[ind];
            }
            intensity /= sqrtScaleLevel;
            // Eliminate the negative part of the wavelet map
            if (intensity < 0) {
                intensity = 0;
            }
            cwtDataPoints[dx] = intensity;
        }

        return cwtDataPoints;
    }

    /**
     * This function calculates the wavelets's coefficients in Time domain
     *
     * @param x Step of the wavelett
     */
    private static double cwtMEXHATreal(double x) {
        /* c = 2 / ( sqrt(3) * pi^(1/4) ) */
        double c = 0.8673250705840776;
        double x2 = x * x;
        return c * (1.0 - x2) * Math.exp(-x2 / 2);
    }

}
