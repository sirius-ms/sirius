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

package de.unijena.bioinf.ms.stores.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.storage.blob.Compressible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface FingerIdClientDataStore extends Compressible {
    /**
     * Get fingerid client data aka statistics.tsv for the given predictor
     *
     * @param type Positive or negative predictor type
     * @return statistics.tsv
     */
    Optional<InputStream> getFingerIdClientData(PredictorType type) throws IOException;

    /**
     * get default scoring tree for the given predictor
     *
     * @param type Positive or negative predictor type
     * @return default scoring tree
     */
    Optional<InputStream> getBayesnetDefaultScoringTree(PredictorType type) throws IOException;

    /**
     * Get a formula specific scoring tree or empty if no specific tree exists
     *
     * @param type    Positive or negative predictor type
     * @param formula Formula for the tree
     * @return formula specific tree if it exists, empty otherwise
     */
    Optional<InputStream> getBayesnetScoringTree(PredictorType type, @Nullable MolecularFormula formula) throws IOException;

    boolean isBayesnetScoringTreeExcluded(@NotNull MolecularFormula formula) throws IOException;


    boolean hasBayesnetScoringTree(PredictorType type, @NotNull MolecularFormula formula) throws IOException;


    /**
     * get fingerprints statistics for default scoring tree for the given predictor
     *
     * @param type Positive or negative predictor type
     * @return fingerprints statistics for default scoring tree
     */
    Optional<InputStream> getBayesnetDefaultStats(PredictorType type) throws IOException;

    /**
     * Get formula specific fingerprints statistics for the corresponding scoring tree or empty if no specific tree exists
     *
     * @param type    Positive or negative predictor type
     * @param formula Formula for the tree
     * @return formula specific fingerprints statistics if corresponding tree exists, empty otherwise
     */
    Optional<InputStream> getBayesnetStats(PredictorType type, @Nullable MolecularFormula formula) throws IOException;


    /**
     * Get a formula specific scoring tree or default if not enough data is available for the specific tree
     *
     * @param type    Positive or negative predictor type
     * @param formula Formula for the tree
     * @return formula specific tree if it exists, default tree if formula is on exclusion list, NULL otherwise
     */
    default Optional<InputStream> getBayesnetScoringTreeOrDefault(PredictorType type, @Nullable MolecularFormula formula) throws IOException {
        if (formula == null || isBayesnetScoringTreeExcluded(formula))
            return getBayesnetDefaultScoringTree(type);

        return getBayesnetScoringTree(type, formula);
    }

    /**
     * Get confidence SVMs as JSON object for the given predictor type
     *
     * @param type Positive or negative predictor type
     * @return Set of confidence SVMs in JSON format
     */
    Optional<InputStream> getConfidenceSVMs(PredictorType type) throws IOException;

    /**
     * Get InChIs of fingerid trainings structures for the given predictor type
     *
     * @param type Positive or negative predictor type
     * @return List of InChIs in .tsv format
     *
     * Replaced by inchikey based json file
     */
    @Deprecated(forRemoval = true)
    Optional<InputStream> getFingerIdTrainingStructures(PredictorType type) throws IOException;

    /**
     * Get InChIKeys of fingerid trainings structures (kernel and dnn) for the given predictor type
     *
     * @param type Positive or negative predictor type
     * @return Two sets of InChIKey in .json format
     */
    Optional<InputStream> getFingerIdTrainingStructuresAll(PredictorType type) throws IOException;
}
