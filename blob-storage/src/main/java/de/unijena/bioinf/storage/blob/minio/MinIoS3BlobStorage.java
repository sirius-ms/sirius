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

package de.unijena.bioinf.storage.blob.minio;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.storage.blob.BlobStorage;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MinIoS3BlobStorage implements BlobStorage {

    private final MinioClient minioClient;
    private final String bucketName;


    public MinIoS3BlobStorage(@NotNull String bucketName, @NotNull MinioClient client) {
        this.minioClient = client;
        this.bucketName = bucketName;
        init();
        if (PropertyManager.getBoolean("de.unijena.bioinf.stores.minio.trace", null, false))
            minioClient.traceOn(System.out);
    }

    private void init() {
        if (!MinIoUtils.existsS3Bucket(bucketName, minioClient))
            throw new IllegalArgumentException("Database bucket seems to be not existent or you have not the correct permissions");
    }

    @Override
    public String getName() {
        return bucketName;
    }

    @Override
    public String getBucketLocation() {
        return MinIoUtils.URL_PREFIX + getName();
    }

    @Override
    public boolean hasBlob(Path relative) throws IOException {
        try {
            return minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(makePath(relative)).build()) != null;
        } catch (ErrorResponseException e) {
            return exists(e);
        } catch (InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new IOException("Error when searching object", e);
        }
    }

    @Override
    public void withWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException {
        try (OutputStream w = writer(relative)) {
            withStream.accept(w);
        }
    }

    public OutputStream writer(Path relative) throws IOException {
        return BackgroundPipedOutputStream.createAndRead((in) -> {
            try {
                return minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(makePath(relative)).stream(in, -1, 100 * 1024 * 1024).build());
            } catch (ErrorResponseException | InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
                throw new IOException(e);
            }
        });
    }

    @Override
    public InputStream reader(Path relative) throws IOException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(makePath(relative)).build());
        } catch (ErrorResponseException e) {
            exists(e);
            return null;
        } catch (InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new IOException("Error when Searching Object", e);
        }
    }

    private String makePath(Path relative) {
        return StreamSupport.stream(relative.spliterator(), false)
                .map(Path::toString)
                .collect(Collectors.joining("/"));
    }

    private static class BackgroundPipedOutputStream<R> extends PipedOutputStream {
        private TinyBackgroundJJob<R> readJob;

        public BackgroundPipedOutputStream() {
            super();
        }

        public void readInBackground(IOFunctions.IOFunction<PipedInputStream, R> readSink) throws IOException {
            final PipedInputStream in = new PipedInputStream(this);
            readJob = SiriusJobs.runInBackground(() -> {
                try {
                    return readSink.apply(in);
                } finally {
                    in.close();
                }
            });
        }

        public R awaitReadInBackground() throws ExecutionException {
            if (readJob == null)
                return null;
            return readJob.awaitResult();
        }

        @Override
        public void close() throws IOException {
            super.close();
            try {
                awaitReadInBackground();
            } catch (ExecutionException e) {
                throw new IOException(e);
            }
        }

        public static <R> BackgroundPipedOutputStream<R> createAndRead(IOFunctions.IOFunction<PipedInputStream, R> readSink) throws IOException {
            BackgroundPipedOutputStream<R> s = new BackgroundPipedOutputStream<>();
            s.readInBackground(readSink);
            return s;
        }
    }

    private boolean exists(ErrorResponseException e) throws IOException {
        String code = e.errorResponse().code();
        if (code != null && (code.equals("NoSuchKey") || code.equals("NoSuchObject"))) {
            // Not found
            return false;
        } else {
            throw new IOException("Unknown Error response when searching Object", e);
        }
    }

    @Override
    public @NotNull Map<String, String> getTags() throws IOException {
        try {
            return minioClient.getBucketTags(GetBucketTagsArgs.builder().bucket(bucketName).build()).get();
        } catch (ErrorResponseException e) {
            exists(e);
            LoggerFactory.getLogger(getClass()).warn("Error Response when requesting tags. No tags returned!", e);
            return Collections.emptyMap();
        } catch (InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new IOException("Error when requesting Tags", e);
        }
    }

    @Override
    public void setTags(@NotNull Map<String, String> tags) throws IOException {
        try {
            minioClient.setBucketTags(SetBucketTagsArgs.builder()
                    .bucket(bucketName)
                    .tags(tags)
                    .build());
        } catch (ErrorResponseException | InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new IOException("Error when writing Tags", e);
        }
    }

    @Override
    public Iterator<Blob> listBlobs() {
        Iterator<Result<Item>> it = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build()).iterator();
        return new BlobIt<>(it, MinIoBlob::of);
    }

    @Override
    public boolean deleteBlob(Path relative) throws IOException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(makePath(relative)).build());
            return true;
        } catch (ErrorResponseException e) {
            exists(e);
            return false;
        } catch (InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new IOException("Error when Searching Object", e);
        }
    }

    @Override
    public void deleteBucket() throws IOException {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            close();
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new IOException("Error when deleting Bucket", e);
        }
    }

    public static class MinIoBlob implements Blob {
        final Item source;

        private MinIoBlob(@NotNull Result<Item> sourceResult) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
            this(sourceResult.get());
        }
        private MinIoBlob(@NotNull Item source) {
            this.source = source;
        }

        @Override
        public boolean isDirectory() {
            return source.isDir();
        }

        @Override
        public String getKey() {
            return source.objectName();
        }

        @Override
        public long size() {
            return source.size();
        }

        public static MinIoBlob of(@NotNull Item source){
            return new MinIoBlob(source);
        }

        public static MinIoBlob of(@NotNull Result<Item> sourceResult) throws IOException {
            try {
                return new MinIoBlob(sourceResult);
            } catch (ErrorResponseException | InvalidResponseException | IOException | InsufficientDataException | InternalException | InvalidKeyException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
                throw new IOException("Error when writing Tags", e);
            }
        }
    }
}
