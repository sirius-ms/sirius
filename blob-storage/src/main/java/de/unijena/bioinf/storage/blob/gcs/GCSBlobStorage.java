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

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageException;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.storage.blob.BlobStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class GCSBlobStorage implements BlobStorage {

    // Init
    private Bucket bucket;


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
    }

    public Bucket getBucket() {
        return bucket;
    }

    public void updateBucket(Consumer<Bucket.Builder> update) {
        final Bucket.Builder b = bucket.toBuilder();
        update.accept(b);
        bucket = b.build().update();
    }

    //API
    protected com.google.cloud.storage.Blob getBlob(@NotNull Path path) throws StorageException {
        com.google.cloud.storage.Blob blob = bucket.get(path.toString());
        if (blob == null || !blob.exists())
            return null;
        return blob;
    }

    @Override
    public @Nullable InputStream reader(@NotNull Path path) throws IOException {
        try {
            com.google.cloud.storage.Blob blob = bucket.get(path.toString());
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
    public String getBucketLocation() {
        return GCSUtils.URL_PREFIX + getName();
    }

    @Override
    public boolean hasBlob(@NotNull Path path) throws IOException {
        try {
            return getBlob(path) != null;
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }


    protected OutputStream writer(Path relative) throws IOException {
        try  {
            return Channels.newOutputStream(bucket.create(relative.toString(), (byte[]) null).writer());
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void withWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException {
        try (OutputStream w = writer(relative)) {
            withStream.accept(w);
        }
    }

    @Override
    public Iterator<Blob> listBlobs() {
        return new BlobIt<>(bucket.list().iterateAll(), GCSBlob::new);
    }


    @Override
    public boolean deleteBlob(Path relative) {
        com.google.cloud.storage.Blob b = getBlob(relative);
        if (b == null)
            return false;
        return b.delete();
    }

    @Override
    public void deleteBucket() throws IOException {
        bucket.delete();
        close();
    }

    @Override
    public @NotNull Map<String, String> getTags() {
        return bucket.getLabels();
    }

    @Override
    public void setTags(@NotNull Map<String, String> tags) {
        updateBucket(b -> b.setLabels(tags));
    }

    public static class GCSBlob implements Blob {
        final com.google.cloud.storage.Blob source;

        private GCSBlob(com.google.cloud.storage.Blob source) {
            this.source = source;
        }

        @Override
        public boolean isDirectory() {
            return source.isDirectory();
        }

        @Override
        public String getKey() {
            return source.getName();
        }

        @Override
        public long size() {
            return source.getSize();
        }

        public static GCSBlob of(@NotNull com.google.cloud.storage.Blob source) {
            return new GCSBlob(source);
        }
    }
}
