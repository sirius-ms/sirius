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

import com.fasterxml.jackson.annotation.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * Each compound is assigned to an CoelutingTraceset PER SAMPLE. The CoelutingTraceset is
 * the collection of all mass traces associated to the compound and its correlated ions (adducts,
 * isotopes, in-source fragments)
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CoelutingTraceSet {

    @JsonIgnore @NotNull protected final String sampleName;
    @JsonIgnore @NotNull protected final MsDataSourceReference sampleRef;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) @NotNull protected final CompoundTrace ionTrace;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) @NotNull protected final long[] retentionTimes;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) @NotNull protected final int[] scanIds; // the INDEX of the spectrum
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) @NotNull protected final float[] noiseLevels;

    /**
     * the IDs of scans that are part of the merged MS/MS spectrum
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) @NotNull protected final int[] ms2ScanIds;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) @NotNull protected final long[] ms2RetentionTimes;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) @NotNull protected final CompoundReport[] reports;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CoelutingTraceSet(@JsonProperty(value="ionTrace", access = JsonProperty.Access.WRITE_ONLY) CompoundTrace trace, @JsonProperty(value="retentionTimes", access = JsonProperty.Access.WRITE_ONLY) long[] retentionTimes, @JsonProperty(value="scanIds", access = JsonProperty.Access.WRITE_ONLY) int[] scanIds, @JsonProperty(value="noiseLevels", access = JsonProperty.Access.WRITE_ONLY) float[] noiselevels, @JsonProperty(value="ms2ScanIds", access = JsonProperty.Access.WRITE_ONLY) int[] ms2Scans, @JsonProperty(value="ms2RetentionTimes", access = JsonProperty.Access.WRITE_ONLY) long[] ms2RetentionTimes/*, @JsonProperty(value="reports", access = JsonProperty.Access.WRITE_ONLY) CompoundReport[] reports*/) {
        this.sampleName = "";
        this.sampleRef = new MsDataSourceReference((URI)null,null,null,null);
        this.ionTrace = trace;
        this.retentionTimes = retentionTimes==null ? new long[0] : retentionTimes;
        this.scanIds = scanIds==null ? new int[0] : scanIds;
        this.noiseLevels = noiselevels==null ? new float[0] : noiselevels;
        this.ms2ScanIds = ms2Scans==null ? new int[0] : ms2Scans;
        this.ms2RetentionTimes = ms2RetentionTimes==null ? new long[0] : ms2RetentionTimes;
        this.reports = null;//reports==null ? new CompoundReport[0] : reports;
    }


    public CoelutingTraceSet(@NotNull String sampleName, @NotNull MsDataSourceReference sampleRef, @NotNull CompoundTrace trace, @NotNull long[] retentionTimes, @NotNull int[] scanIds, @NotNull float[] noiselevels, int[] ms2Scans, long[] ms2RetentionTimes, CompoundReport[] reports) {
        this.sampleName = sampleName;
        this.sampleRef = sampleRef;
        this.ionTrace = trace;
        this.retentionTimes = retentionTimes;
        this.scanIds = scanIds;
        this.noiseLevels = noiselevels;
        this.ms2ScanIds = ms2Scans;
        this.ms2RetentionTimes = ms2RetentionTimes;
        this.reports = reports;
        if (ms2RetentionTimes.length!=ms2ScanIds.length || retentionTimes.length!=scanIds.length) {
            throw new IllegalArgumentException("different number of scan ids and retention times");
        }
    }

    @NotNull
    public long[] getMs2RetentionTimes() {
        return ms2RetentionTimes;
    }

    @NotNull
    public int[] getMs2ScanIds() {
        return ms2ScanIds;
    }

    @NotNull
    public String getSampleName() {
        return sampleName;
    }

    @NotNull
    public MsDataSourceReference getSampleRef() {
        return sampleRef;
    }

    @NotNull
    public CompoundTrace getIonTrace() {
        return ionTrace;
    }

    @NotNull
    public long[] getRetentionTimes() {
        return retentionTimes;
    }

    @NotNull
    public int[] getScanIds() {
        return scanIds;
    }

    @NotNull
    public float[] getNoiseLevels() {
        return noiseLevels;
    }

    @NotNull
    public CompoundReport[] getReports() {
        return reports;
    }
}
