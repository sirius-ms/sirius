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

public final class Trace {
    /**
     * The offset between the first array element and the first index of the trace set
     */
    private final int indexOffset;

    /**
     * the apex for fast access
     */
    private final int apex;

    /**
     * the index of the array element which is the first peak in the detected feature
     * (the peaks before it are background)
     */
    private final int detectedFeatureOffset;
    private final int detectedFeatureLength;
    @Nonnull private final double[] masses;
    @Nonnull private final float[] intensities;

    public Trace(int offset, int detectorOffset, int detectedLength, double[] masses, float[] intensities) {
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

    public double getApexMass() {
        return masses[apex];
    }
    public float getApexIntensity() {
        return intensities[apex];
    }
    public int getAbsoluteIndexApex() {
        return apex + indexOffset;
    }

    public float getLeftEdgeIntensity() {
        return intensities[detectedFeatureOffset];
    }
    public float getRightEdgeIntensity() {
        return intensities[detectedFeatureOffset+detectedFeatureLength-1];
    }

    public int getIndexOffset() {
        return indexOffset;
    }

    public int getDetectedFeatureOffset() {
        return detectedFeatureOffset;
    }

    public int absoluteIndexLeft() {
        return indexOffset+detectedFeatureOffset;
    }

    public int absoluteIndexRight() {
        return absoluteIndexLeft()+detectedFeatureLength-1;
    }

    public int getDetectedFeatureLength() {
        return detectedFeatureLength;
    }

    @Nonnull
    public double[] getMasses() {
        return masses;
    }

    @Nonnull
    public float[] getIntensities() {
        return intensities;
    }
}
