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


import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidatesPerIon;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.IsotopeMs2Settings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Timeout;
import de.unijena.bioinf.FragmentationTreeConstruction.model.UseHeuristic;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User/developer friendly parameter subset for the Formula/SIRIUS tool
 */

@Getter
@Setter
public class Sirius {
    enum Instrument { QTOF, ORBI, FTICR}

    /**
     * Instrument specific profile for internal algorithms
     * Just select what comes closest to the instrument that was used for measuring the data.
     */
    Instrument profile;
    /**
     * Number of formula candidates to keep as result list (Formula Candidates).
     */
    NumberOfCandidates numberOfCandidates;
    /**
     * Use this parameter if you want to force SIRIUS to report at least
     * NumberOfCandidatesPerIon results per ionization.
     * if <= 0, this parameter will have no effect and just the top
     * NumberOfCandidates results will be reported.
     * */
    NumberOfCandidatesPerIon numberOfCandidatesPerIon;
    /**
     * Maximum allowed mass accuracy. Only molecular formulas within this mass window are considered.
     */
    double massAccuracyMS2ppm;

    /**
     * Specify how isotope patterns in MS/MS should be handled.
     *
     * FILTER: When filtering is enabled, molecular formulas are excluded if their
     * theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.
     *
     * SCORE: Use them for SCORING. To use this the instrument should produce clear MS/MS isotope patterns
     *
     * IGNORE: Ignore that there might be isotope patterns in MS/MS
     */
    IsotopeMs2Settings isotopeSettings;

    /**
     * List Structure database to extract molecular formulas from to reduce formula search space.
     * SIRIUS is quite good at de novo formula annotation, so only enable if you have a good reason.
     */
    List<DataSource> formulaSearchDBs;

    /**
     * These configurations hold the information how to autodetect elements based on the given formula constraints.
     * Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.
     *
     * Enforced: Enforced elements are always considered
     */
    String enforcedFormulaConstraints;

    /**
     * These configurations hold the information how to autodetect elements based on the given formula constraints.
     * Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.
     *
     * Fallback: Fallback elements are used, if the auto-detection fails (e.g. no isotope pattern available)
     */
    String fallbackFormulaConstraints;

    /**
     * These configurations hold the information how to autodetect elements based on the given formula constraints.
     * Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.
     *
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
        profile = Instrument.QTOF;
        numberOfCandidates = PropertyManager.DEFAULTS.createInstanceWithDefaults(NumberOfCandidates.class);
        numberOfCandidatesPerIon = PropertyManager.DEFAULTS.createInstanceWithDefaults(NumberOfCandidatesPerIon.class);
        massAccuracyMS2ppm = PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.getPpm();
        isotopeSettings = PropertyManager.DEFAULTS.createInstanceWithDefaults(IsotopeMs2Settings.class);
        formulaSearchDBs = List.of();
        FormulaSettings settings  = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class);
        enforcedFormulaConstraints = settings.getEnforcedAlphabet().toString();
        fallbackFormulaConstraints = settings.getFallbackAlphabet().toString();
        detectableElements = settings.getAutoDetectionElements().stream().map(Element::getSymbol).collect(Collectors.toList());

        ilpTimeout = PropertyManager.DEFAULTS.createInstanceWithDefaults(Timeout.class);
        useHeuristic = PropertyManager.DEFAULTS.createInstanceWithDefaults(UseHeuristic.class);
    }
}
