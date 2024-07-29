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
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Super simple object reading/writing API
 */
public interface BlobStorage extends Closeable, AutoCloseable {

    @Override
    default void close() throws IOException {
    }

    default Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    String getName();

    String getBucketLocation();

    default long size() throws IOException {
        AtomicLong size = new AtomicLong(size());
        listBlobs().forEachRemaining(blob -> {
            if (!blob.isDirectory())
                size.addAndGet(blob.size());
        });
        return size.get();
    }

    boolean hasBlob(Path relative) throws IOException;

    boolean deleteBlob(Path relative) throws IOException;

    default void clear() throws IOException {
        Iterator<Blob> it = listBlobs();
        while (it.hasNext())
            deleteBlob(Path.of(it.next().getKey()));
    }

    /**
     * Applies given function to a writer for the given path
     * in the store and closes it after writing
     *
     * @param relative   relative path from storage root
     * @param withStream consume OutputStream to write data
     */

    void withWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException;


    /**
     * Returns the raw unmodified byte stream from the store.
     *
     * @param relative relative path from storage root
     * @return raw unmodified byte stream
     */
    InputStream reader(Path relative) throws IOException;

    /**
     * Returns the Tag on storage/bucket level for the given key
     *
     * @param key associated with the requested value
     * @return tag corresponding to the given key or NULL if key does not exist or
     * if the storage/bucket does not support tags.
     */
    @Nullable
    default String getTag(@NotNull String key) throws IOException {
        return getTags().get(key);
    }

    /**
     * Get tags associated with this storage/bucket
     * @return map containing key value pairs
     */
    @NotNull
    Map<String, String> getTags() throws IOException;


    /**
     * Set a tag with the given key to the storage/bucket
     * Might throw {@link UnsupportedOperationException} if storage/bucket does not support tags
     * @param key key to be associated with the tag
     * @param value value of the tag
     */
    default String setTag(@NotNull String key, @Nullable String value) throws IOException {
        @NotNull Map<String, String> tags = getTags();
        String r = (value == null || value.isBlank()) ? tags.remove(key) : tags.put(key, value);
        setTags(tags);
        return r;
    }

    /**
     * Set the tags associated with this storage/bucket
     * Might throw {@link UnsupportedOperationException} if storage/bucket does not support tags
     */
    void setTags(@NotNull Map<String, String> tags) throws IOException;

    /**
     * Remove a tag with the given key from the storage/bucket
     * Might throw {@link UnsupportedOperationException} if storage/bucket does not support tags
     * @param key key of the tag to be removed
     */
    default void removeTag(@NotNull String key) throws IOException {
        setTag(key, null);
    }

    Iterator<Blob> listBlobs() throws IOException;

    /**
     * delete bucket and close client if necessary
     */
    void deleteBucket() throws IOException;

    interface Blob {
        boolean isDirectory();

        String getKey();

        default String getFileName() {
            return Path.of(getKey()).getFileName().toString();
        }

        long size();
    }

    class BlobIt<S> implements Iterator<Blob>{
        final Iterator<S> sourceIt;
        final IOFunctions.IOFunction<S,Blob> blobConv;

        public BlobIt(Iterable<S> sourceIterable, IOFunctions.IOFunction<S, Blob> blobConv) {
            this(sourceIterable.iterator(), blobConv);
        }
        public BlobIt(Iterator<S> sourceIterator, IOFunctions.IOFunction<S, Blob> blobConv) {
            this.sourceIt = sourceIterator;
            this.blobConv = blobConv;
        }

        @Override
        public boolean hasNext() {
            return sourceIt.hasNext();
        }

        @Override
        public Blob next() {
            try {
                return blobConv.apply(sourceIt.next());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}