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

package de.unijena.bioinf.ChemistryBase.ms.lcms;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class SampleLibrary {

    protected final ArrayList<String> names;
    protected final ArrayList<MsDataSourceReference> references;
    protected final TObjectIntHashMap<String> name2index;
    protected final TObjectIntHashMap<MsDataSourceReference> ref2index;
    protected ReentrantLock lock;

    public SampleLibrary() {
        this.lock = new ReentrantLock();
        this.names = new ArrayList<>();
        this.references = new ArrayList<>();
        this.name2index = new TObjectIntHashMap<>(20,0.75f, -1);
        this.ref2index = new TObjectIntHashMap<>(20,0.75f, -1);
    }

    public Optional<Integer> getIndexFor(MsDataSourceReference reference) {
        final int index = ref2index.get(reference);
        if (index < 0) return Optional.empty();
        else return Optional.of(index);
    }
    public Optional<Integer> getIndexFor(String name) {
        final int index = name2index.get(name);
        if (index < 0) return Optional.empty();
        else return Optional.of(index);
    }

    public Optional<String> getNameFor(MsDataSourceReference reference) {
        final int index = ref2index.get(reference);
        if (index < 0) return Optional.empty();
        else return Optional.of(names.get(index));
    }

    public String getNameAt(int index) {
        return names.get(index);
    }

    public MsDataSourceReference getReferenceAt(int index) {
        return references.get(index);
    }

    public Optional<MsDataSourceReference> getReferenceFor(String name) {
        final int index = name2index.get(name);
        if (index < 0) return Optional.empty();
        else return Optional.of(references.get(index));
    }

    public boolean register(String name, MsDataSourceReference reference) {
        if (name2index.containsKey(name) || ref2index.containsKey(reference)) {
            return false;
        }
        lock.lock();
        try {
            if (name2index.containsKey(name) || ref2index.containsKey(reference)) {
                return false;
            }
            final int index = names.size();
            names.add(name);
            references.add(reference);
            name2index.put(name,index);
            ref2index.put(reference,index);
            return true;
        } finally {
            lock.unlock();
        }
    }
}
