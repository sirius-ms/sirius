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

package de.unijena.bioinf.fingerid.annotations;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Specifies if the list of Molecular Formula Identifications is filtered by a soft threshold
 * (calculateThreshold) before CSI:FingerID predictions are calculated.
 */

public class FormulaResultThreshold implements Ms2ExperimentAnnotation {
    //could be used as second property to specify different threshold types in the future
    private ThresholdCalculator thresholdCalculator = (topHitScore) -> Math.max(topHitScore, 0) - Math.max(5, topHitScore * 0.15);
    public final boolean value;


    public FormulaResultThreshold(boolean value) {
        this.value = value;
    }

    @DefaultInstanceProvider
    public static FormulaResultThreshold newInstance(@DefaultProperty boolean value) {
        return new FormulaResultThreshold(value);
    }

    public double calculateThreshold(double topHitScore) {
        return thresholdCalculator.calculateThreshold(topHitScore);
    }

    public void setThresholdCalculator(@NotNull ThresholdCalculator thresholdCalculator) {
        this.thresholdCalculator = thresholdCalculator;
    }

    public boolean useThreshold() {
        return value;
    }

    @FunctionalInterface
    public interface ThresholdCalculator {
        double calculateThreshold(double topHitScore);
    }
}
