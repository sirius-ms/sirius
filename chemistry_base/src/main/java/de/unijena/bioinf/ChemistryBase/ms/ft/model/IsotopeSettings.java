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

package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * This configurations define how to deal with isotope patterns in MS1.
 */
public class IsotopeSettings implements Ms2ExperimentAnnotation {

    /**
     * When filtering is enabled, molecular formulas are excluded if their theoretical isotope pattern does not match
     * the theoretical one, even if their MS/MS pattern has high score.
     */
    @DefaultProperty
    protected final boolean filter;

    /**
     * multiplier for the isotope score. Set to 0 to disable isotope scoring. Otherwise, the score from isotope
     * pattern analysis is multiplied with this coefficient. Set to a value larger than one if your isotope
     * pattern data is of much better quality than your MS/MS data.
     */
    @DefaultProperty
    protected final double multiplier;

    private IsotopeSettings() {
        this.filter = true;
        this.multiplier = 1d;
    }

    public IsotopeSettings(boolean filter, double multiplicator) {
        if (multiplicator < 0 || !Double.isFinite(multiplicator))
            throw new IllegalArgumentException(String.valueOf(multiplicator) + " is invalid. Multiplicator has to be a positive (or zero) numerical value");
        this.filter = filter;
        this.multiplier = multiplicator;
    }

    public boolean isFiltering() {
        return filter;
    }

    public boolean isScoring() {
        return multiplier > 0d;
    }

    public double getMultiplier() {
        return multiplier;
    }

    /**
     * @return true if isotope pattern analysis is enablec
     */
    public boolean isEnabled() {
        return filter || multiplier>0;
    }
}
