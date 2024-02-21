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


import de.unijena.bioinf.ms.properties.DefaultProperty;
import lombok.Getter;

/**
 * Specify settings to inject/preserver formula candidates that belong to
 * high scoring reference spectra.
 */
public class InjectHighSpectraMatchFormulas {

    /**
     * Similarity Threshold to inject formula candidates no matter which score they have or which filter is applied.
     */
    @DefaultProperty
    @Getter
    private final double minScoreToInject;

    /**
     * If true formulas candidates with reference spectrum similarity > minScoreToEnforce will be part of the result
     * list no matter of other filter settings or there rank regarding SIRIUS score.
     */
    @DefaultProperty
    @Getter
    private final boolean injectFormulas;

    /**
     * If true Fingerprint/Classes/Structures will be predicted for formulas candidates with
     * reference spectrum similarity > minScoreToEnforce will be predicted no matter which soft threshold rules
     * will apply.
     */
    @DefaultProperty
    @Getter
    private final boolean alwaysPredict;

    private InjectHighSpectraMatchFormulas(double minScoreToInject, boolean injectFormulas, boolean alwaysPredict) {
        this.minScoreToInject = minScoreToInject;
        this.injectFormulas = injectFormulas;
        this.alwaysPredict = alwaysPredict;
    }
}
