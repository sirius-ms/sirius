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
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores a detected adducts with score and source information.
 */
@NoArgsConstructor
@EqualsAndHashCode
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.ANY)
public class DetectedAdducts implements Cloneable {

    public static DetectedAdducts singleton(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source, PrecursorIonType ionType) {
        DetectedAdducts det = new DetectedAdducts();
        det.addAll(DetectedAdduct.unambiguous(source, ionType));
        return det;
    }

    public static DetectedAdducts emptySingleton(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source) {
        DetectedAdducts det = new DetectedAdducts();
        det.addAll(DetectedAdduct.empty(source));
        return det;
    }

    @NotNull
    private final Set<DetectedAdduct> detectedAdducts = new HashSet<>();

    @Override
    public String toString() {
        return "[ " +
                detectedAdducts.stream().map(DetectedAdduct::toString).collect(Collectors.joining(", ")) +
                " ]";
    }

    public DetectedAdducts addEmptySource(@NotNull de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source) {
        addAll(new DetectedAdduct(null, Double.NaN, source));
        return this;
    }

    public DetectedAdducts addAll(DetectedAdduct... detectedAdducts) {
        return addAll(Arrays.asList(detectedAdducts));
    }

    public DetectedAdducts addAll(Collection<DetectedAdduct> detectedAdducts) {
        this.detectedAdducts.addAll(detectedAdducts);
        return this;
    }

    /**
     * @return Return raw adduct list including empty detections.
     */
    @NotNull
    @JsonIgnore
    public List<PrecursorIonType> getAllAdducts() {
        return detectedAdducts.stream().map(DetectedAdduct::getAdduct)
                .filter(Objects::nonNull)
                .filter(Predicate.not(PrecursorIonType::isIonizationUnknown))
                .distinct().toList();
    }

    @NotNull
    @JsonIgnore
    public List<de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source> getAllSources() {
        return detectedAdducts.stream().map(DetectedAdduct::getSource).distinct().toList();
    }

    public Set<DetectedAdduct> removeAllWithAdduct(@Nullable PrecursorIonType adduct) {
        Iterator<DetectedAdduct> iterator = detectedAdducts.iterator();
        Set<DetectedAdduct> deletedAdducts = new HashSet<>();
        while (iterator.hasNext()) {
            DetectedAdduct next =  iterator.next();
            if (Objects.equals(adduct, next.getAdduct())){
                iterator.remove();
                deletedAdducts.add(next);
            }
        }
        return deletedAdducts;
    }

    public List<PrecursorIonType> removeAllWithSource(@NotNull de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source) {
        List<PrecursorIonType> modified = new ArrayList<>();

        Iterator<DetectedAdduct> iterator = detectedAdducts.iterator();
        while (iterator.hasNext()) {
            DetectedAdduct next =  iterator.next();
            if (source.equals(next.getSource())){
                iterator.remove();
                if (next.getAdduct() != null)
                    modified.add(next.getAdduct());
            }
        }
        return modified;
    }

    public boolean remove(@Nullable PrecursorIonType adduct, @NotNull de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source source) {
       return detectedAdducts.removeIf(it -> Objects.equals(source, it.getSource()) && Objects.equals(adduct, it.getAdduct()));
    }

    /**
     * @return true if there is at least one real detected adduct. [M+?]+ and [M+?]- do not count as detected adduct.
     */
    @JsonIgnore
    public boolean hasDetectedAdduct() {
        return !getAllAdducts().isEmpty();
    }

    @JsonIgnore
    public boolean hasSingleAdduct(){
        return getAllAdducts().size() == 1;
    }

    @JsonIgnore
    public boolean isAdductUnknown(){
        return !hasDetectedAdduct();
    }

    /**
     * @return Raw detected adduct list
     */
    @JsonIgnore
    public Stream<DetectedAdduct> getDetectedAdductsStr() {
        return this.detectedAdducts.stream();
    }

    @JsonInclude //getter just for serialization
    private List<DetectedAdduct> getDetectedAdductsList() {
        return getDetectedAdductsStr().toList();
    }

    @JsonInclude //setter just for deserialization
    private void setDetectedAdductsList(List<DetectedAdduct> detectedAdductsList) {
        this.detectedAdducts.clear();
        addAll(detectedAdductsList);
    }

    @SneakyThrows
    @JsonIgnore
    @Override
    public DetectedAdducts clone() {
        DetectedAdducts clone = (DetectedAdducts) super.clone();
        clone.addAll(detectedAdducts);
        return clone;
    }

    public boolean contains(DetectedAdduct adduct) {
        return detectedAdducts.contains(adduct);
    }
}
