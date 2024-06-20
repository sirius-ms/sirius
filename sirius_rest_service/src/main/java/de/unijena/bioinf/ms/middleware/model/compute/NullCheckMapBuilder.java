/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.compute;

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

    public NullCheckMapBuilder putIfNonNull(String key, String value) {
        return putIfNonNull(key, value, v -> v);
    }

    public <T> NullCheckMapBuilder putIfNonNull(String key, T value) {
        return putIfNonNull(key, value, String::valueOf);
    }

    public <T extends Enum<T>> NullCheckMapBuilder putIfNonNull(String key, T value) {
        return putIfNonNull(key, value, Enum::name);
    }

    public <T> NullCheckMapBuilder putIfNonNullObj(@NotNull String key,
                                                   @Nullable T value,
                                                   @NotNull Function<T, ?> valueConverter
    ) {
        return putIfNonNull(key, value, i -> String.valueOf(valueConverter.apply(i)));
    }


    public <T> NullCheckMapBuilder putIfNonNull(
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
