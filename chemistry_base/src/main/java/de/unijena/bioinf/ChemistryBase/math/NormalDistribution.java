
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

package de.unijena.bioinf.ChemistryBase.math;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;

@HasParameters
public final class NormalDistribution extends RealDistribution {
    private final static double sqrt2pi = sqrt(2*PI);
    private final double mean, var;

    public NormalDistribution(@Parameter("mean") double mean, @Parameter("variance") double var) {
        this.mean = mean;
        this.var = var;
    }

    @Override
    public double getDensity(double x) {
        return MathUtils.pdf(x, mean, var);
    }

    @Override
    public double getProbability(double begin, double end) {
        return MathUtils.cdf(begin, end, mean, var);
    }

    /**
     * equal to 1-getProbability(mu-x, mu+x)
     * Computes the probability to observe a value deviating by x from the mean of the normal distribution
     * @param x
     * @return
     */
    public double getErrorProbability(double x) {
        return MathUtils.erfc(Math.abs(mean-x)/(Math.sqrt(2*var)));
    }

    @Override
    public double getCumulativeProbability(double x) {
        return MathUtils.cdf(x, mean, var);
    }

    @Override
    public double getLogDensity(double x) {
        return -(x - mean) * (x - mean) / (2. * var) - sqrt2pi*sqrt(var);
    }

    public double getStandardDeviation() {
        return sqrt(var);
    }

    @Override
    public double getVariance() {
        return var;
    }

    @Override
    public double getMean() {
        return mean;
    }
}
