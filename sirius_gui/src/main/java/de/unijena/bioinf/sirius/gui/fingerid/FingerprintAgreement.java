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

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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

    public static FingerprintAgreement getAgreement(FingerprintVersion version, final double[] platts, boolean[] reference, double[] fscores, double threshold) {
        final ArrayList<Integer> list = new ArrayList<>();
        for (int k=0; k < reference.length; ++k) {
            if (reference[k] && platts[k] >= threshold) {
                list.add(k);
            }
        }
        final int[] indizes = new int[list.size()];
        final double[] weights = new double[list.size()];
        final double[] weights2 = new double[list.size()];
        Collections.sort(list, new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(platts[o2], platts[o1]);
            }
        });
        int k=0;
        for (int i : list) {
            indizes[k] = version.getAbsoluteIndexOf(i);
            weights[k] = (platts[i]-threshold)/(1d-threshold);
            weights2[k] = fscores[i];
            ++k;
        }
        return new FingerprintAgreement(indizes, weights, weights2);
    }

    public static FingerprintAgreement getMissing(FingerprintVersion version, final double[] platts, boolean[] reference, double[] fscores, double threshold) {
        final ArrayList<Integer> list = new ArrayList<>();
        for (int k=0; k < reference.length; ++k) {
            if (reference[k] && platts[k] <= threshold) {
                list.add(k);
            }
        }
        final int[] indizes = new int[list.size()];
        final double[] weights = new double[list.size()];
        final double[] weights2 = new double[list.size()];
        Collections.sort(list, new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(platts[o1], platts[o2]);
            }
        });
        int k=0;
        for (int i : list) {
            indizes[k] = version.getAbsoluteIndexOf(i);
            weights[k] = (threshold-platts[i])/threshold;
            weights2[k] = fscores[i];
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
