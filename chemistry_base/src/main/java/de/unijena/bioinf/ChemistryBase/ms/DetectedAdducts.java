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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Collects multiple PossibleAdducts adducts from different sources and stores a source identifier.
 * This is intended to collect Adducts from different detection sources.
 * Can be attached to MsExperiment
 */
public final class DetectedAdducts extends ConcurrentHashMap<DetectedAdducts.Source, PossibleAdducts> implements Ms2ExperimentAnnotation, Cloneable { //todo ElementFilter: ConcurrentHashMap is not immutable. Hence, in princlipe, this could be cleared. Should Ms2ExperimentAnnotation be immutable?
    public enum Source {
        /*
        for compatibility with old way of settting Ms2Experiment PrecursorIonType in input parser
         */
        KNOWN_ADDUCT(true, false, false),
        /*
        this way adducts should now be specified in the input file
         */
        INPUT_FILE(true, false, false),
        LCMS_ALIGN(true, true, true),
        MS1_PREPROCESSOR(true, true, true),

        //special sources. These are only added additionally, but not used as primary source. Hence. if only these are available, we add the fallbacks.

        SPECTRAL_LIBRARY_SEARCH(false, false, true),
        /*
        this is in case the source string could not be parsed
         */
        UNSPECIFIED_SOURCE(false, false, true);


        private final boolean isPrimarySource;
        private final boolean canBeEmpty;
        private final boolean isEnforced;

        Source(boolean isPrimarySource, boolean canBeEmpty, boolean allowAdditionalSources) {
            this.isPrimarySource = isPrimarySource;
            this.canBeEmpty = canBeEmpty;
            this.isEnforced = allowAdditionalSources;
        }

        public boolean isPrimaryDetectionSource() {
            return isPrimarySource;
        }

        public boolean isAdditionalDetectionSource() {
            return !isPrimarySource;
        }
    }

    public static DetectedAdducts singleton(Source source, PrecursorIonType ionType) {
        DetectedAdducts det = new DetectedAdducts();
        det.put(source, new PossibleAdducts(ionType));
        return det;
    }

    public DetectedAdducts() {
        super();
    }

    /**
     *
     * @return primary adducts, if available. If no adducts are available returns Optional.empty and not empty PossibleAdducts.
     */
    public Optional<PossibleAdducts> getPrimaryAdducts() {
        return getAdducts((Source[]) Arrays.stream(Source.values()).filter(Source::isPrimaryDetectionSource).toArray(l -> new Source[l])).flatMap(pa -> pa.isEmpty() ? Optional.empty() : Optional.of(pa));
    }

    /**
     *
     * @return additional adducts, if available. If no adducts are available returns Optional.empty and not empty PossibleAdducts.
     */
    public Optional<PossibleAdducts> getAdditionalAdducts() {
        return getUnionOfAdducts((Source[]) Arrays.stream(Source.values()).filter(Source::isAdditionalDetectionSource).toArray(l -> new Source[l])).flatMap(pa -> pa.isEmpty() ? Optional.empty() : Optional.of(pa));
    }

    protected boolean hasPrimarySourceTheForbidsAdditionalSources() {
        return Arrays.stream(Source.values()).filter(Source::isPrimaryDetectionSource).anyMatch(source -> getOrDefault(source, PossibleAdducts.empty()).isEmpty());
    }

    /**
     * returns detected adducts from the best primary source (see {@link Source}). Plus detected adducts from the "additional" sources maybe added.
     * @return set of adducts that shall be considered for compound annnotation
     */
    public PossibleAdducts getSelectedDetectedAdducts() {
        return getDetectedAdductsAndOrFallback(() -> Collections.emptySet());
    }

    /**
     * returns detected adducts when a primary source detected/specified adducts (see {@link Source}). Else uses the fallback adducts.
     * On top, detected adducts from the "additional" sources maybe added.
     * Special case: if primary source contains unknown {@link PrecursorIonType} fallback is also added
     * @param fallbackAdductsSupplier
     * @return set of adducts that shall be considered for compound annnotation
     */
    public PossibleAdducts getDetectedAdductsAndOrFallback(Supplier<Set<PrecursorIonType>> fallbackAdductsSupplier) {
        PossibleAdducts primaryAdductsOrFallback = getPrimaryAdducts().map(pa ->
                //an unknown PrecursorIonType such aus [M+?]+ means that we are not sure and still want to add fallback adducts
                        (pa.hasUnknownIontype() || pa.isEmpty()) ? PossibleAdducts.union(pa, fallbackAdductsSupplier.get()) : pa)
                .orElse(new PossibleAdducts(fallbackAdductsSupplier.get()));

        if (hasPrimarySourceTheForbidsAdditionalSources()) return primaryAdductsOrFallback;

        Optional<PossibleAdducts> additionalAdducts = getAdditionalAdducts();
        if (additionalAdducts.isEmpty()) return primaryAdductsOrFallback;
        else return PossibleAdducts.union(primaryAdductsOrFallback, additionalAdducts.get());
    }

    public Optional<PossibleAdducts> getAdducts(Source... keyPrio) {
        for (Source key : keyPrio)
            if (containsKey(key) && (key.canBeEmpty || !get(key).isEmpty()))
                return Optional.of(get(key));
        return Optional.empty();
    }

    /**
     * not so efficient for many sources. But best if most of the times less or equal 1 sources are expected.
     * @param keys
     * @return
     */
    protected Optional<PossibleAdducts> getUnionOfAdducts(Source... keys) {
        PossibleAdducts union = null;
        for (Source key : keys)
            if (containsKey(key)) {
                if (union == null) union = get(key);
                else union = PossibleAdducts.union(union, get(key));
            }
        return union == null ? Optional.empty() : Optional.of(union);
    }

    public PossibleAdducts getAllAdducts() {
        return values().stream().flatMap(it -> it.getAdducts().stream()).collect(Collectors.collectingAndThen(Collectors.toSet(), PossibleAdducts::new));
    }


    public boolean hasAdducts() {
        if (isEmpty())
            return false;
        return values().stream().anyMatch(it -> !it.isEmpty());
    }

    public Set<Source> getSources() {
        return keySet();
    }

    public boolean hasMoreImportantSource(Source source) {
        return keySet().stream().anyMatch(k -> k.compareTo(source)<0);
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
            if (keyValue.length > 0){
                Source source;
                try {
                    source = Source.valueOf(keyValue[0]);
                } catch (IllegalArgumentException e) {
                    source = Source.UNSPECIFIED_SOURCE;
                }
                ads.put(source, val);
            }

        }

        return ads;
    }
}
