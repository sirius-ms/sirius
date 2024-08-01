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

package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.fp.AbstractFingerprint;

/**
 * Created by Marcus Ludwig on 04.07.16.
 */
public class CompoundWithAbstractFP<T extends AbstractFingerprint> {

    private final InChI inchi;
    private final T fingerprint;

    public CompoundWithAbstractFP(InChI inchi, T fingerprint) {
        this.inchi = inchi;
        this.fingerprint = fingerprint;
    }

    public InChI getInchi() {
        return inchi;
    }

    public T getFingerprint() {
        return fingerprint;
    }
}
