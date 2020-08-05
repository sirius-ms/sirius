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
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.procedure.TLongLongProcedure;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.AllPairsShortestPaths;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class SubstructurePathKernel2 implements MoleculeKernel<SubstructurePathKernel2.Prepared> {

    protected int diameter;

    public SubstructurePathKernel2(int diameter) {
        this.diameter = diameter;
    }

    @Override
    public Prepared prepare(PredictableCompound compound) {
        try {
            return new Prepared(compound.getMolecule(),diameter);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Prepared l, Prepared r) {
        return compareLinear(l.explicitMap,r.explicitMap);
    }

    public double compareTanimoto(TLongLongHashMap left, TLongLongHashMap right) {
        long[] count = new long[2];
        left.forEachEntry(new TLongLongProcedure() {
            @Override
            public boolean execute(long a, long b) {
                long s = right.get(a)&b;
                count[0] += Long.bitCount(s);
                count[1] += Long.bitCount(b);
                return true;
            }
        });
        right.forEachEntry(new TLongLongProcedure() {
            @Override
            public boolean execute(long a, long b) {
                long s = left.get(a)&b;
                count[0] += Long.bitCount(s);
                count[1] += Long.bitCount(b);
                return true;
            }
        });
        assert count[0]%2==0;
        count[0] /= 2;
        double intersection = count[0];
        double union = count[1]-count[0];
        return intersection/union;
    }

    public double compareLinear(TLongLongHashMap left, TLongLongHashMap right) {
        long[] count = new long[1];
        left.forEachEntry(new TLongLongProcedure() {
            @Override
            public boolean execute(long a, long b) {
                long s = right.get(a)&b;
                count[0] += Long.bitCount(s);
                return true;
            }
        });
        right.forEachEntry(new TLongLongProcedure() {
            @Override
            public boolean execute(long a, long b) {
                long s = left.get(a)&b;
                count[0] += Long.bitCount(s);
                return true;
            }
        });
        return count[0];
    }

    static TLongLongHashMap makeExplicitMap(int[][] identities, AllPairsShortestPaths paths) {
        final TLongLongHashMap map = new TLongLongHashMap(32,0.75f,0,0);
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        final LongBuffer aslong = buffer.asLongBuffer();
        final IntBuffer asInt = buffer.asIntBuffer();
        for (int i=0; i < identities[0].length; ++i) {
            for (int j=0; j < identities[0].length; ++j) {
                for (int m=0; m < identities.length; ++m) {
                    for (int n=0; n < identities.length; ++n) {
                        final int len = paths.from(i).distanceTo(j);
                        if (len==0)
                            continue;
                        int a = identities[m][i];
                        int b = identities[n][j];
                        asInt.put(a);
                        asInt.put(b);
                        asInt.rewind();
                        long X = aslong.get(0);

                        long set = map.get(X);
                        int pathSize = Math.min(63, len);
                        set |= (1L<<pathSize);
                        map.put(X, set);
                    }
                }
            }
        }
        return map;
    }

    public static class Prepared {
        protected final int[][] identities;
        protected HashMap<Integer, int[]> identity2atoms;
        protected AllPairsShortestPaths shortestPaths;
        protected TLongLongHashMap explicitMap;
        public Prepared(IAtomContainer molecule, int diameter) throws CDKException {
            int st;
            switch (diameter) {
                case 0: st=CircularFingerprinter.CLASS_ECFP0;break;
                case 2: st=CircularFingerprinter.CLASS_ECFP2;break;
                case 4: st=CircularFingerprinter.CLASS_ECFP4;break;
                case 6: st=CircularFingerprinter.CLASS_ECFP6;break;
                default: throw new IllegalArgumentException("Unsupported diameter");
            }
            CircularFingerprinter fp = new CircularFingerprinter(st);
            fp.storeIdentitesPerIteration = true;
            fp.calculate(molecule);
            this.identities = fp.identitiesPerIteration.toArray(int[][]::new);
            this.identity2atoms = new HashMap<>();
            final int[] empty = new int[0];
            for (int j=0; j < identities.length; ++j) {
                for (int i = 0; i < identities[j].length; ++i) {
                    int[] ary = identity2atoms.getOrDefault(identities[j][i], empty);
                    int[] copy = Arrays.copyOf(ary, ary.length + 1);
                    copy[copy.length - 1] = i;
                    identity2atoms.put(identities[j][i], copy);
                }
            }
            this.shortestPaths = new AllPairsShortestPaths(molecule);
            this.explicitMap = makeExplicitMap(identities,shortestPaths);
        }
    }



}
