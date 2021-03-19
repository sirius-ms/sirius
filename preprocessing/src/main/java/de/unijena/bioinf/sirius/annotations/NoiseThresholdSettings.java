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

package de.unijena.bioinf.sirius.annotations;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class NoiseThresholdSettings implements Ms2ExperimentAnnotation  {

    public static enum BASE_PEAK {
        LARGEST,
        NOT_PRECURSOR,
        SECOND_LARGEST
    };

    @DefaultProperty public final double intensityThreshold;
    @DefaultProperty public final int maximalNumberOfPeaks;
    @DefaultProperty public final BASE_PEAK basePeak;
    @DefaultProperty public final double absoluteThreshold;

    public NoiseThresholdSettings(double intensityThreshold, int maximalNumberOfPeaks, BASE_PEAK basePeak, double absoluteThreshold) {
        this.intensityThreshold = intensityThreshold;
        this.maximalNumberOfPeaks = maximalNumberOfPeaks;
        this.basePeak = basePeak;
        this.absoluteThreshold = absoluteThreshold;
    }

    NoiseThresholdSettings() {
        this(0,0,BASE_PEAK.NOT_PRECURSOR,0d);
    }

    public NoiseThresholdSettings withPeakLimit(int maximalNumberOfPeaks) {
        return new NoiseThresholdSettings(intensityThreshold,maximalNumberOfPeaks,basePeak,absoluteThreshold);
    }
    public NoiseThresholdSettings withAbsoluteThreshold(int absoluteThreshold) {
        return new NoiseThresholdSettings(intensityThreshold,maximalNumberOfPeaks,basePeak,absoluteThreshold);
    }
    public NoiseThresholdSettings withIntensityThreshold(int intensityThreshold) {
        return new NoiseThresholdSettings(intensityThreshold,maximalNumberOfPeaks,basePeak,intensityThreshold);
    }
    public NoiseThresholdSettings withBasePeak(BASE_PEAK basePeak) {
        return new NoiseThresholdSettings(intensityThreshold,maximalNumberOfPeaks,basePeak,intensityThreshold);
    }
}
