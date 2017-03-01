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
package de.unijena.bioinf.ChemistryBase.ms.ft;

import com.google.common.base.Joiner;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecalibrationFunction {

    private double[] polynomialTerms;

    public RecalibrationFunction(double[] polynomialTerms) {
        this.polynomialTerms = polynomialTerms;
    }

    public double[] getPolynomialTerms() {
        return polynomialTerms;
    }

    public void setPolynomialTerms(double[] polynomialTerms) {
        this.polynomialTerms = polynomialTerms;
    }

    public double apply(double x) {
        double val = 0;
        for (int k=0; k < polynomialTerms.length; ++k) {
            val += polynomialTerms[k] * Math.pow(x, k);
        }
        return val;
    }

    public String toString() {
        final List<String> terms = new ArrayList<String>(polynomialTerms.length);
        for (int k=polynomialTerms.length-1; k >= 0; --k) {
            if (polynomialTerms[k]==0) continue;
            terms.add(k==0 ? String.valueOf(polynomialTerms[k]) : (k == 1 ? polynomialTerms[k]+"x" : polynomialTerms[k]+"x^"+k));
        }
        return Joiner.on(" + ").join(terms);
    }

    private static String DOUBLE_REG = "[-+]?[0-9]+\\.?[0-9]+([eE][-+]?[0-9]+)?";
    private static Pattern FMATCH = Pattern.compile("("+DOUBLE_REG+")(x(?:^(\\d+))?)?");
    public static RecalibrationFunction fromString(String s) {
        final Matcher m = FMATCH.matcher(s);
        final TDoubleArrayList list = new TDoubleArrayList();
        while (m.find()) {
            final double constTerm = Double.parseDouble(m.group(1));
            final int degree;
            final String x = m.group(3);
            if (x==null || x.isEmpty()) {
                degree=0;
            } else {
                final String y = m.group(4);
                if (y==null || y.isEmpty()) {
                    degree=1;
                } else degree = Integer.parseInt(y);
            }
            if (degree > 1000) throw new IllegalArgumentException("maximal allowed polynomial is 1000");
            while (list.size() <= degree) list.add(0d);
            list.set(degree, constTerm);
        }
        return new RecalibrationFunction(list.toArray());
    }

    public static RecalibrationFunction identity() {
        return new RecalibrationFunction(new double[]{0d,1d});
    }
}
