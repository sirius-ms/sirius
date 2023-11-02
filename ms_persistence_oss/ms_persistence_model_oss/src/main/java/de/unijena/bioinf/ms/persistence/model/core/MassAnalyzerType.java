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

package de.unijena.bioinf.ms.persistence.model.core;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public enum MassAnalyzerType {
    //todo fill me. should be configurable? Maybe exact instrument instead of analyzer?
    TOF("Time of Flight"),
    IONTRAP("Ion Trap"),
    ORBITRAP("Orbitrap"),
    ICR("Ion Cyclotron Resonance"),
    FTICR("Fourier Transform Ion Cyclotron Resonance"),
    QUAD("Quadropol");





    @NotNull
    public final String fullName;
    @NotNull
    public final List<String> hupoIds;


    MassAnalyzerType(@NotNull String fullName, String... hupoIds) {
        this.fullName = fullName;
        this.hupoIds = List.of(hupoIds);
    }
    MassAnalyzerType(@NotNull String fullName, List<String> hupoIds) {
        this.fullName = fullName;
        this.hupoIds = Collections.unmodifiableList(hupoIds);
    }

    public String fullName() {
        return fullName;
    }

    public List<String> hupoIds() {
        return hupoIds;
    }

}
