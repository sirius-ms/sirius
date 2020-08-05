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

package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import gnu.trove.map.hash.TIntIntHashMap;
import org.openscience.cdk.exception.CDKException;

public class SubstructureLinearKernel implements MoleculeKernel<SubstructureLinearKernel.Prepared> {

    private final int diameter;

    public SubstructureLinearKernel(int diameter) {
        this.diameter = diameter;
    }

    public SubstructureLinearKernel() {
        this.diameter = CircularFingerprinter.CLASS_ECFP6;
    }

    @Override
    public Prepared prepare(PredictableCompound compound) {
        final CircularFingerprinter fp = new CircularFingerprinter(diameter);
        try {
            fp.calculate(compound.getMolecule());
            return new Prepared(fp);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Prepared preparedLeft, Prepared preparedRight) {
        int[] dot = new int[]{0,0,0};
        preparedLeft.fps.forEachEntry((key,value)->{
            final int value2 = preparedRight.fps.get(key);
            dot[0] += value*value2;
            dot[1] += value*value;
            dot[2] += value2*value2;
            return true;
        });
        preparedRight.fps.forEachEntry((key,value)->{
            final int value2 = preparedLeft.fps.get(key);
            if (value2 == 0) {
                dot[2] += value*value;
            }
            return true;
        });
        return ((double)dot[0])/Math.sqrt((double)dot[1]*dot[2]);
    }

    public static class Prepared {
        protected final TIntIntHashMap fps;
        public Prepared(CircularFingerprinter fp) {
            this.fps = new TIntIntHashMap(125,0.75f,0,0);
            for (int i=0; i < fp.getFPCount(); ++i) {
                this.fps.adjustOrPutValue(fp.getFP(i).hashCode,1,1);
            }
        }
    }



}
