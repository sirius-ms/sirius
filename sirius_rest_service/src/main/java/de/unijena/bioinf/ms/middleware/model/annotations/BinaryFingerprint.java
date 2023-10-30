/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.annotations;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BinaryFingerprint {
    /**
     * Array that contains all indices of bits that are set (are 1)
     */
    short[] bitsSet;
    /**
     * Size of the fingerprint, e.g. to reconstruct the binary array from the array of set bits
     */
    int length;


    //todo move to service layer
    public static BinaryFingerprint from (Fingerprint fingerprint){
        BinaryFingerprint fp = new BinaryFingerprint();
        fp.setLength(fingerprint.cardinality());
        fp.setBitsSet(fingerprint.toIndizesArray());
        return fp;
    }
}
