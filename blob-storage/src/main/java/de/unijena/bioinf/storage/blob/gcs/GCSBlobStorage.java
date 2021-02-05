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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageException;
import de.unijena.bioinf.gc.GCSUtils;
import de.unijena.bioinf.storage.blob.BlobStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class GCSBlobStorage implements BlobStorage {

    // Init
    private final Bucket bucket;
    protected Map<String, String> bucketLabels;


    public GCSBlobStorage(String bucketName, Path credentials) {
        this(GCSUtils.storageOptions(credentials).getService().get(bucketName));
    }

    public GCSBlobStorage(Bucket bucket) {
        this.bucket = bucket;
        init();
    }

    private void init() {
        if (!bucket.exists())
            throw new IllegalArgumentException("Database bucket seems to be not existent or you have not the correct permissions");
        bucketLabels = Collections.unmodifiableMap(bucket.getLabels());
    }


    public Optional<String> getLabel(String key){
        return Optional.ofNullable(bucketLabels.get(key));
    }

    //API
    protected @Nullable Blob getBlob(@NotNull Path path) throws StorageException {
        Blob blob = bucket.get(path.toString());
        if (blob == null || !blob.exists())
            return null;
        return blob;
    }

    @Override
    public @Nullable InputStream reader(@NotNull Path path) throws IOException {
        try {
            Blob blob = bucket.get(path.toString());
            if (blob == null || !blob.exists())
                return null;
            return Channels.newInputStream(blob.reader());
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getName() {
        return bucket.getName();
    }

    @Override
    public boolean hasBlob(@NotNull Path path) throws IOException {
        try {
            return getBlob(path) != null;
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }


    @Override
    public OutputStream writer(Path relative) throws IOException {
        try  {
            return Channels.newOutputStream(bucket.create(relative.toString(), (byte[]) null).writer());
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }
}
