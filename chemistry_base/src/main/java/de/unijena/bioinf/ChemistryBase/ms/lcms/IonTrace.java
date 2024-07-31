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

package de.unijena.bioinf.ChemistryBase.ms.lcms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;

/**
 * A mass trace of an ion consists of the mass traces of its isotope peaks
 */
public class IonTrace {

    /**
     * traces of the isotopes. The monoisotopic peak has index 0.
     */
    @JsonProperty
    @Nonnull
    protected final Trace[] isotopes;

    @JsonProperty
    @Nonnull protected final double[] correlationScores;

    public IonTrace(@JsonProperty("isotopes") Trace[] isotopes, @JsonProperty("correlationScores") double[] correlationScores) {
        this.isotopes = isotopes==null ? new Trace[0] : isotopes;
        this.correlationScores = correlationScores==null ? new double[0] : correlationScores;
    }

    @JsonIgnore public Trace getMonoisotopicPeak() {
        return this.isotopes[0];
    }

    @Nonnull
    public Trace[] getIsotopes() {
        return isotopes;
    }

    @Nonnull
    public double[] getCorrelationScores() {
        return correlationScores;
    }
}
