
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

package de.unijena.bioinf.babelms.chemdb;

import de.unijena.bioinf.ChemistryBase.algorithm.FastReadWriteLock;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaSet;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.procedure.TIntLongProcedure;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Set;

/**
 * - The DBMolecularFormulaCache stores queries to molecular formulas for a certain database
 * - a query covers ranges of 1 mDa.
 */
public class DBMolecularFormulaCache {
    private final TIntLongHashMap covered;
    private final MolecularFormulaSet known;
    private final CompoundQuery query;
    private final Databases database;
    private final FastReadWriteLock shortLock;
    private int changes;

    public DBMolecularFormulaCache(TIntLongHashMap covered, MolecularFormulaSet known, Databases database) {
        this.covered = covered;
        this.known = known;
        this.database = database;
        this.query = database.getQuery();
        this.shortLock = new FastReadWriteLock();
        this.changes = 0;
    }

    public DBMolecularFormulaCache(ChemicalAlphabet alphabet, Databases database) {
        this(new TIntLongHashMap(), new MolecularFormulaSet(alphabet), database);
    }

    public void store(final OutputStream stream) throws IOException {
        final BufferedOutputStream buffered = (stream instanceof BufferedOutputStream) ? (BufferedOutputStream)stream
                : new BufferedOutputStream(stream);
        final DataOutputStream outstream = new DataOutputStream(buffered);
        outstream.writeUTF(database.name());
        outstream.writeInt(covered.size());
        try {
            covered.forEachEntry(new TIntLongProcedure() {
                @Override
                public boolean execute(int a, long b) {
                    try {
                        outstream.writeInt(a);
                        outstream.writeLong(b);
                    } catch (IOException e) {
                        throw new RuntimeException();
                    }
                    return true;
                }
            });
        } catch (RuntimeException e) {
            final Throwable t = e.getCause();
            if (t != null && t instanceof IOException) throw (IOException)t;
            else throw e;
        }
        outstream.flush();
        known.store(buffered);
    }

    public static DBMolecularFormulaCache load(final InputStream stream) throws IOException {
        final BufferedInputStream buffered = (stream instanceof BufferedInputStream) ? (BufferedInputStream)stream
                : new BufferedInputStream(stream);
        final DataInputStream instream = new DataInputStream(buffered);
        final Databases database = Databases.valueOf(instream.readUTF());
        final int entries = instream.readInt();
        final TIntLongHashMap bitset = new TIntLongHashMap(entries);
        for (int k=0; k < entries; ++k) {
            final int key = instream.readInt();
            final long value = instream.readLong();
            bitset.put(key, value);
        }
        final MolecularFormulaSet set = MolecularFormulaSet.load(instream);
        return new DBMolecularFormulaCache(bitset, set, database);
    }

    public int getChanges() {
        return changes;
    }


    public boolean isFormulaExist(MolecularFormula formula) {
        checkMass(formula.getMass());
        return known.contains(formula);
    }

    private void checkMass(double mass) {
        final int k = (int)Math.floor(mass*5d*64d);
        final long mask = 1l<<(k%64);
        final int slot = k/64;
        boolean isCovered;
        while (true) {
            final long locked = shortLock.startReading();
            isCovered = ((covered.get(slot) & mask) != 0);
            if (shortLock.canFinishReading(locked)) break;
        }
        if (!isCovered) {
            checkMassRange(mass, new Deviation(10));
        }
    }

    public void checkMassRange(double mass, Deviation deviation) {
        final double startMass = mass - deviation.absoluteFor(mass);
        final double endMass = mass + deviation.absoluteFor(mass);
        final int startInt = (int)Math.floor(startMass*5d*64d);
        final int endInt = (int)Math.ceil(endMass*5d*64d);
        int currentSlot = -1;
        long currentSlotValue = 0;
        for (int k=startInt; k <= endInt; ++k) {
            final int slot = k / 64;
            if (currentSlot!=slot) {
                shortLock.startWriting();
                try {
                    if (currentSlot>=0) covered.put(currentSlot, covered.get(currentSlot)|currentSlotValue);
                    currentSlot = slot;
                    currentSlotValue = covered.get(slot);
                    if (currentSlotValue == -1l) {
                        k = (slot+1)*64;
                        continue;
                    }
                } finally {
                    shortLock.finishWriting();
                }
            }
            final int index = k % 64;
            final long mask = (1l<<index);
            if ((currentSlotValue & mask) == 0) {
                startQueryFor(k);
            }
        }
        if (currentSlot>=0) {
            shortLock.startWriting();
            covered.put(currentSlot, covered.get(currentSlot) | currentSlotValue);
            shortLock.finishWriting();
        }
    }

    private void startQueryFor(int k) {
        final double width = 1d / (5d*64d);
        final double dev = width/2d;
        final double mass = k * width + dev;
        final Set<MolecularFormula> formulas = query.findMolecularFormulasByMass(mass, dev + 1e-5);
        LoggerFactory.getLogger(this.getClass()).error("START QUERY FROM " + (mass-dev) + " TO " + (mass+dev));
        shortLock.startWriting();
        covered.put(k/64, covered.get(k/64) | (1l<<(k%64)));
        ++changes;
        try {
            for (MolecularFormula f : formulas) {
                known.add(f);
            }
        } finally {
            shortLock.finishWriting();
        }

    }

}
