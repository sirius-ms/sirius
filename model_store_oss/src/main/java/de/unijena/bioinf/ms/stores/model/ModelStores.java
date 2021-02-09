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
import de.unijena.bioinf.storage.blob.gcs.GCSBlobStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ModelStores {

    public static DefaultBlobModelStore<?> openDefaultStore() throws IOException {
        return openDefaultStore((String) null);
    }

    public static DefaultBlobModelStore<?> openDefaultStore(@Nullable String path) throws IOException {
        if (path == null || path.isBlank())
            path = PropertyManager.getProperty("de.unijena.bioinf.ms.stores.model.path");

        return openDefaultStore(BlobStorages.openDefault(path, getDefaultGCCredentials()));
    }

    public static <BS extends BlobStorage> DefaultBlobModelStore<BS> openDefaultStore(@NotNull BS storage) throws IOException {
        if (storage instanceof GCSBlobStorage) {
            String c = ((GCSBlobStorage) storage).getBucket().getLabels().get("modelstore-compression");
            return new DefaultBlobModelStore<>(storage, c != null ? Compressible.Compression.valueOf(c.toUpperCase()) : Compressible.Compression.NONE);
        }

        if (storage instanceof FileBlobStorage)
            return new DefaultBlobModelStore<>(storage, FileBlobStorage.detectCompression(((FileBlobStorage) storage).getRoot()));

        LoggerFactory.getLogger(ModelStores.class).warn("Unknown blob storage type: " + storage.getClass().getSimpleName());

        return new DefaultBlobModelStore<>(storage);
    }


    public static DefaultBlobModelStore<?> createDefaultStore(@NotNull String path) throws IOException {
        return createDefaultStore(path, Compressible.Compression.GZIP);
    }

    public static DefaultBlobModelStore<?> createDefaultStore(@NotNull String path, @NotNull Compressible.Compression compression) throws IOException {
        BlobStorage s = BlobStorages.createDefault(path, getDefaultGCCredentials());

        if (s instanceof GCSBlobStorage) {
            final Map<String, String> labels = new HashMap<>();
            labels.put("modelstore-name", s.getName());
            labels.put("modelstore-compression", compression.name().toLowerCase());
            ((GCSBlobStorage) s).updateBucket(b -> b.setLabels(labels));
        }

        return new DefaultBlobModelStore<>(s,compression);
    }

    /*
     */
    public static boolean exists(String outputPath) throws IOException {
        return BlobStorages.exists(outputPath, getDefaultGCCredentials());
    }

    public static Path getDefaultGCCredentials() {
        return Path.of(System.getProperty("user.home")).resolve(PropertyManager.getProperty("de.unijena.bioinf.ms.stores.model.gcs.credentials"));
    }
}
