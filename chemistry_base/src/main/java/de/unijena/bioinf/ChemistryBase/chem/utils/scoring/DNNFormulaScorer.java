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

package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;

import java.util.ArrayList;
import java.util.List;

public class DNNFormulaScorer implements MolecularFormulaScorer {

    private final List<double[][]> W;
    private final List<double[]> b;

    public DNNFormulaScorer() {
        W = new ArrayList<>();
        this.b = new ArrayList<>();
    }

    public void addLayer(double[][] weights, double[] bias) {
        W.add(weights);
        b.add(bias);
    }

    public double getDecisionValue(MolecularFormula formula) {
        double[] X = new FormulaFeatureVector(formula).getAlternativeFeatures();
        double y = 0d;
        int lastLayer = W.size()-1;
        for (int i=0; i < W.size(); ++i) {
            final double[][] weights = W.get(i);
            final double[] bias = b.get(i);
            double[] out = new double[weights[0].length];
            for (int row=0; row < weights.length; ++row) {
                final double[] rw = weights[row];
                for (int col=0; col < rw.length; ++col) {
                    out[col] += X[row]*rw[col];
                }
            }
            for (int col=0; col < out.length; ++col) {
                out[col] += bias[col];
                if (i<lastLayer) { // ReLu
                    out[col] = Math.max(0, out[col]);
                }
            }
            if (i==lastLayer) {
                y = out[0];
            } else {
                X = out;
            }
        }
        return y;
    }

    @Override
    public double score(MolecularFormula formula) {
        return 0;
    }
}
