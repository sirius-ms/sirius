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
import de.unijena.bioinf.storage.blob.Compressible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Optional;

public interface ModelStore<ResourceID> extends Compressible {
    Logger LOGGER = LoggerFactory.getLogger(ModelStore.class);


    Charset getCharset();

    @NotNull
    default Optional<InputStream> getResource(@NotNull final ResourceID id) throws IOException {
        return Compressible.decompressRawStream(getRawResource(id), getCompression(), isDecompressStreams());
    }

    @Nullable
    InputStream getRawResource(@NotNull final ResourceID id) throws IOException;


    @NotNull
    default Optional<InputStream> getResource(@NotNull final ResourceID id, @NotNull final PredictorType predictorType) throws IOException {
        return Compressible.decompressRawStream(getRawResource(id, predictorType), getCompression(), isDecompressStreams());
    }

    @Nullable
    InputStream getRawResource(@NotNull final ResourceID id, @NotNull final PredictorType predictorType) throws IOException;


    @NotNull
    default Optional<InputStream> getResource(@NotNull final ResourceID id, @NotNull final PredictorType predictorType, @NotNull MolecularFormula formula) throws IOException {
        return Compressible.decompressRawStream(getRawResource(id, predictorType, formula), getCompression(), isDecompressStreams());
    }

    @Nullable
    InputStream getRawResource(@NotNull final ResourceID id, @NotNull final PredictorType predictorType, @NotNull MolecularFormula formula) throws IOException;


    boolean hasResource(@NotNull final ResourceID id) throws IOException;

    boolean hasResource(@NotNull final ResourceID id, @NotNull final PredictorType predictorType) throws IOException;

    boolean hasResource(@NotNull final ResourceID id, @NotNull final PredictorType predictorType, @NotNull MolecularFormula formula) throws IOException;


    void writeResource(@NotNull final ResourceID id, @NotNull IOFunctions.IOConsumer<OutputStream> streamConsumer) throws IOException;

    void writeResource(@NotNull final ResourceID id, @NotNull final PredictorType predictorType, @NotNull IOFunctions.IOConsumer<OutputStream> streamConsumer) throws IOException;

    void writeResource(@NotNull final ResourceID id, @NotNull final PredictorType predictorType, @NotNull MolecularFormula formula, @NotNull IOFunctions.IOConsumer<OutputStream> streamConsumer) throws IOException;
}
