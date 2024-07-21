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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationTable;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface Instance {
    /**
     * @return The ID (primary key) of this aligned feature (usaully alignedFeatureId).
     */
    String getId();

    /**
     * @return Optional Compound this Instance belongs to (adduct group)
     */
    Optional<String> getCompoundId();

    /**
     * @return FeatureId provided from some external preprocessing tool
     */
    Optional<String> getExternalFeatureId();

    /**
     * @return Display name of this feature
     */
    String getName();

    Optional<RetentionTime> getRT();

    double getIonMass();

    PrecursorIonType getIonType();

    ProjectSpaceManager getProjectSpaceManager();

    Ms2Experiment getExperiment();

    boolean hasMs1();
    boolean hasMsMs();

    Optional<QuantificationTable> getQuantificationTable();
    List<FCandidate<?>> getFormulaCandidates();

    List<SpectralSearchResult.SearchResult> getSpectraMatches();

    List<FCandidate<?>> getFTrees();
    List<FCandidate<?>> getCanopusInput();
    List<FCandidate<?>> getMsNovelistInput();
    List<FCandidate<?>> getFingerblastInput();


    Optional<FCandidate<?>> getTopFormulaCandidate();
    Optional<FCandidate<?>> getTopPredictions();
    Optional<FCandidate<?>> getTopFTree();

    Optional<ParameterConfig> loadInputFileConfig();
    Optional<ParameterConfig> loadProjectConfig();

    void updateProjectConfig(@NotNull ParameterConfig config);


    //region state
    void clearCompoundCache();
    boolean isRecompute();
    default void enableRecompute() {
        setRecompute(true);
    }
    default void disableRecompute() {
        setRecompute(false);
    }
    void setRecompute(boolean recompute);
    //endregion

    //region adduct detection
    boolean hasDetectedAdducts();
    @Deprecated
    void saveDetectedAdductsAnnotation(DetectedAdducts detectedAdducts);
    void saveDetectedAdducts(de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts detectedAdducts);
    @Deprecated
    DetectedAdducts getDetectedAdductsAnnotation();
    de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts getDetectedAdducts();

    default void deleteDetectedAdducts(){
        saveDetectedAdducts((de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts) null);
    }
    //endregion

    void saveSpectraSearchResult(SpectralSearchResult result);
    boolean hasSpectraSearchResult();
    void deleteSpectraSearchResult();

    void savePassatuttoResult(FCandidate<?> id, Decoy decoy);
    boolean hasPassatuttoResult();
    void deletePassatuttoResult();


    void saveSiriusResult(List<FTree> treesSortedByScore);
    boolean hasSiriusResult();
    void deleteSiriusResult();

    void saveZodiacResult(List<FCandidate<?>> zodiacScores);
    boolean hasZodiacResult();
    void deleteZodiacResult();

    void saveFingerprintResult(@NotNull List<FCandidate<?>> fingerprintResultsPerFormula);
    boolean hasFingerprintResult();
    void deleteFingerprintResult();

    void saveStructureSearchResult(@NotNull List<FCandidate<?>> structureSearchResultsPerFormula);
    boolean hasStructureSearchResult();
    void deleteStructureSearchResult();

    void saveCanopusResult(@NotNull List<FCandidate<?>> canopusResultsPerFormula);
    boolean hasCanopusResult();
    void deleteCanopusResult();

    void saveMsNovelistResult(@NotNull List<FCandidate<?>> msNovelistResultsPerFormula);
    boolean hasMsNovelistResult();
    void deleteMsNovelistResult();
}
