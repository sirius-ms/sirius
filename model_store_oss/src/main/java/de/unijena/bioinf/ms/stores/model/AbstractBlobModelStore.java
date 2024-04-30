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
import de.unijena.bioinf.storage.blob.AbstractCompressible;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.Compressible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;

public abstract class AbstractBlobModelStore<Storage extends BlobStorage> extends AbstractCompressible implements ModelStore<Path> {

    protected final Storage blobStorage;

    protected AbstractBlobModelStore(@NotNull Storage blobStorage, @NotNull Compression compression) {
        super(compression);
        this.blobStorage = blobStorage;
    }

    public Storage getBlobStorage() {
        return blobStorage;
    }

    @Override
    public Charset getCharset() {
        return blobStorage.getCharset();
    }

    protected String makeFormulaName(@NotNull final MolecularFormula formula, @Nullable String extension) {
        if (extension == null)
            return formula.toString();
        return formula.toString() + "." + extension;
    }

    protected Path resolve(@NotNull final PredictorType predictorType) {
        return Path.of(predictorType.isPositive() ? "pos" : "neg");
    }

    public @Nullable InputStream getRawResource(@NotNull Path path, boolean autoExtHandling) throws IOException {
        return blobStorage.reader(autoExtHandling ? addExt(path) : path);
    }

    @Override
    public @Nullable InputStream getRawResource(@NotNull Path path) throws IOException {
        return getRawResource(path, true);
    }

    @Override
    public boolean hasResource(@NotNull Path path) throws IOException {
        return blobStorage.hasBlob(addExt(path));
    }


    @Override
    public @Nullable InputStream getRawResource(@NotNull Path path, @NotNull PredictorType predictorType) throws IOException {
        return getRawResource(path, predictorType, true);
    }

    public @Nullable InputStream getRawResource(@NotNull Path path, @NotNull PredictorType predictorType, boolean autoExtHandling) throws IOException {
        return getRawResource(resolve(predictorType).resolve(path), autoExtHandling);
    }

    @Override
    public @Nullable InputStream getRawResource(@NotNull Path path, @NotNull PredictorType predictorType, @NotNull MolecularFormula formula) throws IOException {
        return getRawResource(resolve(predictorType).resolve(path), predictorType, formula, true);
    }

    public @Nullable InputStream getRawResource(@NotNull Path path, @NotNull PredictorType predictorType, @NotNull MolecularFormula formula, boolean autoExtHandling) throws IOException {
        return getRawResource(path, predictorType, formula, null, autoExtHandling);
    }


    public @Nullable InputStream getRawResource(@NotNull Path path, @NotNull PredictorType predictorType, @NotNull MolecularFormula formula, @Nullable String ext) throws IOException {
        return getRawResource(path, predictorType, formula, ext, true);
    }

    public @Nullable InputStream getRawResource(@NotNull Path path, @NotNull PredictorType predictorType, @NotNull MolecularFormula formula, @Nullable String ext, boolean autoExtHandling) throws IOException {
        return getRawResource(resolve(predictorType).resolve(path).resolve(makeFormulaName(formula,ext)), autoExtHandling);
    }

    @NotNull
    public Optional<InputStream> getResource(@NotNull final Path path, @NotNull final PredictorType predictorType, @NotNull MolecularFormula formula, @Nullable String ext) throws IOException {
        return Compressible.decompressRawStream(getRawResource(path, predictorType, formula, ext), getCompression(), isDecompressStreams());
    }

    @Override
    public boolean hasResource(@NotNull Path path, @NotNull PredictorType predictorType) throws IOException {
        return hasResource(resolve(predictorType).resolve(path));
    }

    @Override
    public boolean hasResource(@NotNull Path path, @NotNull PredictorType predictorType, @NotNull MolecularFormula formula) throws IOException {
        return hasResource(path, predictorType, formula, null);

    }

    protected boolean hasResource(@NotNull Path path, @NotNull PredictorType predictorType, @NotNull MolecularFormula formula, @Nullable String ext) throws IOException {
        return hasResource(resolve(predictorType).resolve(path).resolve(makeFormulaName(formula, ext)));
    }

    public void writeResource(@NotNull final Path path, @NotNull IOFunctions.IOConsumer<OutputStream> streamConsumer) throws IOException {
        blobStorage.withWriter(addExt(path), (out) ->
                Compressible.withCompression(out, getCompression(), streamConsumer));
    }

    public void writeResource(@NotNull final Path path, @NotNull final PredictorType predictorType, @NotNull IOFunctions.IOConsumer<OutputStream> streamConsumer) throws IOException {
        writeResource(resolve(predictorType).resolve(path), streamConsumer);
    }

    public void writeResource(@NotNull final Path path, @NotNull final PredictorType predictorType, @NotNull MolecularFormula formula, @NotNull IOFunctions.IOConsumer<OutputStream> streamConsumer) throws IOException {
        writeResource(path, predictorType, formula, null, streamConsumer);
    }

    protected void writeResource(@NotNull Path path, @NotNull PredictorType predictorType, @NotNull MolecularFormula formula, @Nullable String ext, @NotNull IOFunctions.IOConsumer<OutputStream> streamConsumer) throws IOException {
        writeResource(resolve(predictorType).resolve(path).resolve(makeFormulaName(formula, ext)), streamConsumer);
    }
}
