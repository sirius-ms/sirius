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

package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class IsotopicIntensitySettings implements Ms2ExperimentAnnotation {

    /**
     * Ignore isotope peaks below this intensity.
     * This value should reflect the smallest relative intensive which is still above noise level.
     * Obviously, this is hard to judge without having absolute values. Keeping this value around 1 percent is
     * fine for most settings. Set it to smaller values if you trust your small intensities.
     */
    @DefaultProperty(propertyParent = "ms1", propertyKey = "minimalIntensityToConsider")
    public final double minimalIntensityToConsider;

    /**
     * The average absolute deviation between theoretical and measured intensity of isotope peaks.
     * Do not change this parameter without a good reason!
     */
    @DefaultProperty(propertyParent = "ms1", propertyKey = "absoluteIntensityError")
    public final double absoluteIntensityError;

    /**
     * The average relative deviation between theoretical and measured intensity of isotope peaks.
     * Do not change this parameter without a good reason!
     */
    @DefaultProperty(propertyParent = "ms1", propertyKey = "relativeIntensityError")
    public final double relativeIntensityError;

    public IsotopicIntensitySettings(double minimalIntensityToConsider, double absoluteIntensityError, double relativeIntensityError) {
        this.minimalIntensityToConsider = minimalIntensityToConsider;
        this.absoluteIntensityError = absoluteIntensityError;
        this.relativeIntensityError = relativeIntensityError;
    }

    private IsotopicIntensitySettings() {
        this(Double.NaN,Double.NaN,Double.NaN);
    }

    public double getMinimalIntensityToConsider() {
        return minimalIntensityToConsider;
    }

    public double getAbsoluteIntensityError() {
        return absoluteIntensityError;
    }

}
