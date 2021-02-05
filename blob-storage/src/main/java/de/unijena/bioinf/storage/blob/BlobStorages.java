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

import com.google.cloud.storage.*;
import de.unijena.bioinf.gc.GCSUtils;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import de.unijena.bioinf.storage.blob.gcs.GCSBlobStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BlobStorages {

    public static boolean exists(@Nullable String path, Path credentials) throws IOException {
        if (path != null && !path.isBlank()) {
            if (path.startsWith(GCSUtils.URL_PREFIX))
                return GCSUtils.bucketExists(path.substring(5).split("/")[0], credentials);

            return FileBlobStorage.exists(Path.of(path));
        }
        throw new IOException("Unsupported Model storage location `" + path + "`.");
    }


    public static BlobStorage openDefault(@Nullable String path, Path credentials) {
        if (path != null && !path.isBlank()) {
            if (path.startsWith(GCSUtils.URL_PREFIX)) {
                return new GCSBlobStorage(path.substring(5).split("/")[0], credentials);
            }

            if (Files.isDirectory(Path.of(path)))
                return new FileBlobStorage(Path.of(path));
        }
        throw new IllegalArgumentException("Unsupported Model storage location");
    }

    public static BlobStorage createDefault(@NotNull String path, Path credentials) throws IOException {
        return createDefault(path, Compressible.Compression.GZIP, credentials);
    }

    public static BlobStorage createDefault(@NotNull String path, @NotNull Compressible.Compression compression, Path credentials) throws IOException {
        if (!path.isBlank()) {
            if (path.startsWith(GCSUtils.URL_PREFIX))
                return createDefaultGCS(path.substring(5).split("/")[0], compression, credentials);
            return createDefaultFileStore(path);
        }
        throw new IOException("Unsupported Model storage location `" + path + "`.");
    }

    public static BlobStorage createDefaultGCS(@NotNull String name, @NotNull Compressible.Compression compression, Path credentials) throws IOException {
        return createDefaultGCS(name, null, compression, credentials);
    }

    public static BlobStorage createDefaultGCS(@NotNull String name, @Nullable String flavor, @NotNull Compressible.Compression compression, Path credentials)
            throws IOException {
        try {
            final Map<String, String> labels = new HashMap<>();
            labels.put("modelstore-name", name);
            if (flavor != null)
                labels.put("modelstore-flavor", flavor);
            labels.put("modelstore-compression", compression.name().toLowerCase());
            StorageOptions opts = GCSUtils.storageOptions(credentials);
            Bucket b = opts.getService().create(BucketInfo.newBuilder(name)
                    .setStorageClass(StorageClass.STANDARD)
                    .setLocation("EU")
                    .setLabels(labels)
                    .build());
            return new GCSBlobStorage(b);
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }


    public static FileBlobStorage createDefaultFileStore(@NotNull String path) throws IOException {
        return createDefaultFileStore(Path.of(path));
    }

    public static FileBlobStorage createDefaultFileStore(@NotNull Path p) throws IOException {
        if (Files.notExists(p))
            Files.createDirectories(p);
        if (!FileBlobStorage.exists(p)) {
            return new FileBlobStorage(p);
        }
        throw new IOException("Illegal Storage location `" + p.toString() + "`! Must either not exist or be an empty directory");
    }
}
