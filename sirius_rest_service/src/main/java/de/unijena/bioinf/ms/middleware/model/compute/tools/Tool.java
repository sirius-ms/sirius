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

package de.unijena.bioinf.ms.middleware.model.compute.tools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Tool<C> {
    //todo we can delegate command infos as separate files to provide all infos to clients e.g. SIRIUS GUI
    @JsonIgnore
    private final CommandLine.Command command;

    /**
     * tags whether the tool is enabled
     */
    private boolean enabled = true;

    /**
     * Call in empty constructor of superclass that does not call anything else to allow for Jackson compatibility.
     * Everything else should be done with builders.
     *
     * @param annotatedObject the corresponding commandline annotation.
     */
    protected Tool(Class<C> annotatedObject) {
        command = annotatedObject.getAnnotation(CommandLine.Command.class);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JsonIgnore
    public CommandLine.Command getCommand() {
        return command;
    }

    @JsonIgnore
    public abstract Map<String, String> asConfigMap();


    protected static class NullCheckMapBuilder {
        private Map<String, String> map;

        protected NullCheckMapBuilder() {
            this(new HashMap<>());
        }

        protected NullCheckMapBuilder(Map<String, String> map) {
            this.map = map;
        }

        protected NullCheckMapBuilder putNonNull(String key, String value) {
            return putNonNull(key, value, v -> v);
        }
        protected <T> NullCheckMapBuilder putNonNull(String key, T value) {
            return putNonNull(key, value, String::valueOf);
        }

        protected <T extends Enum<T>> NullCheckMapBuilder putNonNull(String key, T value) {
            return putNonNull(key, value, Enum::name);
        }

        protected <T> NullCheckMapBuilder putNonNullObj(@NotNull String key,
                                                        @Nullable T value,
                                                        @NotNull Function<T, ?> valueConverter
        ) {
            return putNonNull(key, value, i -> String.valueOf(valueConverter.apply(i)));
        }


        protected <T> NullCheckMapBuilder putNonNull(
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

        Map<String, String> toUnmodifiableMap(){
            return Collections.unmodifiableMap(toMap());
        }
        Map<String, String> toMap(){
            Map<String, String> map = this.map;
            this.map = null;
            return map;
        }
    }
}
