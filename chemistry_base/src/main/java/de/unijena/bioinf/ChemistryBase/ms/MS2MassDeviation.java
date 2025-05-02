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

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;


/**
 * Mass accuracy setting for MS2 spectra. Mass Accuracies are always written as "X ppm (Y Da)" with X and Y are numerical values.
 * The ppm is a relative measure (parts per million), Da is an absolute measure. For each mass, the maximum of relative and absolute is used.
 */
public class MS2MassDeviation extends MassDeviation {

    /**
     * @param allowedMassDeviation maximum allowed mass accuracy. Only molecular formulas within this mass window are considered.
     * @param standardMassDeviation expected mass accuracy of the instrument. Is used for the scoring.
     * @param massDifferenceDeviation expected mass accuracy of the instrument for two close peaks or for recalibrated spectra. Should be smaller than the standard mass deviation. This value is only used for isotope pattern analysis in MS/MS
     */
    public MS2MassDeviation(@NotNull Deviation allowedMassDeviation, @NotNull Deviation standardMassDeviation, @NotNull Deviation massDifferenceDeviation) {
        super(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }

    @DefaultInstanceProvider
    public static MS2MassDeviation newInstance(@DefaultProperty(propertyKey = "allowedMassDeviation") Deviation allowedMassDeviation, @DefaultProperty(propertyKey = "standardMassDeviation" ) Deviation standardMassDeviation) {
        return new MS2MassDeviation(allowedMassDeviation, standardMassDeviation, Deviation.NULL_DEVIATION);
    }

    @Override
    public MS2MassDeviation withAllowedMassDeviation(Deviation allowedMassDeviation) {
        return new MS2MassDeviation(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }

    @Override
    public MS2MassDeviation withStandardMassDeviation(Deviation standardMassDeviation) {
        return new MS2MassDeviation(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }

    @Override
    public MS2MassDeviation withMassDifferenceDeviation(Deviation massDifferenceDeviation) {
        return new MS2MassDeviation(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }
}
