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

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.BlobStorages;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static de.unijena.bioinf.storage.blob.Compressible.TAG_COMPRESSION;

public class ModelStores {
    public static final String PROPERTY_PREFIX = "de.unijena.bioinf.stores.model";

    public static DefaultBlobModelStore<?> openDefaultStore() throws IOException {
        return openDefaultStore((String) null);
    }

    public static DefaultBlobModelStore<?> openDefaultStore(@Nullable String path) throws IOException {
        if (path == null || path.isBlank())
            path = PropertyManager.getProperty(PROPERTY_PREFIX + ".bucket");

        return openDefaultStore(BlobStorages.openDefault(PROPERTY_PREFIX, path));
    }

    public static <BS extends BlobStorage> DefaultBlobModelStore<BS> openDefaultStore(@NotNull BS storage) throws IOException {
        if (storage instanceof FileBlobStorage)
            return new DefaultBlobModelStore<>(storage, FileBlobStorage.detectCompression(((FileBlobStorage) storage).getRoot()));

        String c = storage.getTag(TAG_COMPRESSION);
        return new DefaultBlobModelStore<>(storage, c != null ? Compressible.Compression.valueOf(c.toUpperCase()) : Compressible.Compression.GZIP);
    }


    public static DefaultBlobModelStore<?> createDefaultStore(@NotNull String path) throws IOException {
        return createDefaultStore(path, Compressible.Compression.GZIP);
    }

    public static DefaultBlobModelStore<?> createDefaultStore(@NotNull String path, @NotNull Compressible.Compression compression) throws IOException {
        BlobStorage s = BlobStorages.createDefault(PROPERTY_PREFIX, path);

        final Map<String, String> tags = new HashMap<>(s.getTags());
        tags.put(TAG_COMPRESSION, compression.name().toLowerCase());
        s.setTags(tags);

        return new DefaultBlobModelStore<>(s, compression);
    }

    public static boolean exists(String outputPath) throws IOException {
        return BlobStorages.exists(PROPERTY_PREFIX, outputPath);
    }
}
