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

package de.unijena.bioinf.ChemistryBase.fp;

/**
 * consists of a MASK that determines which bits are used and a list of Fingerprint types that determine which bits are
 * available
 */
public abstract class FingerprintVersion {
    public abstract MolecularProperty getMolecularProperty(int index);
    public abstract int size();
    public abstract boolean compatible(FingerprintVersion fingerprintVersion);
    //todo maybe use equals?
    public abstract boolean identical(FingerprintVersion fingerprintVersion);

    public int getRelativeIndexOf(int absoluteIndex) {
        return absoluteIndex;
    }
    public int getAbsoluteIndexOf(int relativeIndex) {
        return relativeIndex;
    }
    public boolean hasProperty(int absoluteIndex) {
        return absoluteIndex < size();
    }

    /**
     * returns the index of the molecular property either with the given absolute index or -(insertionPoint - 1) where
     * insertion point is same as in Arrays.binarysearch.
     */
    protected int getClosestRelativeIndexTo(int absoluteIndex) {
        return absoluteIndex;
    }
}
