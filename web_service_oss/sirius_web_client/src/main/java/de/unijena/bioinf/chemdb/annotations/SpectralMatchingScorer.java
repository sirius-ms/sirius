/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb.annotations;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import org.jetbrains.annotations.Nullable;

public class SpectralMatchingScorer implements Ms2ExperimentAnnotation {

    public final SpectralMatchingType spectralMatchingType;

    public SpectralMatchingScorer(SpectralMatchingType spectralMatchingType) {
        this.spectralMatchingType = spectralMatchingType;
    }

    @DefaultInstanceProvider
    public static SpectralMatchingScorer fromString(@DefaultProperty @Nullable String value) {
        return new SpectralMatchingScorer(SpectralMatchingType.valueOf(value));
    }

    @Override
    public String toString() {
        return spectralMatchingType.toString();
    }
}
