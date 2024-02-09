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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class MassAnalyzer {

    public enum Type {

        TOF("Time of Flight", "tof|time[-\\s]+of[-\\s]+flight", "MS:1000084"),
        IONTRAP("Ion Trap", "ion[-\\s]*trap|trap|lcq|qqit|qqlit|it|lit","MS:1000264"),
        ORBITRAP("Orbitrap", "orbi|(?:q-)?exactive|velos|lumos", "MS:1000484"),
        FTICR("Fourier Transform Ion Cyclotron Resonance", "ft[-\\s]*icr|hybrid[-\\s]*ft|ltq[-\\s]*fticr|ft", "MS:1000079"),
        QUAD("Quadropole", "quad|q[-\\s]*tof|maxis|impact", "MS:1000082");

        @NotNull
        @Getter
        private final String fullName;

        @NotNull
        private final Pattern pattern;

        @NotNull
        private final List<String> hupoIds;


        Type(@NotNull String fullName, @NotNull String pattern, String... hupoIds) {
            this.fullName = fullName;
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.hupoIds = List.of(hupoIds);
        }

    }

    public static Optional<MassAnalyzer> byHupoId(String hupoId) {
        return Arrays.stream(Type.values()).filter(type -> type.hupoIds.stream().anyMatch(id -> id.equalsIgnoreCase(hupoId))).findFirst().map(type -> new MassAnalyzer(null, type));
    }

    public static Optional<MassAnalyzer> byValue(String value) {
        return Arrays.stream(Type.values()).map(
                type -> Pair.of(type, type.pattern.matcher(value).results().map(mr -> mr.end() - mr.start()).max(Integer::compare).orElse(0))
        ).max(Comparator.comparing(Pair::right)).map(best -> best.right() > 0 ? new MassAnalyzer(value, best.left()) : null);
    }

    @Nullable private String userSuppliedName;

    @NotNull private Type type;

}
