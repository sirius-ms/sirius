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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;


public final class Trace {
    /**
     * The offset between the first array element and the first index of the trace set
     */
    @JsonProperty private final int indexOffset;

    /**
     * the apex for fast access
     */
    @JsonIgnore private final int apex;

    /**
     * the index of the array element which is the first peak in the detected feature
     * (the peaks before it are background)
     */
    @JsonProperty private final int detectedFeatureOffset;
    @JsonProperty private final int detectedFeatureLength;
    @JsonProperty @Nonnull private final double[] masses;
    @JsonProperty @Nonnull private final float[] intensities;

    @JsonCreator
    public Trace(@JsonProperty("indexOffset") int offset, @JsonProperty("detectedFeatureOffset") int detectorOffset, @JsonProperty("detectedFeatureLength")  int detectedLength, @JsonProperty("masses")  double[] masses, @JsonProperty("intensities") float[] intensities) {
        if (masses==null || intensities==null) {
            throw new NullPointerException();
        }
        if (masses.length!=intensities.length) {
            throw new IllegalArgumentException("array sizes differ: " + masses.length + " vs. " + intensities.length);
        }
        this.indexOffset = offset;
        this.detectedFeatureOffset = detectorOffset;
        this.detectedFeatureLength = detectedLength;
        this.masses = masses;
        this.intensities = intensities;
        // find apex
        {
            int apexIndex=0;
            double maxI = Double.NEGATIVE_INFINITY;
            for (int i=detectedFeatureOffset, n= detectorOffset+detectedLength; i<n; ++i) {
                if (intensities[i] > maxI) {
                    maxI = intensities[i];
                    apexIndex =i;
                }
            }
            this.apex = apexIndex;
        }

    }

    @JsonIgnore public double getApexMass() {
        return masses[apex];
    }
    @JsonIgnore public float getApexIntensity() {
        return intensities[apex];
    }
    @JsonIgnore public int getAbsoluteIndexApex() {
        return apex + indexOffset;
    }

    @JsonIgnore public float getLeftEdgeIntensity() {
        return intensities[detectedFeatureOffset];
    }
    @JsonIgnore public float getRightEdgeIntensity() {
        return intensities[detectedFeatureOffset+detectedFeatureLength-1];
    }

    @JsonIgnore public int getIndexOffset() {
        return indexOffset;
    }

    @JsonIgnore public int getDetectedFeatureOffset() {
        return detectedFeatureOffset;
    }

    @JsonIgnore public int absoluteIndexLeft() {
        return indexOffset+detectedFeatureOffset;
    }

    @JsonIgnore public int absoluteIndexRight() {
        return absoluteIndexLeft()+detectedFeatureLength-1;
    }

    @JsonIgnore public int getDetectedFeatureLength() {
        return detectedFeatureLength;
    }

    @Nonnull @JsonIgnore
    public double[] getMasses() {
        return masses;
    }

    @Nonnull @JsonIgnore
    public float[] getIntensities() {
        return intensities;
    }
}
