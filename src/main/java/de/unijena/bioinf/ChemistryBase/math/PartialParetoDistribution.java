/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.ChemistryBase.math;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;

import static java.lang.Math.pow;

/**
 * A distribution which is uniform from a to b and pareto distributed from b to infinity
 */
@HasParameters
public class PartialParetoDistribution implements DensityFunction {

    private final double a, b, k, norm, opt, kdivbnorm;

    /**
     * @param a minimal allowed value. P(x < a) = 0
     * @param b interval [a,b) is optimal, For all x with a < x < b, density(x) is maximal
     * @param k shape parameter of underlying pareto distribution
     */
    public PartialParetoDistribution(@Parameter("a") double a, @Parameter("b") double b, @Parameter("k") double k) {
        this.a = a;
        this.b = b;
        this.k = k;
        final ParetoDistribution pareto = new ParetoDistribution(k, b);
        final double opt = pareto.getDensity(b);
        final double square = (b - a) * opt;
        this.norm = 1d / (square + 1d);
        this.opt = opt * this.norm;
        this.kdivbnorm = (k / b) * norm;
    }

    @Override
    public double getDensity(double x) {
        if (x < b) {
            if (x >= a) return opt;
            else return 0d;
        } else {
            return kdivbnorm * pow(b / x, k + 1);
        }
    }

    public ParetoDistribution getUnderlyingParetoDistribution() {
        return new ParetoDistribution(k, b);
    }

    public double getA() {
        return a;
    }

    public double getB() {
        return b;
    }

    public double getK() {
        return k;
    }
}
