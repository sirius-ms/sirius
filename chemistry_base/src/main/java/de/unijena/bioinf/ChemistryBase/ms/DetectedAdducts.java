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
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Collects multiple PossibleAdducts adducts from different sources and stores a source identifier.
 * This is intended to collect Adducts from different detection sources.
 * Can be attached to MsExperiment
 */
public final class DetectedAdducts extends ConcurrentHashMap<DetectedAdducts.Source, PossibleAdducts> implements Ms2ExperimentAnnotation, Cloneable { //todo ElementFilter: ConcurrentHashMap is not immutable. Hence, in princlipe, this could be cleared. Should Ms2ExperimentAnnotation be immutable?
    public enum Source {
        /**
         * this source indicates adducts specified in the input file
         */
        INPUT_FILE(true, false, true),
        /**
         * adducts found during SIRIUS LCMS preprocessing. May contain unknown adduct to indicate to add fallback adducts.
         */
        LCMS_ALIGN(true, true, false),
        /**
         * adducts detected based on MS1 only are never very confident. Hence, we will always add the fallback adducts.
         */
        MS1_PREPROCESSOR(true, true, false),

        //special sources. These are only added additionally, but not used as primary source. Hence. if only these are available, we add the fallbacks.

        /**
         * adducts found by spectral library search. May be additionally added.
         */
        SPECTRAL_LIBRARY_SEARCH(false, false, false),
        /**
         * this is never directly specified. It is only used to make sure the map can be parsed from string. Unknown sources are mapped to this enum value. May be additionally added.
         */
        UNSPECIFIED_SOURCE(false, false, false);


        private final boolean isPrimarySource;
        private final boolean canBeEmpty;
        private final boolean forbidAdditionalSources;

        Source(boolean isPrimarySource, boolean canBeEmpty, boolean forbidAdditionalSources) {
            this.isPrimarySource = isPrimarySource;
            this.canBeEmpty = canBeEmpty;
            this.forbidAdditionalSources = forbidAdditionalSources;
        }

        public boolean isPrimaryDetectionSource() {
            return isPrimarySource;
        }

        public boolean isAdditionalDetectionSource() {
            return !isPrimarySource;
        }

        public boolean isForbidAdditionalSources() {
            return forbidAdditionalSources;
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
    protected Optional<PossibleAdducts> getPrimaryAdducts() {
        return getAdducts((Source[]) Arrays.stream(Source.values()).filter(Source::isPrimaryDetectionSource).toArray(l -> new Source[l])).flatMap(pa -> pa.isEmpty() ? Optional.empty() : Optional.of(pa));
    }

    /**
     *
     * @return additional adducts, if available. If no adducts are available returns Optional.empty and not empty PossibleAdducts.
     */
    protected Optional<PossibleAdducts> getAdditionalAdducts() {
        return getUnionOfAdducts((Source[]) Arrays.stream(Source.values()).filter(Source::isAdditionalDetectionSource).toArray(l -> new Source[l])).flatMap(pa -> pa.isEmpty() ? Optional.empty() : Optional.of(pa));
    }

    protected boolean hasPrimarySourceThatForbidsAdditionalSources() {
        return Arrays.stream(Source.values()).filter(Source::isPrimaryDetectionSource).filter(Source::isForbidAdditionalSources).anyMatch(source -> !getOrDefault(source, PossibleAdducts.empty()).isEmpty());
    }


    /**
     * returns adducts by the following rules
     * 1. input file adducts are returned if prioritized
     * 2. adducts of the most important primary source (may be from input file) are selected
     * 3. adducts of the additional sources are added if primary source allows for addition
     * 4. fallback adducts are added if set of adducts is empty so far or if the set contains an unknown {@link PrecursorIonType} [M+?] to specify fallback adduct shall be added
     * 5. the intersection of the set of adducts and the detectable adducts is calculated
     * 6. enforced adducts are added
     * 7. the final set is cleaned of unknown adducts and is returnd
     * @return set of adducts that shall be considered for compound annnotation
     */
    public PossibleAdducts getDetectedAdductsAndOrFallback(AdductSettings adductSettings, int charge) {
        if (adductSettings.isPrioritizeInputFileAdducts() && containsKey(Source.INPUT_FILE)) {
            PossibleAdducts inputAdducts = get(Source.INPUT_FILE);
            if (inputAdducts.isEmpty()){
                warnIsEmpty(Source.INPUT_FILE);
            } else if (!inputAdducts.hasUnknownIontype()) {
                return inputAdducts;
            } else {
                //input file detected adduct annotation contains unknown adduct.
                //Probably a very uncommon way to specify this
            }
        }
        PossibleAdducts primaryAdductsOrFallback = getPrimaryAdducts().map(pa ->
                        (allowFallbackAdducts(pa)) ? PossibleAdducts.union(pa, adductSettings.getFallback(charge)) : pa)
                .orElse(new PossibleAdducts(adductSettings.getFallback(charge)));

        if (hasPrimarySourceThatForbidsAdditionalSources()) return processwithAdductSettingsAndClean(primaryAdductsOrFallback, adductSettings, charge);

        Optional<PossibleAdducts> additionalAdducts = getAdditionalAdducts();
        if (additionalAdducts.isEmpty()) return processwithAdductSettingsAndClean(primaryAdductsOrFallback, adductSettings, charge);
        else return processwithAdductSettingsAndClean(PossibleAdducts.union(primaryAdductsOrFallback, additionalAdducts.get()), adductSettings, charge);
    }

    private boolean allowFallbackAdducts(PossibleAdducts pa) {
        //an unknown PrecursorIonType such aus [M+?]+ means that we are not sure and still want to add fallback adducts
        return pa.hasUnknownIontype() || pa.isEmpty();
    }

    private void warnIsEmpty(Source source) {
        LoggerFactory.getLogger(this.getClass()).warn("Detected adduct source '" + source + "' specified, but adducts are empty.");
    }


    private static final PrecursorIonType M_PLUS = PrecursorIonType.getPrecursorIonType("[M]+");
    private static final PrecursorIonType M_H_PLUS = PrecursorIonType.getPrecursorIonType("[M+H]+");

    /**
     * 1. remove unknown adducts
     * 2. guarantees that never both, [M]+ and [M+H]+, are contained. This prevents issues with duplicate structure candidates in subsequent steps. [M+H]+ is favored.
     * @param possibleAdducts
     * @return
     */
    private PossibleAdducts cleanAdducts(PossibleAdducts possibleAdducts) {
        Set<PrecursorIonType> adducts = possibleAdducts.getAdducts().stream().filter(a -> !a.isIonizationUnknown()).collect(Collectors.toCollection(HashSet::new));
        if (adducts.contains(M_PLUS) && adducts.contains(M_H_PLUS)) adducts.remove(M_PLUS);
        return new PossibleAdducts(adducts);
    }

    /**
     * interect with detectable adducts, add enforced and clean unknown adducts
     * @param possibleAdducts
     * @param as
     * @param charge
     * @return
     */
    private PossibleAdducts processwithAdductSettingsAndClean(PossibleAdducts possibleAdducts, AdductSettings as, int charge) {
        possibleAdducts = PossibleAdducts.intersection(possibleAdducts, as.getDetectable());

        if (!as.getEnforced(charge).isEmpty())
            possibleAdducts = PossibleAdducts.union(possibleAdducts, as.getEnforced(charge));

        possibleAdducts = cleanAdducts(possibleAdducts);

        if (possibleAdducts.isEmpty())
            LoggerFactory.getLogger(this.getClass()).error("Final set of selected adducts is empty.");

        return possibleAdducts;
    }

    public Optional<PossibleAdducts> getAdducts(Source... keyPrio) {
        for (Source key : keyPrio) {
            if (containsKey(key)) {
                if (key.canBeEmpty || !get(key).isEmpty()) {
                    return Optional.of(get(key));
                } else {
                    warnIsEmpty(key);
                }
            }
        }
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

    public PossibleAdducts put(@NotNull Source key, @NotNull PossibleAdducts value) {
        if (key == Source.MS1_PREPROCESSOR)
            return super.put(key, ensureMS1PreprocessorAllowsToAddFallbackAdducts(value));
        else
            return super.put(key, value);
    }

    private PossibleAdducts ensureMS1PreprocessorAllowsToAddFallbackAdducts(@NotNull PossibleAdducts value) {
        //this ensures we add fallback adducts, because we are never certain enough with MS1_PREPROCESSOR
        //both, empty list or unknown adduct indicate to add fallback
        if (!value.isEmpty() && !value.hasUnknownIontype()) {
            Set<PrecursorIonType> adducts = new HashSet<>(value.getAdducts());
            //to indicate that MS1_PREPROCESSOR is always combined with fallback adducts
            if (adducts.stream().anyMatch(PrecursorIonType::isPositive)) {
                adducts.add(PrecursorIonType.unknown(1));
            } else {
                adducts.add(PrecursorIonType.unknown(-1));
            }
            return new PossibleAdducts(adducts);
        } else {
            return value;
        }
    }

    /**
     *
     * @param source
     * @return true if {@link DetectedAdducts} contain adducts from a {@link Source} that is more important than the queried.
     */
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
