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
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidatesPerIonization;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.UseHeuristic;
import de.unijena.bioinf.elgordo.EnforceElGordoFormula;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.spectraldb.InjectSpectralLibraryMatchFormulas;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * User/developer friendly parameter subset for the Formula/SIRIUS tool
 * Can use results from Spectral library search tool.
 */

@Getter
@Setter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sirius extends Tool<SiriusOptions> {
    //todo NewWorkflow: adapt to new search modes
    @Schema(enumAsRef = true, nullable = true)
    enum Instrument {QTOF, ORBITRAP} //todo NewWorkflow: we don't have a FTICR profile anymore. in the GUI this is just the orbitrap with lower ppm. but 'ppm' is set separately here. I assume there is currently no better way to do this.

    /**
     * Instrument specific profile for internal algorithms
     * Just select what comes closest to the instrument that was used for measuring the data.
     */
    Instrument profile;
    /**
     * Number of formula candidates to keep as result list (Formula Candidates).
     */
    Integer numberOfCandidates;
    /**
     * Use this parameter if you want to force SIRIUS to report at least
     * NumberOfCandidatesPerIonization results per ionization.
     * if <= 0, this parameter will have no effect and just the top
     * NumberOfCandidates results will be reported.
     */
    Integer numberOfCandidatesPerIonization;
    /**
     * Maximum allowed mass deviation. Only molecular formulas within this mass window are considered.
     */
    Double massAccuracyMS2ppm;

    /**
     * Specify how isotope patterns in MS/MS should be handled.
     * <p>
     * FILTER: When filtering is enabled, molecular formulas are excluded if their
     * theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.
     * <p>
     * SCORE: Use them for SCORING. To use this the instrument should produce clear MS/MS isotope patterns
     * <p>
     * IGNORE: Ignore that there might be isotope patterns in MS/MS
     */
    IsotopeMs2Settings.Strategy isotopeMs2Settings;

    /**
     * When filtering is enabled, molecular formulas are excluded if their theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.
     */
    Boolean filterByIsotopePattern;

    ////////////////////////////////////////////////////////////////////////////
    //// molecular formula generation and search ///////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * El Gordo may predict that an MS/MS spectrum is a lipid spectrum. If enabled, the corresponding molecular formula will be enforeced as molecular formula candidate.
     */
    Boolean enforceElGordoFormula;

    /**
     * If true, molecular formula generation via bottom up search is enabled.
     */
    Boolean performBottomUpSearch;

    /**
     * Specifies the m/z below which de novo molecular formula generation is enabled. Set to 0 to disable de novo molecular formula generation.
     */
    Double  performDenovoBelowMz;


    /**
     * List Structure database to extract molecular formulas from to reduce formula search space.
     * SIRIUS is quite good at de novo formula annotation, so only enable if you have a good reason.
     */
    List<String> formulaSearchDBs;


    /**
     * By default, the formula (element) constraints are only applied to de novo molecular formula generation.
     * If true, the constraints are as well applied to database search and bottom up search.
     */
    Boolean applyFormulaConstraintsToDBAndBottomUpSearch;

    /////////////////////////////////////////////////////////////////////
    //// formula constraints ///////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////
    /**
     * These configurations hold the information how to autodetect elements based on the given formula constraints.
     * Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.
     * <p>
     * Enforced: Enforced elements are always considered
     */
    String enforcedFormulaConstraints;

    /**
     * These configurations hold the information how to autodetect elements based on the given formula constraints.
     * Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.
     * <p>
     * Fallback: Fallback elements are used, if the auto-detection fails (e.g. no isotope pattern available)
     */
    String fallbackFormulaConstraints;

    /**
     * These configurations hold the information how to autodetect elements based on the given formula constraints.
     * Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.
     * <p>
     * Detectable: Detectable elements are added to the chemical alphabet, if there are indications for them (e.g. in isotope pattern)
     */
    List<String> detectableElements;

    ////////////////////////////////////////////////////////////////////////////
    ///// fragmentation tree computation ///////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Timout settings for the ILP solver used for fragmentation tree computation
     * secondsPerInstance: Set the maximum number of seconds for computing a single compound. Set to 0 to disable the time constraint.
     * secondsPerTree: Set the maximum number of seconds for a single molecular formula check. Set to 0 to disable the time constraint
     */
    Timeout ilpTimeout;
    /**
     * Mass thresholds for heuristic fragmentation tree computation which dramatically speeds up computations.
     * useHeuristicAboveMz: For compounds above this threshold fragmentation trees will be computed heuristically for ranking. Tree that will be kept (numberOfCandidates) will be recomputed exactly
     * useOnlyHeuristicAboveMz:For compounds above this threshold fragmentation trees will be computed heuristically.
     */
    UseHeuristic useHeuristic;

    ////////////////////////////////////////////////////////////////////////////
    ///// spectral library serach //////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Similarity Threshold to inject formula candidates no matter which score/rank they have or which filter settings are applied.
     * If threshold >= 0 formulas candidates with reference spectrum similarity above the threshold will be injected.
     * If NULL injection is disables.
     */
    Double minScoreToInjectSpecLibMatch;


    private Sirius() {
        super(SiriusOptions.class);
    }

    @JsonIgnore
    @Override
    public Map<String, String> asConfigMap() {
        return new NullCheckMapBuilder()
                .putNonNullObj("UseHeuristic.useHeuristicAboveMz", useHeuristic, UseHeuristic::getUseHeuristicAboveMz)
                .putNonNullObj("UseHeuristic.useOnlyHeuristicAboveMz", useHeuristic, UseHeuristic::getUseOnlyHeuristicAboveMz)

                .putNonNullObj("Timeout.secondsPerInstance", ilpTimeout, Timeout::getNumberOfSecondsPerInstance)
                .putNonNullObj("Timeout.secondsPerTree", ilpTimeout, Timeout::getNumberOfSecondsPerDecomposition)

                .putNonNull("FormulaSettings.enforced", enforcedFormulaConstraints)
                .putNonNull("FormulaSettings.detectable", detectableElements, d -> String.join(",", d))
                .putNonNull("FormulaSettings.fallback", fallbackFormulaConstraints)

                .putNonNull("IsotopeMs2Settings", isotopeMs2Settings)
                .putNonNull("IsotopeSettings.filter", filterByIsotopePattern)

                .putNonNull("MS2MassDeviation.allowedMassDeviation", massAccuracyMS2ppm, it -> it + " ppm")

                .putNonNull("FormulaSearchSettings.performBottomUpAboveMz", (performBottomUpSearch==null || performBottomUpSearch==false) ? String.valueOf(Double.POSITIVE_INFINITY) : "0")
                .putNonNull("FormulaSearchSettings.performDeNovoBelowMz", (performDenovoBelowMz==null) ? "0" : String.valueOf(performDenovoBelowMz))
                .putNonNull("FormulaSearchDB", formulaSearchDBs, f -> String.join(",", f))

                .putNonNull("FormulaSearchSettings.applyFormulaConstraintsToBottomUp", applyFormulaConstraintsToDBAndBottomUpSearch)
                .putNonNull("FormulaSearchSettings.applyFormulaConstraintsToDatabaseCandidates", applyFormulaConstraintsToDBAndBottomUpSearch)

                .putNonNull("NumberOfCandidates", numberOfCandidates)
                .putNonNull("NumberOfCandidatesPerIonization", numberOfCandidatesPerIonization)
                .putNonNull("AlgorithmProfile", profile)

                .putNonNull("EnforceElGordoFormula", enforceElGordoFormula)
                .putNonNull("InjectSpectralLibraryMatchFormulas.minScoreToInject", minScoreToInjectSpecLibMatch)
                .putNonNullObj("InjectSpectralLibraryMatchFormulas.injectFormulas", minScoreToInjectSpecLibMatch, Objects::nonNull)
                .toUnmodifiableMap();
    }

    public static Sirius buildDefault() {
        return builderWithDefaults().build();
    }
    public static Sirius.SiriusBuilder<?,?> builderWithDefaults() {
        return Sirius.builder()
                .profile(Instrument.QTOF)
                .numberOfCandidates(PropertyManager.DEFAULTS.createInstanceWithDefaults(NumberOfCandidates.class).value)
                .numberOfCandidatesPerIonization(PropertyManager.DEFAULTS.createInstanceWithDefaults(NumberOfCandidatesPerIonization.class).value)
                .massAccuracyMS2ppm(PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.getPpm())
                .isotopeMs2Settings(PropertyManager.DEFAULTS.createInstanceWithDefaults(IsotopeMs2Settings.class).value)
                .filterByIsotopePattern(PropertyManager.DEFAULTS.createInstanceWithDefaults(IsotopeSettings.class).isFiltering())
                .performBottomUpSearch(true)
                .performDenovoBelowMz(PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSearchSettings.class).performDeNovoBelowMz)
                .formulaSearchDBs(List.of())
                .applyFormulaConstraintsToDBAndBottomUpSearch(false) //todo not 100% sure about that since in principle API allows to do all 3 at once, database + bottom up + denovo
                .enforcedFormulaConstraints(PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class).getEnforcedAlphabet().toString())
                .fallbackFormulaConstraints(PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class).getFallbackAlphabet().toString())
                .detectableElements(PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class).getAutoDetectionElements().stream().map(Element::getSymbol).collect(Collectors.toList()))
                .enforceElGordoFormula(PropertyManager.DEFAULTS.createInstanceWithDefaults(EnforceElGordoFormula.class).value)
                .minScoreToInjectSpecLibMatch(PropertyManager.DEFAULTS.createInstanceWithDefaults(InjectSpectralLibraryMatchFormulas.class).isInjectFormulas()
                        ? PropertyManager.DEFAULTS.createInstanceWithDefaults(InjectSpectralLibraryMatchFormulas.class).getMinScoreToInject() : null)
                .ilpTimeout(PropertyManager.DEFAULTS.createInstanceWithDefaults(Timeout.class))
                .useHeuristic(PropertyManager.DEFAULTS.createInstanceWithDefaults(UseHeuristic.class));
    }
}
