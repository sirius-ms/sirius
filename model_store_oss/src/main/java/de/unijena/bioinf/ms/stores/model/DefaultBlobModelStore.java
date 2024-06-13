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
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.Compressible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;

public class DefaultBlobModelStore<Storage extends BlobStorage> extends AbstractBlobModelStore<Storage> implements MsNovelistDataStore, CanopusDataStore, FingerIdDataStore {

    public DefaultBlobModelStore(@NotNull Storage blobStorage) {
        this(blobStorage, Compression.GZIP);
    }
    public DefaultBlobModelStore(@NotNull Storage blobStorage, @NotNull Compressible.Compression compression) {
        super(blobStorage, compression);
    }

    @Override
    public Optional<InputStream> getMsNovelistData(PredictorType type) throws IOException {
        return Optional.ofNullable(getRawResource(Path.of("msnovelist-weights.hdf5"), type, false));
    }

    //todo insert if we have a real pos/neg model
//    @Override
//    public Optional<InputStream> getCanopusFastData() throws IOException {
//        return getResource(Path.of("canopus-fast.data"));
//    }

    @Override
    public Optional<InputStream> getCanopusFastData(PredictorType type) throws IOException {
        return getResource(Path.of("canopus-fast.data"), type);
    }

    @Override
    public Optional<InputStream> getCanopusCfClientData(PredictorType type) throws IOException {
        return getResource(Path.of("canopusCfClientData.tsv"), type);
    }

    @Override
    public Optional<InputStream> getCanopusNpcClientData(PredictorType type) throws IOException {
        return getResource(Path.of("canopusNpcClientData.tsv"), type);
    }

    @Override
    public Optional<InputStream> getFingerIdData(PredictorType type) throws IOException {
        return getResource(Path.of("fingerid.data"), type);
    }

    @Override
    public Optional<InputStream> getFingerIdFastData(PredictorType type) throws IOException {
        return getResource(Path.of("fingerid-fast.data"), type);
    }

    @Override
    public Optional<InputStream> getFingerIdClientData(PredictorType type) throws IOException {
        return getResource(Path.of("fingeridClientData.tsv"), type);
    }

    @Override
    public Optional<InputStream> getPredictedFPsTrainingData(PredictorType type) throws IOException {
        return getResource(Path.of("predictedFPs.data"), type);
    }

    @Override
    public Optional<InputStream> getBayesnetDefaultScoringTree(PredictorType type) throws IOException {
        return getResource(Path.of("bayesnetScoring").resolve("trees").resolve("default.tsv"), type);
    }

    @Override
    public Optional<InputStream> getBayesnetScoringTree(PredictorType type, @Nullable MolecularFormula formula) throws IOException {
        if (formula == null)
            return getBayesnetDefaultScoringTree(type);
        return getResource(Path.of("bayesnetScoring").resolve("trees"), type, formula, "tsv");

    }

    @Override
    public boolean isBayesnetScoringTreeExcluded(@NotNull MolecularFormula formula) throws IOException {
        return hasResource(Path.of("bayesnetScoring").resolve("exclusions2"), PredictorType.CSI_FINGERID_POSITIVE, formula, null);
    }

    @Override
    public boolean hasBayesnetScoringTree(PredictorType type, @NotNull MolecularFormula formula) throws IOException {
        return hasResource(Path.of("bayesnetScoring").resolve("trees"), type, formula, "tsv");
    }

    @Override
    public Optional<InputStream> getBayesnetDefaultStats(PredictorType type) throws IOException {
        return getResource(Path.of("bayesnetScoring").resolve("stats").resolve("default.json"), type);
    }

    @Override
    public Optional<InputStream> getBayesnetStats(PredictorType type, @Nullable MolecularFormula formula) throws IOException {
        if (formula == null)
            return getBayesnetDefaultStats(type);
        return getResource(Path.of("bayesnetScoring").resolve("stats"), type, formula, "json");
    }

    @Override
    public Optional<InputStream> getConfidenceSVMs(PredictorType type) throws IOException {
        return getResource(Path.of("confidence.json"), type);

    }

    @Override
    public Optional<InputStream> getFingerIdTrainingStructures(PredictorType type) throws IOException {
        return getResource(Path.of("trainingStructures.tsv"), type);
    }

    @Override
    public Optional<InputStream> getFingerIdTrainingStructuresAll(PredictorType type) throws IOException {
        return getResource(Path.of("trainingStructuresAll.json"), type);
    }

    @Override
    public void writeBayesnetScoringTree(@NotNull PredictorType type, @Nullable MolecularFormula formula, IOFunctions.IOConsumer<OutputStream> consume) throws IOException {
        if (formula == null)
            writeResource(Path.of("bayesnetScoring").resolve("trees").resolve("default.tsv"), type, consume);
        else
            writeResource(Path.of("bayesnetScoring").resolve("trees"), type, formula, "tsv", consume);
    }

    @Override
    public void writeBayesnetScoringStats(@NotNull PredictorType type, @Nullable MolecularFormula formula, IOFunctions.IOConsumer<OutputStream> consume) throws IOException {
        if (formula == null)
            writeResource(Path.of("bayesnetScoring").resolve("stats").resolve("default.json"), type, consume);
        else
            writeResource(Path.of("bayesnetScoring").resolve("stats"), type, formula, "json", consume);
    }

    @Override
    public void addToExclusions(@NotNull MolecularFormula formula) throws IOException {
        writeResource(Path.of("bayesnetScoring").resolve("exclusions2"), PredictorType.CSI_FINGERID_POSITIVE, formula, null, (w) ->{});
    }
}
