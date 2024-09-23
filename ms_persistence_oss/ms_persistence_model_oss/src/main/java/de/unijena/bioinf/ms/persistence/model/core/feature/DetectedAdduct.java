/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.persistence.model.core.feature;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@Getter
@Builder
@Jacksonized
@EqualsAndHashCode
public class DetectedAdduct implements Comparable<DetectedAdduct> {
    public static de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdduct empty(@NotNull de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source) {
       return new DetectedAdduct(null, Double.NaN, source);
    }

    public static de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdduct unambiguous(@NotNull de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source, @NotNull PrecursorIonType precursorIonType) {
        return new DetectedAdduct(precursorIonType, 1d, source);
    }


    @Nullable
    private final PrecursorIonType adduct;

    @EqualsAndHashCode.Exclude
    private double score = Double.NaN;

    @NotNull
    private DetectedAdducts.Source source = DetectedAdducts.Source.UNSPECIFIED_SOURCE;

    @Override
    public int compareTo(@NotNull DetectedAdduct o) {
        return Double.compare(score, o.getScore());
    }
}
