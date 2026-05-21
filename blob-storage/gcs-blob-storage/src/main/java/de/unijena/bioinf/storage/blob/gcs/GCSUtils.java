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

package de.unijena.bioinf.storage.blob.gcs;


import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.storage.blob.BlobStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class GCSUtils {
    public static final String URL_PREFIX = "gc://";

    public static StorageOptions storageOptions(@Nullable Path credentials) {
        if (credentials == null)
            return StorageOptions.getDefaultInstance();

        try (InputStream stream = Files.newInputStream(credentials)) {
            return StorageOptions.newBuilder().setCredentials(ServiceAccountCredentials
                    .fromStream((PropertyManager.isB64Credentials() ? Base64.getDecoder().wrap(stream) : stream))).build();
        } catch (IOException e) {
            throw new RuntimeException("Could not find google cloud credentials json at: " + credentials, e);
        }
    }

    public static boolean bucketExists(@Nullable String propertyPrefix, String name) {
        return bucketExists(name, tryGetGCCredentials(propertyPrefix));
    }

    public static boolean bucketExists(String name, @Nullable Path credentials) {
        final Bucket b = storageOptions(credentials).getService().get(name);
        return b != null && b.exists();
    }


    public static GCSBlobStorage openDefaultGCStorage(@Nullable String propertyPrefix, @NotNull String bucketName) {
        return new GCSBlobStorage(bucketName, tryGetGCCredentials(propertyPrefix));
    }

    public static BlobStorage createDefaultGCS(@Nullable String propertyPrefix, @NotNull String name) throws IOException {
        try {

            StorageOptions opts = GCSUtils.storageOptions(tryGetGCCredentials(propertyPrefix));
            Bucket b = opts.getService().create(BucketInfo.newBuilder(name)
                    .setStorageClass(StorageClass.STANDARD)
                    .setLocation("EU")
                    .build());
            return new GCSBlobStorage(b);
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Nullable
    public static Path tryGetGCCredentials(@Nullable String propertyPrefix) {
        String key = (propertyPrefix == null || propertyPrefix.isBlank() ? "" : propertyPrefix + ".") + "gcs.credentials";
        Path p = PropertyManager.getPath(key);
        if (p == null)
            return null;

        if (p.isAbsolute())
            return p;
        return Path.of(System.getProperty("user.home")).resolve(p);
    }
}
