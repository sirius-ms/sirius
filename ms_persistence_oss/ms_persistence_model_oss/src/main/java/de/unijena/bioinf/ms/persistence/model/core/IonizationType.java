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

public enum IonizationType {

    ESI("Electrospray ionization", "esi|electrospray", true, "MS:1000073"),
    CI("Chemical ionization", "ci|chemical", true, "MS:1000071"),
    SICRIT("Soft Ionization by Chemical Reaction in Transfer", "sicrit", true),
    MALDI("Matrix-assisted laser desorption ionization", "maldi", true, "MS:1000075"),

    FAB("Fast atom bombardment", "fab|atom[-\\s]+bombardment", true, "MS:1000074"),

    FD("Field desorption ionization", "fd|field[-\\s]+desorption", true, "MS:1000257"),

    SIMS("Secondary ion mass spectrometry", "sims|secondary[-\\s]+ion", true),

    PD("Plasma desorption", "pd|plasma[-\\s]+desorption", true, "MS:1000134"),

    LD("Laser desorption", "ld|laser[-\\s]+desorption", true, "MS:1000266"),

    TSI("Thermal spray", "tsi|thermal[-\\s]+spray", true),

    EI("Electron impact", "ei|electron[-\\s]+impact", false, "MS:1000072"),
    ICP("Inductively Coupled Plasma", "icp|inductively[-\\s]+coupled", false);

    public static Optional<IonizationType> byHupoId(String hupoId) {
        return Arrays.stream(IonizationType.values()).filter(type -> type.hupoIds.stream().anyMatch(id -> id.equalsIgnoreCase(hupoId))).findFirst();
    }

    public static Optional<IonizationType> byValue(String value) {
        return Arrays.stream(IonizationType.values()).map(
                type -> Pair.of(type, type.pattern.matcher(value).results().map(mr -> mr.end() - mr.start()).max(Integer::compare).orElse(0))
        ).max(Comparator.comparing(Pair::right)).map(best -> best.right() > 0 ? best.left() : null);
    }

    @NotNull
    @Getter
    private final String fullName;

    @NotNull
    private final Pattern pattern;

    @NotNull
    private final List<String> hupoIds;

    @Getter
    private final boolean soft;

    IonizationType(@NotNull String fullName, @NotNull String pattern, boolean soft, String... hupoIds) {
        this.fullName = fullName;
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        this.soft = soft;
        this.hupoIds = List.of(hupoIds);
    }

}
