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

/**
 * A chromatographic peak that is splitted in halve because
 * the MS/MS scans on the right side have high dissimilarity to
 * the MS/MS on the left side
 */
public class SplittedSegmentDueToLowCosine extends SplittedSegment {

    static final long serialVersionUID = 2139576800756180792L;


    private final float bestCosine;
    private final short bestNumberOfCommonPeaks;
    private final long[] retentionTimes;
    private final int[] scanIds;

    public SplittedSegmentDueToLowCosine(long retentionTime, float bestCosine, short bestNumberOfCommonPeaks, long[] retentionTimes, int[] scanIds) {
        super(retentionTime);
        this.bestCosine = bestCosine;
        this.bestNumberOfCommonPeaks = bestNumberOfCommonPeaks;
        this.retentionTimes = retentionTimes;
        this.scanIds = scanIds;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public float getBestCosine() {
        return bestCosine;
    }

    public short getBestNumberOfCommonPeaks() {
        return bestNumberOfCommonPeaks;
    }

    public long[] getRetentionTimes() {
        return retentionTimes;
    }

    public int[] getScanIds() {
        return scanIds;
    }



    @Override
    public String toString() {
        return "Segment splitted due to low cosine of " + (bestCosine*100) + " %, " + getBestNumberOfCommonPeaks() + " shared peaks.";
    }
}
