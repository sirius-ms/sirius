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

package de.unijena.bioinf.storage.blob.s3;

import de.unijena.bioinf.aws.AWSUtils;
import de.unijena.bioinf.storage.blob.BlobStorage;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

//todo finish Amazon S3 Storage
public class S3BlobStorage implements BlobStorage {

    // Init
    private String bucketName;
    private S3Client client;


    public S3BlobStorage(@NotNull String bucketName, @NotNull String url, @NotNull AwsCredentials credentials) {
        this(bucketName, AWSUtils.storageOptions(url, credentials));
    }

    public S3BlobStorage(@NotNull String bucketName, S3Client client) {
        this.client = client;
        this.bucketName = bucketName;
        init();
    }

    private void init() {
        //todo check if this is efficient
        if (client.listBuckets().buckets().stream().noneMatch(b ->  b.name().equals(bucketName)))
            throw new IllegalArgumentException("Database bucket seems to be not existent or you have not the correct permissions");
    }

    @Override
    public String getName() {
        return bucketName;
    }

    @Override
    public boolean hasBlob(Path relative) throws IOException {
        return hasBlob(relative.toString());
    }

    public boolean hasBlob(String key) throws IOException {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public OutputStream writer(Path relative) throws IOException {
        /*PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName).key(relative.toString()).contentEncoding(StandardCharsets.UTF_8.name())
                .build();
        RequestBody body = RequestBody.fromContentProvider(new ContentStreamProvider() {
            @Override
            public InputStream newStream() {
                return null;
            }
        });
        return client.putObject(request,body);*/
        return null;
    }

    @Override
    public InputStream reader(Path relative) throws IOException {
        return null;
    }
}
