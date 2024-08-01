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
 * A chromatographic peak with two Maxima, that is splitted into halve.
 * The MS/MS in both sides are very similar, but the distance between the maxima
 * is very large, so probably those are similar compounds or stereo isomers.
 */
public class SimilarMsMsButLargeGap extends SplittedSegment {

    private float cosineSimilarity;
    private short numberOfCommonPeaks;
    private int[] scanIds;
    private long[] retentionTimes;
    private long retentionTimeGap;

    static final long serialVersionUID = 4234378784115374145L;

    public SimilarMsMsButLargeGap(long split, float cosineSimilarity, short numberOfCommonPeaks, long[] retentionTimes, int[] scanIds, long retentionTimeGap) {
        super(split);
        this.cosineSimilarity = cosineSimilarity;
        this.numberOfCommonPeaks = numberOfCommonPeaks;
        this.scanIds = scanIds;
        this.retentionTimes = retentionTimes;
        this.retentionTimeGap = retentionTimeGap;
    }

    public float getCosineSimilarity() {
        return cosineSimilarity;
    }

    public short getNumberOfCommonPeaks() {
        return numberOfCommonPeaks;
    }

    public int[] getScanIds() {
        return scanIds;
    }

    public long[] getRetentionTimes() {
        return retentionTimes;
    }

    public long getRetentionTimeGap() {
        return retentionTimeGap;
    }

    @Override
    public String toString() {
        return "Segment splitted due to large gap of " + retentionTimeGap+ " ms.";
    }
}
