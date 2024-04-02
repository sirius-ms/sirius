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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
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
    Optional<String> getProvidedFeatureId();

    /**
     * @return Display name of this feature
     */
    String getName();

    @Override
    String toString();

    double getIonMass();

    ProjectSpaceManager getProjectSpaceManager();

    //region load from projectSpace
    Ms2Experiment getExperiment();

    LCMSPeakInformation getLCMSPeakInformation();
    List<FCandidate<?>> getFormulaCandidates();

    List<SpectralSearchResult.SearchResult> getSpectraMatches();

    List<FCandidate<?>> getFTrees();
    List<FCandidate<?>> getCanopusInput();
    List<FCandidate<?>> getMsNovelistInput();
    List<FCandidate<?>> getFingerblastInput();


    Optional<FCandidate<?>> getTopFormulaCandidate();
    Optional<FCandidate<?>> getTopPredictions();
    Optional<FCandidate<?>> getTopFTree();


    @Nullable ParameterConfig loadInputFileConfig();

    @Nullable ParameterConfig loadProjectConfig();

    void updateConfig(@NotNull ParameterConfig config);

    //region clear cache
    void clearCompoundCache();

    default void enableComputing() {
        setComputing(true);
    }

    default void disableComputing() {
        setComputing(false);
    }


    void setComputing(boolean computing);

    boolean isComputing();

    void saveDetectedAdducts(DetectedAdducts detectedAdducts);

    Optional<DetectedAdducts> getDetectedAdducts();

    default void deleteDetectedAdducts(){
        saveDetectedAdducts(null);
    }


    boolean saveSpectraSearchResult(SpectralSearchResult result);
    boolean hasSpectraSearchResult();
    void deleteSpectraSearchResult();

    boolean savePassatuttoResult(FCandidate<?> id, Decoy decoy);
    boolean hasPassatuttoResult();
    void deletePassatuttoResult();


    void saveSiriusResult(List<FTree> treesSortedByScore);
    boolean hasSiriusResult();
    void deleteSiriusResult();

    void saveZodiacResult(Map<FCandidate<?>, ZodiacScore> zodiacScores);
    boolean hasZodiacResult();
    void deleteZodiacResult();

    void saveFingerprintResult(@NotNull Map<FCandidate<?>, FingerprintResult> fingerprintResultsByFormula);
    boolean hasFingerprintResult();
    void deleteFingerprintResult();

    //TODO TEMP solution -> make better api method without FormulaResult but with Id
    void saveStructureSearchResult(@NotNull Map<FCandidate<?>, FingerIdResult> structureSearchResults);
    boolean hasStructureSearchResult();
    void deleteStructureSearchResult();

    void saveCanopusResult(@NotNull List<FCandidate<?>> canopusResults);
    boolean hasCanopusResult();
    void deleteCanopusResult();

    void saveMsNovelistResult(@NotNull Map<FCandidate<?>, FingerIdResult> CanopusResultsByFormula);
    boolean hasMsNovelistResult();
    void deleteMsNovelistResult();
}
