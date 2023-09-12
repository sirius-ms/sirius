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

public enum FragmentationType {
    CID("Collision-induced dissociation"),
    HCD("Higher-energy C-trap dissociation "),
    SID("Surface-induced dissociation"),
    ECD("Electron capture dissociation"),
    ETD("Electron-transfer dissociation"),
    NETD("Negative electron-transfer dissociation"),
    EDD("Electron-detachment dissociation"),
    ION_SOURCE("In-source fragmentation"),
    NONE("No fragmentation");


    @NotNull
    public final String fullName;
    @NotNull
    public final List<String> hupoIds;


    FragmentationType(@NotNull String fullName, String... hupoIds) {
        this.fullName = fullName;
        this.hupoIds = List.of(hupoIds);
    }
    FragmentationType(@NotNull String fullName, List<String> hupoIds) {
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
