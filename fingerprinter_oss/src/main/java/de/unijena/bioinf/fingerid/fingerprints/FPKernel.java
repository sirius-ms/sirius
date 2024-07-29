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

package de.unijena.bioinf.fingerid.fingerprints;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;

public class FPKernel {


    protected static class ECFPKernel {

        public ECFPKernel() {

        }

        public double[] oneAgainstAll(IAtomContainer a, List<IAtomContainer> rest) throws CDKException {
            final double[] kernel = new double[rest.size()];

            final CircularFingerprinter circularFingerprinter = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6);
            circularFingerprinter.calculate(a);
            final TIntIntHashMap LEFT = new TIntIntHashMap(100,0.75f,-1,0), RIGHT = new TIntIntHashMap(100,0.75f,-1,0);
            final TIntHashSet keys = new TIntHashSet(100,0.75f, -1);
            for (int k=0; k < circularFingerprinter.getFPCount(); ++k) {
                final CircularFingerprinter.FP fp = circularFingerprinter.getFP(k);
                LEFT.adjustOrPutValue(fp.hashCode, 1, 1);
            }
            final CircularFingerprinter right = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6);
            for (int i=0; i < rest.size(); ++i) {
                RIGHT.clear();
                keys.clear();
                keys.addAll(LEFT.keys());
                right.calculate(rest.get(i));
                for (int k=0; k < right.getFPCount(); ++k) {
                    final CircularFingerprinter.FP fp = right.getFP(k);
                    RIGHT.adjustOrPutValue(fp.hashCode, 1, 1);
                    keys.add(fp.hashCode);
                }

                for (int key : keys.toArray()) {
                    kernel[i] += Math.min(LEFT.get(key), RIGHT.get(key)) / (double)Math.max(LEFT.get(key), RIGHT.get(key));
                }
            }
            return kernel;
        }

    }

    protected static class ShortestPathKernel {

        public ShortestPathKernel() {

        }

        public double[] oneAgainstAll(IAtomContainer a, List<IAtomContainer> rest) throws CDKException {
            return null;
        }


    }

}
