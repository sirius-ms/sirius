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
import io.swagger.v3.oas.annotations.media.Schema;

public class ZodiacLibraryScoring implements Ms2ExperimentAnnotation {

    /**
     * Specifies whether adding library search based anchors is used or not.
     */
    @DefaultProperty public final boolean enabled;


    /**
     * Lambda used in the scoring function of spectral library hits. The higher this value the higher are library hits weighted in ZODIAC scoring.
     */
    @Schema(hidden = true)
    @DefaultProperty public final double lambda; //1000

    /**
     * Spectral library hits must have at least this cosine or higher to be considered in scoring. Value must be in [0,1].
     */
    @DefaultProperty public final double minSimilarity; //0.5

    public ZodiacLibraryScoring() {
        lambda = Double.NaN;
        minSimilarity = Double.NaN;
        enabled = true;
    }

    public ZodiacLibraryScoring(double lambda, double minSimilarity, boolean enabled) {
        this.lambda = lambda;
        this.minSimilarity = minSimilarity;
        this.enabled = enabled;
    }
}
