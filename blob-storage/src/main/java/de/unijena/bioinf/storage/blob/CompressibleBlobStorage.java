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

package de.unijena.bioinf.storage.blob;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

public class CompressibleBlobStorage<Storage extends BlobStorage> extends AbstractCompressible implements BlobStorage {

    protected final Storage rawStorage;


    public static <S extends BlobStorage> CompressibleBlobStorage<S> of(S rawStorage) {
        try {
            return of(rawStorage, Compression.valueOf(rawStorage.getTags().get(TAG_COMPRESSION)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <S extends BlobStorage> CompressibleBlobStorage<S> of(S rawStorage, Compression compression) {
        return new CompressibleBlobStorage<>(rawStorage, compression);
    }

    protected CompressibleBlobStorage(Storage rawStorage, Compression compression) {
        super(compression);
        setDecompressStreams(true);
        this.rawStorage = rawStorage;
    }

    @Override
    public String getName() {
        return rawStorage.getName();
    }

    @Override
    public String getBucketLocation() {
        return rawStorage.getBucketLocation();
    }

    @Override
    public boolean hasBlob(Path relative) throws IOException {
        return rawStorage.hasBlob(addExt(relative));
    }

    public boolean hasRawBlob(Path relative) throws IOException {
        return rawStorage.hasBlob(relative);
    }

    @Override
    public boolean deleteBlob(Path relative) throws IOException {
        return rawStorage.deleteBlob(relative);
    }

    @Override
    public void withWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException {
        rawStorage.withWriter(addExt(relative), (out) ->
                Compressible.withCompression(out, getCompression(), withStream));
    }

    public void withRawWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException {
        rawStorage.withWriter(relative, withStream);
    }

    @Override
    public InputStream reader(Path relative) throws IOException {
        return Compressible.decompressRawStream(rawStorage.reader(addExt(relative)), compression, decompressStreams).orElse(null);
    }

    public InputStream rawReader(Path relative) throws IOException {
        return rawStorage.reader(relative);
    }

    @Override
    public @NotNull Map<String, String> getTags() throws IOException {
        return rawStorage.getTags();
    }

    @Override
    public void setTags(@NotNull Map<String, String> tags) throws IOException {
        rawStorage.setTags(tags);
    }

    @Override
    public Iterator<Blob> listBlobs() throws IOException {
        return rawStorage.listBlobs();
    }

    @Override
    public void deleteBucket() throws IOException {
        rawStorage.deleteBucket();
    }

    public Storage getRawStorage() {
        return rawStorage;
    }
}