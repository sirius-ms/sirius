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
import gnu.trove.map.hash.TLongShortHashMap;
import gnu.trove.procedure.TLongShortProcedure;
import gnu.trove.set.hash.TIntHashSet;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import java.util.zip.CRC32;

public class OutGroupKernel implements MoleculeKernel<OutGroupKernel.Prepared> {


    @Override
    public Prepared prepare(PredictableCompound compound) {
        return new Prepared(compound);
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Prepared preparedLeft, Prepared preparedRight) {
        final long[] counts = new long[3];
        preparedLeft.map.forEachEntry(new TLongShortProcedure() {
            @Override
            public boolean execute(long a, short l) {
                long r = preparedRight.map.get(a);
                counts[0] += l*r;
                counts[1] += l*l;
                return true;
            }
        });
        preparedRight.map.forEachEntry(new TLongShortProcedure() {
            @Override
            public boolean execute(long a, short r) {
                long l = preparedLeft.map.get(a);
                counts[0] += l*r;
                counts[2] += r*r;
                return true;
            }
        });
        double uv = counts[0]/2d, uu = counts[1], vv = counts[2];
        if (uu==0 || vv==0) return 0;
        return uv / Math.sqrt(uu*vv);

    }

    public static TLongShortHashMap makeExplicitMap(IAtomContainer molecule) {
        final CRC32 crc            = new CRC32();
        final TIntHashSet visited = new TIntHashSet();
        final TLongShortHashMap map = new TLongShortHashMap(16, 0.75f,0,(short)0);
        // we search for all atoms with degree 1, then extend until we find atom with degree >= 3
        for (IAtom atom : molecule.atoms()) {
            if (atom.getBondCount()==1) {
                visited.clear();
                visited.add(atom.getIndex());
                long hash = atomHash(crc, atom);
                IBond bond = atom.bonds().iterator().next();
                IAtom neighbour = bond.getOther(atom);
                while (true) {
                    if (neighbour.getBondCount() != 2)
                        break;
                    if (!visited.add(neighbour.getIndex())) break;
                    hash += (bond.getOrder().numeric()*75983L * (bond.isAromatic() ? 1 : 3));
                    hash += atomHash(crc, neighbour);
                    for (IBond b : neighbour.bonds()) {
                        if (b != bond) {
                            bond = b;
                            neighbour = bond.getOther(neighbour);
                            break;
                        }
                    }
                }
                if (hash==0) ++hash;
                map.put(hash, (short)(map.get(hash)+1));
            }
        }
        return map;
    }

    private static long atomHash(CRC32 crc, IAtom atom) {
        crc.reset();
        crc.update(atom.isInRing() ? 1 : 0);
        crc.update(atom.getFormalCharge());
        crc.update(atom.getImplicitHydrogenCount());
        crc.update(atom.isAromatic() ? 8 : 4);
        crc.update(atom.getHybridization().ordinal());
        crc.update(atom.getAtomicNumber());
        return crc.getValue()*98849L;
    }

    public static class Prepared {
        private TLongShortHashMap map;
        public Prepared(PredictableCompound compound) {
            this.map = makeExplicitMap(compound.getMolecule());
        }
    }

}
