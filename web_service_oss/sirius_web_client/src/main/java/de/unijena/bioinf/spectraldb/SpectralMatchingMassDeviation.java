/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class SpectralMatchingMassDeviation implements Ms2ExperimentAnnotation {
    @Getter
    public final Deviation allowedPeakDeviation;
    @Getter
    public final Deviation allowedPrecursorDeviation;


    /**
     *
     * @param allowedPeakDeviation Maximum allowed mass deviation in ppm for matching peaks.
     * @param allowedPrecursorDeviation Maximum allowed mass deviation in ppm for matching the precursor.
     */
    @DefaultInstanceProvider
    public static SpectralMatchingMassDeviation newInstance(@DefaultProperty(propertyKey = "allowedPeakDeviation") Deviation allowedPeakDeviation,
                                                            @DefaultProperty(propertyKey = "allowedPrecursorDeviation") Deviation allowedPrecursorDeviation) {
        return new SpectralMatchingMassDeviation(allowedPeakDeviation, allowedPrecursorDeviation);
    }

    public SpectralMatchingMassDeviation withAllowedPeakDeviation(Deviation allowedPeakDeviation) {
        return new SpectralMatchingMassDeviation(allowedPeakDeviation, allowedPrecursorDeviation);
    }

    public SpectralMatchingMassDeviation withAllowedPrecursorDeviation(Deviation allowedPrecursorDeviation) {
        return new SpectralMatchingMassDeviation(allowedPeakDeviation, allowedPrecursorDeviation);
    }
}
