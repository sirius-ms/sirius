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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

/**
 * Create a new read-cached {@link BlobStorage} from a source and a cache storage.
 * {@link ReadCachedBlobStorage} are READ_ONLY storages.
 * The cache storage should be significantly faster to access to have any positive effect on performance.
 *
 * Tags are already cached by most BlobStorage implementations.
 * So that the cache storage will neither contains any tags nor cache the tags of the source storage
 *
 * Blob Listing
 * @param <Source> Blob storage to be cached (Slower)
 * @param <Cache> Blob storage used as the cache (Faster)
 */
public class ReadCachedBlobStorage<Source extends BlobStorage, Cache extends BlobStorage> implements BlobStorage {

    private final Source source;
    private final Cache cache;

    public ReadCachedBlobStorage(Source source, Cache cache) {
        this.source = source;
        this.cache = cache;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public String getBucketLocation() {
        return source.getBucketLocation();
    }

    @Override
    public boolean hasBlob(Path relative) throws IOException {
        if (cache.hasBlob(relative)) {
            return true;
        } else {
            return source.hasBlob(relative);
        }
    }

    @Override
    public boolean deleteBlob(Path relative) throws IOException {
        throw new UnsupportedOperationException("Read Cached BlobStorages do not support delete operations.");
    }

    @Override
    public void withWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException {
        throw new UnsupportedOperationException("Read Cached BlobStorages do not support write operations.");
    }

    @Override
    public InputStream reader(Path relative) throws IOException {
        if (cache.hasBlob(relative))
            return cache.reader(relative);
        final byte[] buffer = source.reader(relative).readAllBytes();

        SiriusJobs.runInBackground(() -> {
            cache.withWriter(relative, (w) -> w.write(buffer));
            return true;
        });

        return new ByteArrayInputStream(buffer);
    }

    @Override
    public @NotNull Map<String, String> getTags() throws IOException {
        return source.getTags();
    }

    @Override
    public void setTags(@NotNull Map<String, String> tags) throws IOException {
        source.setTags(tags);
    }

    @Override
    public Iterator<Blob> listBlobs() throws IOException {
        return source.listBlobs();
    }

    @Override
    public void deleteBucket() throws IOException {
        throw new UnsupportedOperationException("Read Cached BlobStorages cannot be deleted. Please delete the Source and Cache store separately");
    }
}
