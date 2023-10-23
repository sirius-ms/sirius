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

public enum IonizationType {

    ESI("Electrospray ionization", true),
    CI("Chemical ionization", true),
    SICRIT("Soft Ionization by Chemical Reaction in Transfer", true),
    MALDI("Matrix-assisted laser desorption ionization", true),

    FAB("Fast atom bombardment", true),

    FD("Field desorption ionization", true),

    SIMS("Secondary ion mass spectrometry", true),

    PD("Plasma desorption", true),

    LD("Laser desorption", true),

    TSI("Thermal spray", true),

    EI("Electron impact", false),
    ICP("Inductively Coupled Plasma", false);


    @NotNull
    public final String fullName;
    @NotNull
    public final List<String> hupoIds;

    public final boolean soft;

    IonizationType(@NotNull String fullName, boolean soft, String... hupoIds) {
        this.fullName = fullName;
        this.soft = soft;
        this.hupoIds = List.of(hupoIds);
    }
    IonizationType(@NotNull String fullName, boolean soft, List<String> hupoIds) {
        this.fullName = fullName;
        this.soft = soft;
        this.hupoIds = Collections.unmodifiableList(hupoIds);
    }

    public String fullName() {
        return fullName;
    }

    public List<String> hupoIds() {
        return hupoIds;
    }

    public boolean isSoft() {
        return soft;
    }

    public String getType(){
        return isSoft()? "SOFT" : "HARD";
    }
}
