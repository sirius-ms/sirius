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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.middleware.model.compute.tools.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Object to submit a job to be executed by SIRIUS
 */
@Getter
@Setter
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobSubmission extends AbstractSubmission {
    /**
     * Describes how to deal with Adducts: Fallback adducts are considered if the auto detection did not find any indication for an ion mode.
     * Pos Examples: [M+H]+,[M]+,[M+K]+,[M+Na]+,[M+H-H2O]+,[M+Na2-H]+,[M+2K-H]+,[M+NH4]+,[M+H3O]+,[M+MeOH+H]+,[M+ACN+H]+,[M+2ACN+H]+,[M+IPA+H]+,[M+ACN+Na]+,[M+DMSO+H]+
     * Neg Examples: [M-H]-,[M]-,[M+K-2H]-,[M+Cl]-,[M-H2O-H]-,[M+Na-2H]-,M+FA-H]-,[M+Br]-,[M+HAc-H]-,[M+TFA-H]-,[M+ACN-H]-
     */
    @Schema(nullable = true)
    List<String> fallbackAdducts;
    /**
     * Describes how to deal with Adducts:  Enforced adducts that are always considered.
     * Pos Examples: [M+H]+,[M]+,[M+K]+,[M+Na]+,[M+H-H2O]+,[M+Na2-H]+,[M+2K-H]+,[M+NH4]+,[M+H3O]+,[M+MeOH+H]+,[M+ACN+H]+,[M+2ACN+H]+,[M+IPA+H]+,[M+ACN+Na]+,[M+DMSO+H]+
     * Neg Examples: [M-H]-,[M]-,[M+K-2H]-,[M+Cl]-,[M-H2O-H]-,[M+Na-2H]-,M+FA-H]-,[M+Br]-,[M+HAc-H]-,[M+TFA-H]-,[M+ACN-H]-
     */
    @Schema(nullable = true)
    List<String> enforcedAdducts;
    /**
     * Describes how to deal with Adducts: Detectable adducts which are only considered if there is an indication in the MS1 scan (e.g. correct mass delta).
     * Pos Examples: [M+H]+,[M]+,[M+K]+,[M+Na]+,[M+H-H2O]+,[M+Na2-H]+,[M+2K-H]+,[M+NH4]+,[M+H3O]+,[M+MeOH+H]+,[M+ACN+H]+,[M+2ACN+H]+,[M+IPA+H]+,[M+ACN+Na]+,[M+DMSO+H]+
     * Neg Examples: [M-H]-,[M]-,[M+K-2H]-,[M+Cl]-,[M-H2O-H]-,[M+Na-2H]-,M+FA-H]-,[M+Br]-,[M+HAc-H]-,[M+TFA-H]-,[M+ACN-H]-
     */
    @Schema(nullable = true)
    List<String> detectableAdducts;

    /**
     * Indicate if already existing result for a tool to be executed should be overwritten or not.
     */
    @Schema(nullable = true)
    Boolean recompute;

    /**
     * Parameter Object for spectral library search tool (CLI-Tool: spectra-search).
     * Library search results can be used to enhance formula search results
     * If NULL the tool will not be executed.
     */
    @Schema(nullable = true)
    SpectralLibrarySearch spectraSearchParams;
    /**
     * Parameter Object for molecular formula identification tool (CLI-Tool: formula, sirius).
     * If NULL the tool will not be executed.
     */
    @Schema(nullable = true)
    Sirius formulaIdParams;
    /**
     * Parameter Object for network  based molecular formula re-ranking (CLI-Tool: zodiac).
     * If NULL the tool will not be executed.
     */
    @Schema(nullable = true)
    Zodiac zodiacParams;
    /**
     * Parameter Object for Fingerprint prediction with CSI:FingerID (CLI-Tool: fingerint).
     * If NULL the tool will not be executed.
     */
    @Schema(nullable = true)
    FingerprintPrediction fingerprintPredictionParams;
    /**
     * Parameter Object for CANOPUS compound class prediction tool (CLI-Tool: canopus).
     * If NULL the tool will not be executed.
     */
    @Schema(nullable = true)
    Canopus canopusParams;
    /**
     * Parameter Object for structure database search with CSI:FingerID (CLI-Tool: structure).
     * If NULL the tool will not be executed.
     */
    @Schema(nullable = true)
    StructureDbSearch structureDbSearchParams;

    /**
     * Parameter Object for MsNovelist DeNovo structure generation (CLI-Tool: msnovelist)
     * If NULL the tool will not be executed.
     */
    @Schema(nullable = true)
    MsNovelist msNovelistParams;

    /**
     * As an alternative to the object based parameters, this map allows to store key value pairs
     * of ALL SIRIUS parameters. All possible parameters can be retrieved from SIRIUS via the respective endpoint.
     */
    @Nullable
    Map<String, String> configMap;

    public static JobSubmission createDefaultInstance(boolean includeConfigMap) {
        AdductSettings settings = PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class);
        //common default search dbs for spectra and structure. Formula only if db search is used.
        List<String> searchDbs = Stream.concat(Stream.of(
                        CustomDataSources.getSourceFromName(DataSource.BIO.name())),
                CustomDataSources.sourcesStream().filter(CustomDataSources.Source::isCustomSource)).distinct().map(CustomDataSources.Source::name).toList();


        JobSubmissionBuilder<?, ?> b = JobSubmission.builder()
                .fallbackAdducts(settings.getFallback().stream().map(PrecursorIonType::toString).collect(Collectors.toList()))
                .enforcedAdducts(settings.getEnforced().stream().map(PrecursorIonType::toString).collect(Collectors.toList()))
                .detectableAdducts(settings.getDetectable().stream().map(PrecursorIonType::toString).collect(Collectors.toList()))
                .recompute(false)
                .spectraSearchParams(SpectralLibrarySearch.builderWithDefaults().spectraSearchDBs(searchDbs).build())
                .formulaIdParams(Sirius.buildDefault())
                .zodiacParams(Zodiac.buildDefault())
                .fingerprintPredictionParams(FingerprintPrediction.buildDefault())
                .canopusParams(Canopus.buildDefault())
                .structureDbSearchParams(StructureDbSearch.builderWithDefaults().structureSearchDBs(searchDbs).build())
                .msNovelistParams(MsNovelist.buildDefault());
        if (includeConfigMap) {
            final Map<String, String> configMap = new HashMap<>();
            PropertyManager.DEFAULTS.getConfigKeys().forEachRemaining(k ->
                    configMap.put(k, PropertyManager.DEFAULTS.getConfigValue(k)));
            b.configMap(configMap);
        }
        return b.build();
    }


    /**
     * Provides the full toolchain command needed to compute the full workflow defined by this object.
     * @return Config command as list
     */
    @JsonIgnore
    public List<String> asCommand() {
        List<String> commands = asConfigToolCommand();
        commands.addAll(getEnabledTools().stream()
                .map(Tool::getCommand)
                .map(CommandLine.Command::name)
                .toList());
        return commands;
    }

    /**
     * Provides a Config subtool command with all parameters from the combined config map (asCombinedConfigMap).
     * @return Config command as list
     */
    @JsonIgnore
    public List<String> asConfigToolCommand() {
        List<String> configTool = new ArrayList<>();
        configTool.add("config");
        asCombinedConfigMap().forEach((k, v) -> {
            if (v == null){
                configTool.add("--" + k);
            } else {
                configTool.add("--" + k + "=" + v);
            }
        });
        //ensure that there are not whitespaces
        return configTool.stream().map(s -> s.replaceAll("\\s+", "")).collect(Collectors.toList());
    }


    /**
     * Provides a combined config map that can be used to create a command for execution.
     * The map combines the map representation from all tools and the map representation
     * of the submission. Parameters set in the object (non-null) will override values in the global config
     * map if the JobSubmission object.
     * @return Combined config map
     */
    @JsonIgnore
    public Map<String, String> asCombinedConfigMap() {
        Map<String, String> combined = asConfigMap();
        getEnabledTools().stream().map(Tool::asConfigMap).forEach(combined::putAll);
        return combined;
    }


    @JsonIgnore
    private Map<String, String> asConfigMap() {

        return new NullCheckMapBuilder(Optional.ofNullable(getConfigMap()).orElse(new HashMap<>()))
                .putIfNonNull("AdductSettings.enforced", getEnforcedAdducts(), (v) -> v.isEmpty()
                        ? "," : String.join(",", v))

                .putIfNonNull("AdductSettings.detectable", getDetectableAdducts(), (v) -> v.isEmpty()
                        ? "," : String.join(",", v))

                .putIfNonNull("AdductSettings.fallback", getFallbackAdducts(), (v) -> v.isEmpty()
                        ? "," : String.join(",", v))
                .putIfNonNull("RecomputeResults", getRecompute())
                .toMap();
    }

    @JsonIgnore
    public List<Tool<?>> getEnabledTools() {
        return Stream.of(spectraSearchParams, formulaIdParams, zodiacParams, fingerprintPredictionParams, canopusParams, structureDbSearchParams, msNovelistParams)
                .filter(Objects::nonNull).filter(Tool::isEnabled).collect(Collectors.toList());
    }
}
