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
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public interface FingerIdDataStore extends FingerIdClientDataStore {
    /**
     * Get fingerid-fast.data file for the given predictor
     *
     * @param type Positive or negative predictor type
     * @return fingerid-fast.data in binary format
     */
    Optional<InputStream> getFingerIdFastData(PredictorType type) throws IOException;

    /**
     * Get fingerid.data file for the given predictor
     *
     * @param type Positive or negative predictor type
     * @return fingerid.data in binary format
     */
   Optional<InputStream> getFingerIdData(PredictorType type) throws IOException;


    /**
     * Get predicted fingerprints for bayesnet scoring tree computation for given predictor type
     *
     * @param type Positive or negative predictor type
     * @return Predicted fingerprints for training data.
     */
    Optional<InputStream> getPredictedFPsTrainingData(PredictorType type) throws IOException;

    /**
     * Write a bayesnet tree for given PredictorType and formula ino the model Storage
     * @param type Positive or negative predictor type. If null default tree will be written
     * @param formula formula for which the tree should be added.
     */
    void writeBayesnetScoringTree(@NotNull PredictorType type, @Nullable MolecularFormula formula, IOFunctions.IOConsumer<OutputStream> consume) throws IOException;

    /**
     * Write fingerprint statistics for given PredictorType and formula into the model Storage
     * @param type Positive or negative predictor type. If null default tree will be written
     * @param formula formula for which the tree should be added.
     */
    void writeBayesnetScoringStats(@NotNull PredictorType type, @Nullable MolecularFormula formula, IOFunctions.IOConsumer<OutputStream> consume) throws IOException;

    /**
     * Add the given molecular formula to the bayesnet scoring tree exclusion list
     * @param formula formula for which the tree should be added.
     */
    void addToExclusions(@NotNull MolecularFormula formula) throws IOException;
}
