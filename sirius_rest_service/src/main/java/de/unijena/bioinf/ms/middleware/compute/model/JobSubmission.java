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

package de.unijena.bioinf.ms.middleware.compute.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ms.middleware.compute.model.tools.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Object to submit a job to be executed by SIRIUS
 */
@Getter
@Setter
public class JobSubmission {
    /**
     * Compounds that should be the input for this Job
     */
    List<String> compoundIds;

    /**
     * Describes how to deal with Adducts: Fallback adducts are considered if the auto detection did not find any indication for an ion mode.
     * Pos Examples: [M+H]+,[M]+,[M+K]+,[M+Na]+,[M+H-H2O]+,[M+Na2-H]+,[M+2K-H]+,[M+NH4]+,[M+H3O]+,[M+MeOH+H]+,[M+ACN+H]+,[M+2ACN+H]+,[M+IPA+H]+,[M+ACN+Na]+,[M+DMSO+H]+
     * Neg Examples: [M-H]-,[M]-,[M+K-2H]-,[M+Cl]-,[M-H2O-H]-,[M+Na-2H]-,M+FA-H]-,[M+Br]-,[M+HAc-H]-,[M+TFA-H]-,[M+ACN-H]-
     */
    List<String> fallbackAdducts;
    /**
     * Describes how to deal with Adducts:  Enforced adducts that are always considered.
     * Pos Examples: [M+H]+,[M]+,[M+K]+,[M+Na]+,[M+H-H2O]+,[M+Na2-H]+,[M+2K-H]+,[M+NH4]+,[M+H3O]+,[M+MeOH+H]+,[M+ACN+H]+,[M+2ACN+H]+,[M+IPA+H]+,[M+ACN+Na]+,[M+DMSO+H]+
     * Neg Examples: [M-H]-,[M]-,[M+K-2H]-,[M+Cl]-,[M-H2O-H]-,[M+Na-2H]-,M+FA-H]-,[M+Br]-,[M+HAc-H]-,[M+TFA-H]-,[M+ACN-H]-
     */
    List<String> enforcedAdducts;
    /**
     * Describes how to deal with Adducts: Detectable adducts which are only considered if there is an indication in the MS1 scan (e.g. correct mass delta).
     * Pos Examples: [M+H]+,[M]+,[M+K]+,[M+Na]+,[M+H-H2O]+,[M+Na2-H]+,[M+2K-H]+,[M+NH4]+,[M+H3O]+,[M+MeOH+H]+,[M+ACN+H]+,[M+2ACN+H]+,[M+IPA+H]+,[M+ACN+Na]+,[M+DMSO+H]+
     * Neg Examples: [M-H]-,[M]-,[M+K-2H]-,[M+Cl]-,[M-H2O-H]-,[M+Na-2H]-,M+FA-H]-,[M+Br]-,[M+HAc-H]-,[M+TFA-H]-,[M+ACN-H]-
     */
    List<String> detectableAdducts;

    /**
     * Indicate if already existing result for a tool to be executed should be overwritten or not.
     */
    boolean recompute;

    /**
     * Parameter Object for molecular formula identification tool (CLI-Tool: formula, sirius).
     * If NULL the tool will not be executed.
     */
    Sirius formulaIdParas;
    /**
     * Parameter Object for network  based molecular formula re-ranking (CLI-Tool: zodiac).
     * If NULL the tool will not be executed.
     */
    Zodiac zodiacParas;
    /**
     * Parameter Object for Fingerprint prediction with CSI:FingerID (CLI-Tool: fingerint).
     * If NULL the tool will not be executed.
     */
    FingerprintPrediction fingerprintPredictionParas;
    /**
     * Parameter Object for structure database search with CSI:FingerID (CLI-Tool: structure).
     * If NULL the tool will not be executed.
     */
    StructureDbSearch structureDbSearchParas;
    /**
     * Parameter Object for CANOPUS compound class prediction tool (CLI-Tool: canopus).
     * If NULL the tool will not be executed.
     */
    Canopus canopusParas;

    //todo passatutto api.

    /**
     * As an alternative to the object based parameters, this map allows to store key value pairs
     * of ALL SIRIUS parameters. All possible parameters can be retrieved from SIRIUS via the respective endpoint.
     */
    @Nullable
    Map<String, String> configMap;

    public static JobSubmission createDefaultInstance(boolean includeConfigMap) {
        JobSubmission j = new JobSubmission();
        AdductSettings settings = PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class);
        j.setFallbackAdducts(settings.getFallback().stream().map(PrecursorIonType::toString).collect(Collectors.toList()));
        j.setEnforcedAdducts(settings.getEnforced().stream().map(PrecursorIonType::toString).collect(Collectors.toList()));
        j.setDetectableAdducts(settings.getDetectable().stream().map(PrecursorIonType::toString).collect(Collectors.toList()));
        j.setRecompute(false);
        j.setFormulaIdParas(new Sirius());
        j.setZodiacParas(new Zodiac());
        j.setFingerprintPredictionParas(new FingerprintPrediction());
        j.setStructureDbSearchParas(new StructureDbSearch());
        j.setCanopusParas(new Canopus());
        if (includeConfigMap) {
            final Map<String, String> configMap = new HashMap<>();
            PropertyManager.DEFAULTS.getConfigKeys().forEachRemaining(k ->
                    configMap.put(k, PropertyManager.DEFAULTS.getConfigValue(k)));
            j.setConfigMap(configMap);
        }
        return j;
    }


    @JsonIgnore
    public List<Tool<?>> getEnabledTools() {
        return Stream.of(formulaIdParas, zodiacParas, fingerprintPredictionParas, structureDbSearchParas, canopusParas)
                .filter(Objects::nonNull).filter(Tool::isEnabled).collect(Collectors.toList());
    }
}
