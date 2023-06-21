/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * additional fields and comments which are parsed from the input file and are no parameters for SIRIUS computations.
 */
public class AdditionalFields implements Map<String, String>, Ms2ExperimentAnnotation, SpectrumAnnotation {
    private final Map<String, String> source;

    public AdditionalFields() {
        this(true);
    }

    public AdditionalFields(boolean caseSensitive) {
        this(caseSensitive ? null : String.CASE_INSENSITIVE_ORDER);
    }

    public AdditionalFields(@Nullable Comparator<String> comparator) {
        source = comparator == null ? new HashMap<>() : new TreeMap<>(comparator);
    }

    public Optional<String> getField(String key) {
        return Optional.ofNullable(get(key.toLowerCase()));
    }

    public boolean isComparatorBasedMap() {
        return (source instanceof TreeMap);
    }

    public Comparator<String> comparator() {
        if (!isComparatorBasedMap())
            return null;
        else
            return ((TreeMap) source).comparator();
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    public boolean isEmpty() {
        return source.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return source.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return source.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return source.get(key);
    }

    @Nullable
    @Override
    public String put(String key, String value) {
        return source.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return source.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends String> m) {
        source.putAll(m);
    }

    @Override
    public void clear() {
        source.clear();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return source.keySet();
    }

    @NotNull
    @Override
    public Collection<String> values() {
        return source.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, String>> entrySet() {
        return source.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AdditionalFields))
            return false;
        return source.equals(o);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }
}
