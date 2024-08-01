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

package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC.
 * This is the threshold used for all compounds with mz below 300 m/z and is used to interpolate the number of candidates for larger compounds.
 * If lower than 0, all available candidates are considered.
 */
@DefaultProperty
public class ZodiacNumberOfConsideredCandidatesAt300Mz implements Ms2ExperimentAnnotation {
    public final int value;

    public ZodiacNumberOfConsideredCandidatesAt300Mz() {
        this.value = 10;
    }

    public ZodiacNumberOfConsideredCandidatesAt300Mz(int numberOfCandidates) {
        this.value = numberOfCandidates;
    }
}
