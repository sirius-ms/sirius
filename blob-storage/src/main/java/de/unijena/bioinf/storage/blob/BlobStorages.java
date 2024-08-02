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

import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import de.unijena.bioinf.storage.blob.minio.MinIoUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlobStorages {
    public static boolean exists(@Nullable String propertyPrefix, @NotNull String bucketPath) throws IOException {
        if (!bucketPath.isBlank()) {
            if (bucketPath.startsWith(MinIoUtils.URL_PREFIX))
                return MinIoUtils.existsS3Bucket(propertyPrefix,bucketPath.substring(MinIoUtils.URL_PREFIX.length()).split("/")[0]);
            return FileBlobStorage.exists(Path.of(bucketPath));
        }
        throw new IOException("Unsupported Blob storage location `" + bucketPath + "`.");
    }


    public static BlobStorage openDefault(@Nullable String propertyPrefix, @NotNull String bucketPath) throws IOException  {
        if (!bucketPath.isBlank()) {
            if (bucketPath.startsWith(MinIoUtils.URL_PREFIX)){
                if (propertyPrefix ==  null || propertyPrefix.isBlank())
                    throw new IOException("Property prefix need to be given for generic S3 storages!");
                return MinIoUtils.openDefaultS3Storage(propertyPrefix, bucketPath.substring(MinIoUtils.URL_PREFIX.length()).split("/")[0]);
            }
            if (Files.isDirectory(Path.of(bucketPath)))
                return new FileBlobStorage(Path.of(bucketPath));
        }
        throw new IOException("Unsupported Blob storage location or location does not exist: '" + bucketPath + "'.");
    }


    public static BlobStorage createDefault(@Nullable String propertyPrefix, @NotNull String bucketPath) throws IOException {
        if (!bucketPath.isBlank()) {
            if (bucketPath.startsWith(MinIoUtils.URL_PREFIX))
                throw new UnsupportedOperationException("S3 storage location creation not yet supported");
            return createDefaultFileStore(bucketPath);
        }
        throw new IOException("Unsupported Blob storage location `" + bucketPath + "`.");
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
        throw new IOException("Illegal Storage location `" + p + "`! Must either not exist or be an empty directory");
    }
}
