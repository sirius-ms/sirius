
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

import static java.lang.Math.*;

@HasParameters
public class LogNormalDistribution extends RealDistribution {

    public static final double SQRT2PI = sqrt(2 * PI);
    private final double mean, var, sd;
    private final double shift;

    public static LogNormalDistribution withMeanAndSd(double mean, double sd) {
        return new LogNormalDistribution(mean, sd*sd);
    }

    public LogNormalDistribution(@Parameter("mean") double mean, @Parameter("variance") double var) {
        this.mean = mean;
        this.var = var;
        this.sd = sqrt(var);
        this.shift = 0d;
    }
    public LogNormalDistribution(double mean, double var, double shift) {
        this.mean = mean;
        this.var = var;
        this.sd = sqrt(var);
        this.shift = shift;
    }



    @Override
    public String toString() {
        return "LogNormalDistribution(\u03BC=" + mean + ", \u03C3\u00B2=" + var + ")";
    }

    public static LogNormalDistribution learnFromData(double[] xs) {
        double mean = 0d;
        for (double x : xs) mean += log(x);
        mean /= xs.length;
        double var = 0d;
        for (double x : xs) {
            final double s = log(x)-mean;
            var += s*s;
        }
        return new LogNormalDistribution(mean, var);
    }

    @Override
    public double getDensity(double x) {
        x += shift;
        return (1/(SQRT2PI *sd*x) * exp(-pow(log(x)-mean, 2)/(2*var)));
    }

    @Override
    public double getCumulativeProbability(double x) {
        x += shift;
        return MathUtils.cdf(log(x), mean, var);
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
