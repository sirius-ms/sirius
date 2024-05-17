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

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Collects multiple PossibleAdducts adducts from different sources and stores a source identifier.
 * This is intended to collect Adducts from different detection sources.
 * Can be attached to MsExperiment
 */
public final class DetectedAdducts extends ConcurrentHashMap<String, PossibleAdducts> implements Ms2ExperimentAnnotation, Cloneable { //todo ElementFilter: ConcurrentHashMap is not immutable. Hence, in princlipe, this could be cleared. Should Ms2ExperimentAnnotation be immutable?
    public enum Source {INPUT_FILE, LCMS_ALIGN, MS1_PREPROCESSOR, SPECTRAL_LIBRARY_SEARCH, UNSPECIFIED_SOURCE} //todo implement PossibleAdducts by library search

    public DetectedAdducts() {
        super();
    }

    public Optional<PossibleAdducts> getAdducts() {
        return getAdducts(Source.values());
    }

    public Optional<PossibleAdducts> getAdducts(Source... keyPrio) {
        return getAdducts(Arrays.stream(keyPrio).map(Source::name).toArray(String[]::new));
    }

    public Optional<PossibleAdducts> getAdducts(String... keyPrio) {
        for (String key : keyPrio)
            if (containsKey(key))
                return Optional.of(get(key));

        return Optional.empty();
    }

    public PossibleAdducts getAllAdducts() {
        return values().stream().flatMap(it -> it.getAdducts().stream()).collect(Collectors.collectingAndThen(Collectors.toSet(), PossibleAdducts::new));
    }


    public boolean hasAdducts() {
        if (isEmpty())
            return false;
        return values().stream().anyMatch(it -> !it.isEmpty());
    }

    public boolean containsKey(Source key) {
        return containsKey(key.name());
    }

    public PossibleAdducts get(Source key) {
        return get(key.name());
    }

    public PossibleAdducts put(@NotNull Source key, @NotNull PossibleAdducts value) {
        return put(key.name(), value);
    }

    public Set<String> getSourceStrings() {
        //todo ElementFilter: Do we require String keys for flexibility or can be change it to the Source enum?
        return Collections.unmodifiableSet(keySet());
    }

    public Set<Source> getSources() {
        Set<Source> sourceSet = new HashSet<>();
        for (String key : keySet()) {
            try {
                Source source = Source.valueOf(key);
                sourceSet.add(source);
            } catch (IllegalArgumentException e) {
                sourceSet.add(Source.UNSPECIFIED_SOURCE);
            }
        }
        return sourceSet;
    }

    @Override
    public String toString() {
        return toString(this);
    }

    public static String toString(DetectedAdducts in) {
        return in.entrySet().stream().map(e -> e.getKey() + ":{" + e.getValue().getAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.joining(",")) + "}").collect(Collectors.joining(","));
    }

    public static DetectedAdducts fromString(String json) {
        if (json == null || json.isBlank())
            return null;

        String[] mappings = json.split("\\s*}\\s*,\\s*");
        if (mappings.length ==0)
            return null;

        final DetectedAdducts ads = new DetectedAdducts();
        for (String mapping : mappings) {
            String[] keyValue = mapping.replace("}", "").split("\\s*(:|->)\\s*\\{\\s*");
            PossibleAdducts val = keyValue.length > 1 ? Arrays.stream(keyValue[1].split(",")).filter(Objects::nonNull).filter(s -> !s.isBlank()).map(PrecursorIonType::parsePrecursorIonType).flatMap(Optional::stream)
                    .collect(Collectors.collectingAndThen(Collectors.toSet(), PossibleAdducts::new)) : new PossibleAdducts();
            if (keyValue.length > 0)
                ads.put(keyValue[0], val);
        }

        return ads;
    }
}
