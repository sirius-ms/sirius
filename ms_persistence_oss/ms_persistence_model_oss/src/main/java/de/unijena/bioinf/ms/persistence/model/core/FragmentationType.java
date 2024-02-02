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

import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public enum FragmentationType {
    CID("Collision-induced dissociation", "cid|collision[-\\s]+induced", "MS:1000133"),
    HCD("Higher-energy C-trap dissociation", "hcd|higher[-\\s]+energy|c[-\\s]*trap"),
    SID("Surface-induced dissociation", "sid|surface[-\\s]+induced", "MS:1000136"),
    ECD("Electron capture dissociation", "ecd|electron[-\\s]+capture", "MS:1000250"),
    ETD("Electron-transfer dissociation", "etd|electron[-\\s]+transfer", "MS:1000598"),
    NETD("Negative electron-transfer dissociation", "netd|negative[-\\s]+electron[-\\s]+transfer", "MS:1003247"),
    EDD("Electron-detachment dissociation", "edd|electron[-\\s]+detachment"),
    ION_SOURCE("In-source fragmentation", "ion[-\\s]*source|in[-\\s]*source","MS:1001880");


    @NotNull
    @Getter
    private final String fullName;

    @NotNull
    private final Pattern pattern;

    @NotNull
    private final List<String> hupoIds;


    public static Optional<FragmentationType> byHupoId(String hupoId) {
        return Arrays.stream(FragmentationType.values()).filter(type -> type.hupoIds.stream().anyMatch(id -> id.equalsIgnoreCase(hupoId))).findFirst();
    }

    public static Optional<FragmentationType> byValue(String value) {
        return Arrays.stream(FragmentationType.values()).map(
                type -> Pair.of(type, type.pattern.matcher(value).results().map(mr -> mr.end() - mr.start()).max(Integer::compare).orElse(0))
        ).max(Comparator.comparing(Pair::right)).map(best -> best.right() > 0 ? best.left() : null);
    }


    FragmentationType(@NotNull String fullName, @NotNull String pattern, String... hupoIds) {
        this.fullName = fullName;
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        this.hupoIds = List.of(hupoIds);
    }

}
