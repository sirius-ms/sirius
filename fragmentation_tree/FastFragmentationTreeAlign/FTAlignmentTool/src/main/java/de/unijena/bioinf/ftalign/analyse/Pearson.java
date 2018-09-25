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
package de.unijena.bioinf.ftalign.analyse;

/**
 * @author Kai Dührkop
 */
public class Pearson {

    public static double pearson(double[] xs, double[] ys) {
        final double Exs = expectation(xs);
        final double Eys = expectation(ys);
        final double n = (Math.sqrt(variance(xs, Exs)) * Math.sqrt(variance(ys, Eys)));
        return (covariance(xs, ys, Exs, Eys) / n);
    }

    public static double expectation(double[] xs) {
        double sum = 0d;
        for (int i=0; i < xs.length; ++i) {
            sum += xs[i];
        }
        sum /= xs.length;
        return sum;
    }

    public static double variance(double[] xs) {
        return variance(xs, expectation(xs));
    }

    public static double variance(double[] xs, double expectation) {
        double sum = 0d;
        for (int i=0; i < xs.length; ++i) {
            final double d = xs[i] - expectation;
            sum += (d*d);
        }
        return sum / xs.length;
    }

    public static double covariance(double[] xs, double[] ys) {
        return covariance(xs, ys, expectation(xs), expectation(ys));
    }

    public static double covariance(double[] xs, double[] ys, double expectation_x, double expectation_y) {
        if (xs.length != ys.length) throw new IllegalArgumentException("Both arrays should have the same length!");
        double sum = 0d;
        for (int i=0; i < xs.length; ++i) {
            sum += (xs[i] - expectation_x) * (ys[i] - expectation_y);
        }
        return sum / xs.length;
    }

}
