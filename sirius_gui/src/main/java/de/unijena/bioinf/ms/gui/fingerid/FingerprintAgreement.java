

/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;

import java.awt.*;
import java.util.ArrayList;

public class FingerprintAgreement {

    protected int[] indizes;
    protected double[] weights;
    protected double[] weights2;
    protected int x, y, w, h, numberOfCols;

    private FingerprintAgreement(int[] indizes, double[] weights, double[] weights2) {
        this.indizes = indizes;
        this.weights = weights;
        this.weights2 = weights2;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void setNumberOfCols(int colsize) {
        this.numberOfCols = colsize;
    }

    public Rectangle getBounds() {
        return new Rectangle(x,y,w,h);
    }

    public static FingerprintAgreement getSubstructures(FingerprintVersion version, final double[] platts, boolean[] reference, PredictionPerformance[] performances, double occurenceThreshold) {
        // only pick fingerprints where #occurences is smaller than 25%
        final ArrayList<Integer> list = new ArrayList<>();
        double T = performances[0].numberOfSamplesWithPseudocounts()*occurenceThreshold;
        for (int k=0; k < reference.length; ++k) {
            if (reference[k]  && (performances[k].getTp() + performances[k].getFn()) <= T) {
                list.add(k);
            }
        }
        final int[] indizes = new int[list.size()];
        final double[] weights = new double[list.size()];
        final double[] weights2 = new double[list.size()];
        list.sort((o1, o2) -> Double.compare(platts[o2], platts[o1]));
        int k=0;
        for (int i : list) {
            indizes[k] = version.getAbsoluteIndexOf(i);
            weights[k] = platts[i];
            weights2[k] = performances[i].getF();
            ++k;
        }
        return new FingerprintAgreement(indizes, weights, weights2);
    }

    public int indexAt(int row, int col) {
        try {
            return indizes[row* numberOfCols + col];
        }catch (IndexOutOfBoundsException e) {
            return 0;
        }
    }
}
