
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

package de.unijena.bioinf.IsotopePatternAnalysis.util;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

/**
 * A piecewise linear function F: Intensity {@literal ->} R.
 * Each

 */
public class PiecewiseLinearFunctionIntensityDependency extends FixedBagIntensityDependency {

    public PiecewiseLinearFunctionIntensityDependency() {
    }

    public PiecewiseLinearFunctionIntensityDependency(double[] bags, double[] values) {
        super(bags, values);
    }

    @Override
    public double getValueAt(double intensity) {
        int i;
        for (i=0; i < bags.length; ++i) {
            if (intensity > bags[i]) {
                break;
            }
        }
        if (i >= bags.length) return values[bags.length-1];
        if (i==0) return values[0];
        final double p = (intensity-bags[i])/(bags[i-1] - bags[i]);
        return (p*values[i-1] + (1-p)*values[i]);
    }

    @Override
    public <G, D, L> PiecewiseLinearFunctionIntensityDependency readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L bagsList = document.getListFromDictionary(dictionary, "bags");
        final double[] bags = new double[document.sizeOfList(bagsList)];
        for (int i = 0; i < bags.length; i++) {
            bags[i] = document.getDoubleFromList(bagsList, i);
        }
        final L valuesList = document.getListFromDictionary(dictionary, "values");
        final double[] values = new double[document.sizeOfList(valuesList)];
        for (int i = 0; i < values.length; i++) {
            values[i] = document.getDoubleFromList(valuesList, i);
        }
        return new PiecewiseLinearFunctionIntensityDependency(bags, values);
    }
}
