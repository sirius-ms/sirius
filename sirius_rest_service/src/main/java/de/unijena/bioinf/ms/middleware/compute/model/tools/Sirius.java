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

package de.unijena.bioinf.ms.middleware.compute.model.tools;


import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidatesPerIon;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.IsotopeMs2Settings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Timeout;
import de.unijena.bioinf.FragmentationTreeConstruction.model.UseHeuristic;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User/developer friendly parameter subset for the Formula/SIRIUS tool
 */

@Getter
@Setter
public class Sirius extends Tool<SiriusOptions> {
    enum Instrument {QTOF, ORBI, FTICR}

    /**
     * Instrument specific profile for internal algorithms
     * Just select what comes closest to the instrument that was used for measuring the data.
     */
    Instrument profile;
    /**
     * Number of formula candidates to keep as result list (Formula Candidates).
     */
    int numberOfCandidates;
    /**
     * Use this parameter if you want to force SIRIUS to report at least
     * NumberOfCandidatesPerIon results per ionization.
     * if <= 0, this parameter will have no effect and just the top
     * NumberOfCandidates results will be reported.
     */
    int numberOfCandidatesPerIon;
    /**
     * Maximum allowed mass accuracy. Only molecular formulas within this mass window are considered.
     */
    double massAccuracyMS2ppm;

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
     * List Structure database to extract molecular formulas from to reduce formula search space.
     * SIRIUS is quite good at de novo formula annotation, so only enable if you have a good reason.
     */
    List<DataSource> formulaSearchDBs;

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

    /**
     * Timout settings for the ILP solver used for fragmentation tree computation
     * secondsPerInstance: Set the maximum number of seconds for computing a single compound. Set to 0 to disable the time constraint.
     * secondsPerTree: Set the maximum number of seconds for a single molecular formula check. Set to 0 to disable the time constraint
     */
    Timeout ilpTimeout;
    /**
     * Mass thresholds for heuristic fragmentation tree computation which dramatically speeds up computations.
     * mzToUseHeuristic: For compounds above this threshold fragmentation trees will be computed heuristically for ranking. Tree that will be kept (numberOfCandidates) will be recomputed exactly
     * mzToUseHeuristicOnly:For compounds above this threshold fragmentation trees will be computed heuristically.
     */
    UseHeuristic useHeuristic;


    public Sirius() {
        super(SiriusOptions.class);
        profile = Instrument.QTOF;
        numberOfCandidates = PropertyManager.DEFAULTS.createInstanceWithDefaults(NumberOfCandidates.class).value;
        numberOfCandidatesPerIon = PropertyManager.DEFAULTS.createInstanceWithDefaults(NumberOfCandidatesPerIon.class).value;
        massAccuracyMS2ppm = PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.getPpm();
        isotopeMs2Settings = PropertyManager.DEFAULTS.createInstanceWithDefaults(IsotopeMs2Settings.class).value;
        formulaSearchDBs = List.of();
        FormulaSettings settings = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class);
        enforcedFormulaConstraints = settings.getEnforcedAlphabet().toString();
        fallbackFormulaConstraints = settings.getFallbackAlphabet().toString();
        detectableElements = settings.getAutoDetectionElements().stream().map(Element::getSymbol).collect(Collectors.toList());

        ilpTimeout = PropertyManager.DEFAULTS.createInstanceWithDefaults(Timeout.class);
        useHeuristic = PropertyManager.DEFAULTS.createInstanceWithDefaults(UseHeuristic.class);
    }

    @JsonIgnore
    @Override
    public Map<String, String> asConfigMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put("UseHeuristic.mzToUseHeuristic", String.valueOf(useHeuristic.mzToUseHeuristic));
        map.put("UseHeuristic.mzToUseHeuristicOnly", String.valueOf(useHeuristic.mzToUseHeuristicOnly));

        map.put("Timeout.secondsPerInstance", String.valueOf(ilpTimeout.getNumberOfSecondsPerInstance()));
        map.put("Timeout.secondsPerTree", String.valueOf(ilpTimeout.getNumberOfSecondsPerDecomposition()));

        map.put("FormulaSettings.enforced", enforcedFormulaConstraints);
        map.put("FormulaSettings.detectable", String.join(",", detectableElements));
        map.put("FormulaSettings.fallback", fallbackFormulaConstraints);

        map.put("IsotopeMs2Settings", isotopeMs2Settings.name());

        map.put("FormulaSearchDB", formulaSearchDBs.stream().map(DataSource::name).collect(Collectors.joining(",")));

        map.put("NumberOfCandidates", String.valueOf(numberOfCandidates));
        map.put("NumberOfCandidatesPerIon", String.valueOf(numberOfCandidatesPerIon));
        map.put("AlgorithmProfile", profile.name());
        return Collections.unmodifiableMap(map);
    }
}
