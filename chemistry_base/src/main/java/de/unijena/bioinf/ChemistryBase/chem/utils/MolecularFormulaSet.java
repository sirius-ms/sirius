
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

package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.algorithm.FastReadWriteLock;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class MolecularFormulaSet extends AbstractSet<MolecularFormula> {

    private final TLongHashSet hashset;
    private final HashSet<MolecularFormula> uncompressed;
    private final MolecularFormulaPacker packer;
    private final FastReadWriteLock lock;

    private final static long MASK = (Long.MIN_VALUE + ((1l << 32) - 1l));
    private final static long ADD_SIGN_FLAG = (1l << 31);
    private final static int CLEAR_SIGN_FLAG = Integer.MAX_VALUE;

    public MolecularFormulaSet(MolecularFormulaPacker packer) {
        this.hashset = new TLongHashSet(1000);
        this.uncompressed = new HashSet<MolecularFormula>(100);
        this.packer = packer;
        this.lock = new FastReadWriteLock();
    }

    public int numberOfCompressedFormulas() {
        return hashset.size();
    }
    public int numberOfUncompressedFormulas() {
        return uncompressed.size();
    }

    public MolecularFormulaSet(ChemicalAlphabet alphabet) {
        this(MolecularFormulaPacker.newPacker(alphabet));
    }

    public static MolecularFormulaSet load(InputStream stream) throws IOException {
        final DataInputStream din;
        {
            if (stream instanceof DataInputStream) din = (DataInputStream) stream;
            else {
                final BufferedInputStream bin;
                if (stream instanceof BufferedInputStream) bin = (BufferedInputStream) stream;
                else bin = new BufferedInputStream(stream);
                din = new DataInputStream(bin);
            }
        }

        // read meta information
        final Charset ASCII = Charset.forName("US-ASCII");
        final int num32 = din.readInt();
        final int num64 = din.readInt();
        final int numX = din.readInt();
        final int bytesX = din.readInt();
        final MolecularFormulaPacker packer;
        {
            final int len = din.readInt();
            final byte[] packerFormatBytes = new byte[len];
            int offset = 0;
            while (offset < len) {
                offset += din.read(packerFormatBytes, offset, len);
            }
            packer = MolecularFormulaPacker.fromString(new String(packerFormatBytes, ASCII));
        }

        // add compressed formulas
        final MolecularFormulaSet set = new MolecularFormulaSet(packer);
        for (int k = 0; k < num32; ++k) {
            final int value = din.readInt();
            final long conv = (value >= 0) ? (long) value : ((value & (CLEAR_SIGN_FLAG)) | ADD_SIGN_FLAG);
            set.hashset.add(conv);
        }
        for (int k = 0; k < num64; ++k) {
            final long value = din.readLong();
            set.hashset.add(value);
        }
        // add uncompressed formulas
        final int _128_KILOBYTE = 128 * 1024;
        final byte[] buffer = new byte[_128_KILOBYTE];
        int offset = 0;
        for (int k = 0; k <= (bytesX / _128_KILOBYTE); ++k) {
            final int len = din.read(buffer, 0, Math.min(_128_KILOBYTE, bytesX - offset));
            offset += len;
            int lastSignificantByte = len - 1;
            for (; lastSignificantByte >= 0; --lastSignificantByte) {
                if (buffer[lastSignificantByte] != 0) break;
            }
            final String str = new String(buffer, 0, lastSignificantByte + 1, ASCII);
            for (String f : str.split(";")) {
                try {
                    set.uncompressed.add(MolecularFormula.parse(f));
                } catch (UnknownElementException e) {
                    LoggerFactory.getLogger(MolecularFormulaSet.class).warn("Could not parse formula: " + f + "Skipping this Entry!", e);
                }
            }
        }
        return set;
    }

    public void store(OutputStream stream) throws IOException {
        lock.startExclusiveReading();
        try {
            final BufferedOutputStream bout;
            if (stream instanceof BufferedOutputStream) {
                bout = (BufferedOutputStream) stream;
            } else bout = new BufferedOutputStream(stream);
            final DataOutputStream dout = new DataOutputStream(bout);
            final TLongArrayList f64 = new TLongArrayList(hashset.size() / 4);
            final TIntArrayList f32 = new TIntArrayList(hashset.size() / 4);
            final TLongIterator iter = hashset.iterator();
            final TByteArrayList buffer = new TByteArrayList();
            final Charset ASCII = Charset.forName("US-ASCII");
            final byte seperator = 59; //;
            final byte filler = 0; // whitespace
            int chunks = 0;
            final Iterator<MolecularFormula> fiter = uncompressed.iterator();
            final int _128_KILOBYTE = 128 * 1024;
            while (fiter.hasNext()) {
                final byte[] add = fiter.next().toString().getBytes(ASCII);
                if (add.length > 64000)
                    throw new RuntimeException("Strange molecular formula format: Molecular Formulas have to be smaller than 64KB");
                final int toadd = add.length + (fiter.hasNext() ? 1 : 0);
                if ((buffer.size() + toadd - chunks) > _128_KILOBYTE) {
                    final int tofill = (_128_KILOBYTE - (buffer.size() - chunks));
                    final byte[] fillerbits = new byte[tofill];
                    Arrays.fill(fillerbits, filler);
                    buffer.add(fillerbits);
                    chunks = buffer.size();
                }
                buffer.add(add);
                if (fiter.hasNext()) buffer.add(seperator);
            }
            while (iter.hasNext()) {
                final long value = iter.next();
                if ((value & MASK) == value) {
                    // clear sign bit
                    f32.add((int) (value));
                } else {
                    f64.add(value);
                }
            }
            // first store meta informations
            final String pack = packer.serializeToString();
            dout.writeInt(f32.size());
            dout.writeInt(f64.size());
            dout.writeInt(uncompressed.size());
            dout.writeInt(buffer.size());
            // store packer format
            dout.writeInt(pack.length());
            dout.write(pack.getBytes(ASCII));
            // then store 32 bit formulas
            for (int k = 0; k < f32.size(); ++k)
                dout.writeInt(f32.get(k));
            // then store 64 bit formulas
            for (int k = 0; k < f64.size(); ++k)
                dout.writeLong(f64.get(k));
            // finally store uncompressed formulas
            dout.write(buffer.toArray());
            dout.flush();
        } finally {
            lock.finishExclusiveReading();
        }
    }


    @Override
    public boolean isEmpty() {
        while (true) {
            final long v = lock.startReading();
            final boolean empty = hashset.isEmpty() && uncompressed.isEmpty();
            if (lock.canFinishReading(v)) return empty;
        }
    }

    @Override
    public boolean add(MolecularFormula f) {
        final long value = packer.tryEncode(f);
        lock.startWriting();
        try {
            if (value >= 0) return hashset.add(value);
            else return uncompressed.add(f);
        } finally {
            lock.finishWriting();
        }
    }

    @Override
    public void clear() {
        lock.startWriting();
        try {
            hashset.clear();
            uncompressed.clear();
        } finally {
            lock.finishWriting();
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof MolecularFormula) {
            final MolecularFormula f = (MolecularFormula) o;
            final long value = packer.tryEncode(f);
            lock.startWriting();
            try {
                if (value >= 0) return hashset.remove(value);
                else return uncompressed.remove(f);
            } finally {
                lock.finishWriting();
            }
        } else return false;
    }


    public boolean contains(MolecularFormula f) {
        final long value = packer.tryEncode(f);
        while (true) {
            final long locked = lock.startReading();
            final boolean contained = (value >= 0) ? hashset.contains(value) : uncompressed.contains(f);
            if (lock.canFinishReading(locked)) return contained;
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof MolecularFormula) return contains((MolecularFormula) o);
        else return false;
    }

    @Override
    public Iterator<MolecularFormula> iterator() {
        return new Iterator<MolecularFormula>() {
            final Iterator<MolecularFormula> uncompressedIterator = uncompressed.iterator();
            final TLongIterator iterator = hashset.iterator();
            boolean iteratesUncompressed = false;

            @Override
            public boolean hasNext() {
                lock.startReading(); // if some writing operation was done, the iterator should fail
                // so there is no need to unlock the reading lock
                return iterator.hasNext() || uncompressedIterator.hasNext();
            }

            @Override
            public MolecularFormula next() {
                if (iterator.hasNext()) return packer.decode(iterator.next());
                iteratesUncompressed = true;
                if (uncompressedIterator.hasNext()) return uncompressedIterator.next();
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                lock.startWriting();
                try {
                    if (iteratesUncompressed) uncompressedIterator.remove();
                    else iterator.remove();
                } finally {
                    lock.finishWriting();
                }
            }
        };
    }

    @Override
    public int size() {
        while (true) {
            final long v = lock.startReading();
            final int size = uncompressed.size() + hashset.size();
            if (lock.canFinishReading(v)) return size;
        }
    }
}
