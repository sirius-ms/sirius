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
 * Ratio of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are forced for each ionization to be considered by ZODIAC.
 * This depends on the number of candidates ZODIAC considers. E.g. if 50 candidates are considered and a ratio of 0.2 is set, at least 10 candidates per ionization will be considered, which might increase the number of candidates above 50.
 */
@DefaultProperty
public class ZodiacRatioOfConsideredCandidatesPerIonization implements Ms2ExperimentAnnotation {
    public final double value;

    public ZodiacRatioOfConsideredCandidatesPerIonization() {
        this.value = 0.2;
    }

    public ZodiacRatioOfConsideredCandidatesPerIonization(double ratioOfCandidatesForcedPerAdduct) {
        this.value = ratioOfCandidatesForcedPerAdduct;
    }
}
