/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms.trace.filter;

/**
 * Gaussian convolution for one dimensional arrays.
 */
public class GaussFilter implements Filter {

    private final double[] kernel;

    public GaussFilter(double sigma) {
        if (sigma <= 0)
            throw new IllegalArgumentException("sigma must be > 0! (was " + sigma + ")");
        this.kernel = computeKernel(sigma);
    }

    private double[] computeKernel(double sigma) {
        int radius = (int) Math.round(4.0 * sigma);
        double sigma2 = -0.5 / (sigma * sigma);
        double sum = 0;
        double[] k = new double[2 * radius + 1];
        for (int i = -radius; i < radius + 1; i++) {
            k[i + radius] = Math.exp(sigma2 * Math.pow(i, 2));
            sum += k[i + radius];
        }
        for (int i = 0; i < k.length; i++) {
            k[i] /= sum;
        }
        return k;
    }

    public double[] apply(double[] src) {
        int kw = (kernel.length  - 1) / 2;

        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++) {
            dst[i] = 0;
            for (int di = -kw; di <= kw; di++) {
                if (i + di < 0)
                    dst[i] += kernel[di + kw] * src[0];
                else if (i + di >= src.length)
                    dst[i] += kernel[di + kw] * src[src.length - 1];
                else
                    dst[i] += kernel[di + kw] * src[i + di];
            }
        }

        return dst;
    }

}
