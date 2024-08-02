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

package de.unijena.bioinf.ms.persistence.model.core.feature;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stores a detected adducts with score and source information.
 */
@NoArgsConstructor
@EqualsAndHashCode
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.ANY)
public class DetectedAdducts {

    public static DetectedAdducts singleton(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source, PrecursorIonType ionType) {
        DetectedAdducts det = new DetectedAdducts();
        det.add(new DetectedAdduct(ionType, 1d, source));
        return det;
    }

    @NotNull
    private final Map<PrecursorIonType, Set<DetectedAdduct>> detectedAdducts = new HashMap<>();

    public Map<PrecursorIonType, Set<DetectedAdduct>> asMap() {
        return detectedAdducts;
    }

    public DetectedAdducts add(PrecursorIonType key, DetectedAdduct... adducts) {
        return add(key, Arrays.asList(adducts));
    }

    public DetectedAdducts add(PrecursorIonType key, Collection<DetectedAdduct> adducts) {
        detectedAdducts.computeIfAbsent(key, k -> new HashSet<>()).addAll(adducts);
        return this;
    }

    public DetectedAdducts add(DetectedAdduct... detectedAdducts) {
        return add(Arrays.asList(detectedAdducts));
    }

    public DetectedAdducts add(Collection<DetectedAdduct> detectedAdducts) {
        detectedAdducts.stream().collect(Collectors.groupingBy(DetectedAdduct::getAdduct, Collectors.toList()))
                .forEach((k, v) -> this.detectedAdducts.computeIfAbsent(k, key -> new HashSet<>()).addAll(v));
        return this;
    }

    @NotNull
    @JsonIgnore
    public List<PrecursorIonType> getAllAdducts() {
        return detectedAdducts.keySet().stream().toList();
    }

    @NotNull
    @JsonIgnore
    public Optional<PrecursorIonType> getBestAdduct() {
        return getBestDetectedAdduct().map(DetectedAdduct::getAdduct);
    }

    @JsonIgnore
    public Optional<DetectedAdduct> getBestDetectedAdduct() {
        return detectedAdducts.values().stream().flatMap(Collection::stream).max(Comparator.naturalOrder());
    }

    @Nullable
    @JsonIgnore
    Set<DetectedAdduct> getDetections(String key) {
        return getDetections(PrecursorIonType.fromString(key));
    }

    @Nullable
    @JsonIgnore
    Set<DetectedAdduct> getDetections(PrecursorIonType key) {
        return detectedAdducts.get(key);
    }

    public Set<DetectedAdduct> remove(@NotNull PrecursorIonType key) {
        return detectedAdducts.remove(key);
    }

    public List<PrecursorIonType> removeBySource(@NotNull de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source) {
        List<PrecursorIonType> modified = new ArrayList<>();

        detectedAdducts.forEach((adduct, list) -> {
            if (list.removeIf(detection -> detection.getSource() == source))
                modified.add(adduct);
        });

        modified.stream().filter(adduct -> detectedAdducts.get(adduct).isEmpty()).forEach(detectedAdducts::remove);

        return modified;
    }

    public boolean remove(@NotNull PrecursorIonType key, @NotNull de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source) {
        Set<DetectedAdduct> set = detectedAdducts.get(key);
        if (set != null)
            if (set.removeIf(adduct -> adduct.getSource() == source)) {
                if (set.isEmpty())
                    detectedAdducts.remove(key);
                return true;
            }
        return false;
    }

    @JsonIgnore
    public boolean hasAdduct() {
        return !detectedAdducts.isEmpty();
    }

    @JsonIgnore
    public boolean hasSingleAdduct(){
        return detectedAdducts.size() == 1;
    }

    @JsonInclude //getter just for serialization
    private List<DetectedAdduct> getDetectedAdductsList() {
        return this.detectedAdducts.values().stream().flatMap(Collection::stream).toList();
    }

    @JsonInclude //setter just for deserialization
    private void setDetectedAdductsList(List<DetectedAdduct> detectedAdductsList) {
        this.detectedAdducts.clear();
        add(detectedAdductsList);
    }
}
