
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

import static java.lang.Math.exp;
import static java.lang.Math.log;

@HasParameters
public final class ExponentialDistribution extends RealDistribution {

    @Parameter
    private final double lambda;

    public static ExponentialDistribution fromLambda(double lambda) {
        return new ExponentialDistribution(lambda);
    }

    public static ExponentialDistribution fromMean(double mean) {
        return new ExponentialDistribution(1/mean);
    }

    public ExponentialDistribution(@Parameter("lambda") double lambda) {
        this.lambda = lambda;
    }

    @Override
    public double getDensity(double x) {
        return x < 0d ? 0d : lambda * exp(-lambda*x);
    }

    @Override
    public double getCumulativeProbability(double x) {
        if (x < 0) return 0;
        return 1-exp(-lambda*x);
    }

    @Override
    public double getInverseLogCumulativeProbability(double x) {
        if (x < 0) return Double.NEGATIVE_INFINITY;
        return -lambda*x;
    }

    @Override
    public double getVariance() {
        return 1d/(lambda*lambda);
    }

    @Override
    public double getMean() {
        return 1d/lambda;
    }

    public double getMedian() {
        return log(2)/lambda;
    }

    @Override
    public String toString() {
        return "ExponentialDistribution(\u03BB = " + lambda + ")";
    }

    public static ExponentialDistribution learnFromData(double[] values) {
        double avg = 0d;
        for (double v : values) avg += v;
        return ExponentialDistribution.fromMean(avg/values.length);
    }

    public static ByMedianEstimatable<ExponentialDistribution> getMedianEstimator() {
        return new EstimateByMedian();
    }

    public double getLambda() {
        return lambda;
    }

    @HasParameters
    public static class EstimateByMedian implements ByMedianEstimatable<ExponentialDistribution> {
        public EstimateByMedian() {
        }
        @Override
        public ExponentialDistribution extimateByMedian(double median) {
            return ExponentialDistribution.fromLambda(Math.log(2)/median);
        }
    }
}
