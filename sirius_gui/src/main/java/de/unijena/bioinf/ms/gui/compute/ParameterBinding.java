/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.compute;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class ParameterBinding extends HashMap<String, Supplier<String>> {

    public ParameterBinding(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ParameterBinding(int initialCapacity) {
        super(initialCapacity);
    }

    public ParameterBinding() {
        super();
    }

    public Optional<String> getOpt(@NotNull String key) {
        return Optional.ofNullable(get(key)).map(Supplier::get);
    }

    public Optional<Boolean> getOptBoolean(@NotNull String key) {
        return getOpt(key).map("~true"::equals);
    }

    public Optional<Integer> getOptInt(@NotNull String key) {
        return getOpt(key).map(Integer::parseInt);
    }

    public Optional<Double> getOptDouble(@NotNull String key) {
        return getOpt(key).map(Double::parseDouble);
    }

    public boolean getAsBoolean(@NotNull String key) {
        return getOptBoolean(key).orElse(false);
    }

        @NotNull
    public Map<String, String> asConfigMap(){
        final Map<String, String> out = new LinkedHashMap<>(size() * 2);
        forEach((k, v) -> {
            final String value = v.get();
            if (value != null) { // hack to destinguish between config und non config parameters
                if ("~true".equalsIgnoreCase(value)){
                    out.put(k, null);
                }else if (!"~false".equalsIgnoreCase(value)){
                    out.put(k, value);
                }//skip if it is boolean and false
            }
        });

        return out;
    }

    public List<String> asParameterList() {
        final List<String> out = new ArrayList<>();
        asConfigMap().forEach((k, v) -> {
            if (v == null){
                out.add("--" + k);
            } else {
                out.add("--" + k + "=" + v);
            }
        });

        return out;
    }

    public List<String> getParameter(String key) {
        if (!containsKey(key))
            return Collections.emptyList();
        return List.of(("--" + key), get(key).get());
    }
}
