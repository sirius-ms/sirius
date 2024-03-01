/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.middleware.model.compute;

import de.unijena.bioinf.ms.middleware.model.compute.tools.Tool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NullCheckMapBuilder {
    private Map<String, String> map;

    public NullCheckMapBuilder() {
        this(new HashMap<>());
    }

    public NullCheckMapBuilder(Map<String, String> map) {
        this.map = map;
    }

    public NullCheckMapBuilder putNonNull(String key, String value) {
        return putNonNull(key, value, v -> v);
    }

    public <T> NullCheckMapBuilder putNonNull(String key, T value) {
        return putNonNull(key, value, String::valueOf);
    }

    public <T extends Enum<T>> NullCheckMapBuilder putNonNull(String key, T value) {
        return putNonNull(key, value, Enum::name);
    }

    public <T> NullCheckMapBuilder putNonNullObj(@NotNull String key,
                                                    @Nullable T value,
                                                    @NotNull Function<T, ?> valueConverter
    ) {
        return putNonNull(key, value, i -> String.valueOf(valueConverter.apply(i)));
    }


    public <T> NullCheckMapBuilder putNonNull(
            @NotNull String key,
            @Nullable T value,
            @NotNull Function<T, String> valueConverter
    ) {
        if (map == null)
            throw new IllegalStateException("Map has already been build, please create an new Builder");
        if (value != null)
            map.put(key, valueConverter.apply(value));
        return this;
    }

    public Map<String, String> toUnmodifiableMap() {
        return Collections.unmodifiableMap(toMap());
    }

    public Map<String, String> toMap() {
        Map<String, String> map = this.map;
        this.map = null;
        return map;
    }
}
