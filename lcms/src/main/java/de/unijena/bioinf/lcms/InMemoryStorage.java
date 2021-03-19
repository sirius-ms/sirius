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

package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.model.lcms.Scan;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;

/**
 * Stores spectra in memory. Not threadsafe when used for writing!
 */
public class InMemoryStorage implements SpectrumStorage {

    protected final TIntObjectHashMap<SimpleSpectrum> scan2spectrum;

    public InMemoryStorage() {
        this.scan2spectrum = new TIntObjectHashMap<>();
    }

    @Override
    public synchronized void add(Scan scan, SimpleSpectrum spectrum) {
        scan2spectrum.put(scan.getIndex(), spectrum);
    }

    @Override
    public SimpleSpectrum getScan(Scan scan) {
        return scan2spectrum.get(scan.getIndex());
    }

    @Override
    public void close() throws IOException {
        // dummy
    }
}
