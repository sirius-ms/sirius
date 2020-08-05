
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

import java.util.Arrays;

public class FixedBagIntensityDependency implements IntensityDependency {

    protected double[] bags;
    protected double[] values;

    public FixedBagIntensityDependency(){
        bags = new double[0];
        values = new double[0];
    }
    public FixedBagIntensityDependency(double[] bags, double[] values) {
        if (bags.length != values.length) throw new IllegalArgumentException("array sizes differ");
        this.bags = Arrays.copyOf(bags, bags.length);
        this.values = Arrays.copyOf(values, values.length);
    }

    public double[] getBags() {
        return bags;
    }

    public double[] getValues() {
        return values;
    }

    @Override
    public double getValueAt(double intensity) {
        double minDiff = Double.POSITIVE_INFINITY;
        int minIndex = 0;
        for (int i=0; i < bags.length; ++i) {
            final double diff = Math.abs(bags[i] - intensity);
            if (diff < minDiff) {
                minDiff = diff;
                minIndex = i;
            }
        }
        return values[minIndex];
    }

    @Override
    public <G, D, L> FixedBagIntensityDependency readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
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
        return new FixedBagIntensityDependency(bags, values);
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L bagsList = document.newList();
        for (double bag : bags) {
            document.addToList(bagsList, bag);
        }
        final L valuesList = document.newList();
        for (double value : values) {
            document.addToList(valuesList, value);
        }
        document.addListToDictionary(dictionary, "bags", bagsList);
        document.addListToDictionary(dictionary, "values", valuesList);
    }
}
