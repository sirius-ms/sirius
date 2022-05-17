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

package de.unijena.bioinf.storage.blob.memory;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.storage.blob.BlobStorage;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBlobStorage implements BlobStorage {

    private final String name;
    protected final Map<String, byte[]> blobs = new ConcurrentHashMap<>();
    protected Map<String, String> tags = new ConcurrentHashMap<>();

    public InMemoryBlobStorage(String name) {
        this.name = name;
    }

    protected byte[] get(@NotNull String key){
        return blobs.get(key);
    }

    protected byte[] put(String key, byte[] value){
        return blobs.put(key, value);
    }

    protected byte[] remove(String key){
        return blobs.remove(key);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBucketLocation() {
        return getName();
    }

    @Override
    public boolean hasBlob(Path relative) throws IOException {
        return blobs.containsKey(relative.toString());
    }

    @Override
    public void withWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException {
        try (ByteArrayOutputStream w = new ByteArrayOutputStream()) {
            withStream.accept(w);
            put(relative.toString(), w.toByteArray());
        }
    }

    @Override
    public InputStream reader(Path relative) throws IOException {
        byte[] blob = get(relative.toString());
        if (blob == null)
            throw new IOException("Path '" + relative + "' does not exist in InMemory BlobStorage '" + getName() + "'!");
        return new ByteArrayInputStream(blob);
    }

    @Override
    public @NotNull Map<String, String> getTags() throws IOException {
        return Collections.unmodifiableMap(tags);
    }

    @Override
    public void setTags(@NotNull Map<String, String> tags) throws IOException {
        this.tags = new ConcurrentHashMap<>(tags);
    }

    @Override
    public Iterator<Blob> listBlobs() {
        return new BlobIt<>(blobs.keySet().iterator(), MemBlob::new);
    }

    @Override
    public boolean deleteBlob(Path relative) {
        return remove(relative.toString()) != null;
    }

    @Override
    public void deleteBucket() {
        blobs.clear();
        tags.clear();
    }

    public class MemBlob implements Blob {
        final String key;

        private MemBlob(@NotNull String key) {
            this.key = key;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public long size() {
            return blobs.get(key).length;
        }
    }
}
