/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker, Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms.lcms.info;

public class RejectedMsMsDueToLowCosine extends RejectedMsMs {

    static final long serialVersionUID = 8944410858575762915L;

    private final float cosine;
    private final short numberOfCommonPeaks;

    public RejectedMsMsDueToLowCosine(long retentionTime, int scanId, float cosine, short numberOfCommonPeaks) {
        super(retentionTime, scanId);
        this.cosine = cosine;
        this.numberOfCommonPeaks = numberOfCommonPeaks;
    }

    public float getCosine() {
        return cosine;
    }

    public short getNumberOfCommonPeaks() {
        return numberOfCommonPeaks;
    }

    @Override
    public String toString() {
        return super.toString() +  " due to low cosine (" + (cosine*100) + " %, " + numberOfCommonPeaks+ " shared peaks).";
    }
}
