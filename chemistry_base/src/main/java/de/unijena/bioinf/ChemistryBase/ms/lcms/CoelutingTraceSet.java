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

import javax.annotation.Nonnull;

/**
 * Each compound is assigned to an CoelutingTraceset PER SAMPLE. The CoelutingTraceset is
 * the collection of all mass traces associated to the compound and its correlated ions (adducts,
 * isotopes, in-source fragments)
 */
public class CoelutingTraceSet {

    @Nonnull protected final String sampleName;
    @Nonnull protected final MsDataSourceReference sampleRef;
    @Nonnull protected final CompoundTrace ionTrace;

    @Nonnull  protected final long[] retentionTimes;
    @Nonnull  protected final int[] scanIds; // the INDEX of the spectrum
    @Nonnull protected final float[] noiseLevels;

    /**
     * the IDs of scans that are part of the merged MS/MS spectrum
     */
    @Nonnull protected final int[] ms2ScanIds;
    @Nonnull protected final long[] ms2RetentionTimes;

    @Nonnull protected final CompoundReport[] reports;


    public CoelutingTraceSet(@Nonnull String sampleName, @Nonnull MsDataSourceReference sampleRef, @Nonnull CompoundTrace trace, @Nonnull long[] retentionTimes, @Nonnull int[] scanIds, @Nonnull float[] noiselevels, int[] ms2Scans, long[] ms2RetentionTimes, CompoundReport[] reports) {
        this.sampleName = sampleName;
        this.sampleRef = sampleRef;
        this.ionTrace = trace;
        this.retentionTimes = retentionTimes;
        this.scanIds = scanIds;
        this.noiseLevels = noiselevels;
        this.ms2ScanIds = ms2Scans;
        this.ms2RetentionTimes = ms2RetentionTimes;
        this.reports = reports;
        if (getMs2RetentionTimes().length!=getMs2ScanIds().length || retentionTimes.length!=scanIds.length) {
            throw new IllegalArgumentException("different number of scan ids and retention times");
        }
    }

    @Nonnull
    public long[] getMs2RetentionTimes() {
        return ms2RetentionTimes;
    }

    @Nonnull
    public int[] getMs2ScanIds() {
        return ms2ScanIds;
    }

    @Nonnull
    public String getSampleName() {
        return sampleName;
    }

    @Nonnull
    public MsDataSourceReference getSampleRef() {
        return sampleRef;
    }

    @Nonnull
    public CompoundTrace getIonTrace() {
        return ionTrace;
    }

    @Nonnull
    public long[] getRetentionTimes() {
        return retentionTimes;
    }

    @Nonnull
    public int[] getScanIds() {
        return scanIds;
    }

    @Nonnull
    public float[] getNoiseLevels() {
        return noiseLevels;
    }

    @Nonnull
    public CompoundReport[] getReports() {
        return reports;
    }
}
