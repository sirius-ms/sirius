
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


import static java.lang.Math.log;

public abstract class RealDistribution implements IsRealDistributed {

    public double getLogDensity(double x) {
        return log(getDensity(x));
    }

    public double getLogProbability(double begin, double end) {
        return log(getProbability(begin, end));
    }

    public double getLogCumulativeProbability(double x) {
        return log(getCumulativeProbability(x));
    }

    public double getInverseLogCumulativeProbability(double x) {
        return log(1-getCumulativeProbability(x));
    }

    @Override
    public double getProbability(double begin, double end) {
        if (end < begin) throw new IllegalArgumentException();
        if (end==begin) return 0d;
        return getCumulativeProbability(end) - getCumulativeProbability(begin);
    }

    public static RealDistribution wrap(final IsRealDistributed d) {
        return new RealDistribution(){

            @Override
            public double getDensity(double x) {
                return d.getDensity(x);
            }

            @Override
            public double getCumulativeProbability(double x) {
                return d.getCumulativeProbability(x);
            }

            @Override
            public double getVariance() {
                return d.getVariance();
            }

            @Override
            public double getMean() {
                return d.getMean();
            }
        };
    }
}
