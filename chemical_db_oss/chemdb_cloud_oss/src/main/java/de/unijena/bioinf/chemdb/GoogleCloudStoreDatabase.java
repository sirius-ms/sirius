/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageOptions;
import com.sun.istack.Nullable;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/*
    A file based database based on Google cloud storage consists of a directory of files (either .csv or .json), each file contains compounds from the
    same molecular formula. The filenames consists of the molecular formula strings.
 */
public class GoogleCloudStoreDatabase extends AbstractBlobBasedDatabase {

    private final Bucket bucket;
    protected Map<String,String> bucketLabels;

    public GoogleCloudStoreDatabase() throws IOException {
        this(FingerIDProperties.gcsBucketName());
    }
    public GoogleCloudStoreDatabase(String bucketName) throws IOException {
        this(USE_EXTENDED_FINGERPRINTS ? CdkFingerprintVersion.getExtended() : CdkFingerprintVersion.getDefault(), bucketName);
    }
    public GoogleCloudStoreDatabase(FingerprintVersion version, String bucketName) throws IOException {
        this(version, storageOptions().getService().get(bucketName));
    }

    public GoogleCloudStoreDatabase(FingerprintVersion version, Bucket bucket) throws IOException {
        super(version, bucket.getName());
        this.bucket = bucket;
        refresh();
    }

    private static StorageOptions storageOptions(){
        try {
            FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(
                    ServiceAccountCredentials.fromStream(Files.newInputStream(FingerIDProperties.gcsCredentialsPath())));
            return StorageOptions.newBuilder().setCredentials(credentialsProvider.getCredentials()).build();
        } catch (IOException e) {
            throw new RuntimeException("Could not found google cloud credentials json at: " + FingerIDProperties.gcsCredentialsPath());
        }
    }


    @Override
    protected void refresh() throws IOException {

        final ArrayList<MolecularFormula> formulas = new ArrayList<>();
        if (!bucket.exists())
            throw new IOException("Database bucket seems to be not existent or you have not the correct permissions");
        bucketLabels = bucket.getLabels();
        format = "." + bucketLabels.get("chemdb-format").replace('-','.').toUpperCase(Locale.US);

        final Iterable<Blob> blobs = bucket.list().iterateAll();
        for (Blob b : blobs) {
            final String name = b.getName();
            final String upName = name.toUpperCase(Locale.US);

            for (String s : SUPPORTED_FORMATS) {
                if (upName.endsWith(s)) {
                    if (format == null || format.equals(s)) {
                        format = s;
                        break;
                    } else {
                        throw new IOException("Database contains several formats. Only one format is allowed! Given format is " + format + " but " + name + " found.");
                    }
                }
            }

            final String form = name.substring(0, name.length() - format.length());
            MolecularFormula.parseAndExecute(form, formulas::add);
        }
        if (format == null) throw new IOException("Couldn't find any compounds in given database");
        format = format.toLowerCase();
        this.reader = format.equals(".json") || format.equals(".json.gz") ? new JSONReader() : new CSVReader();
        this.compressed = format.endsWith(".gz");
        this.formulas = formulas.toArray(MolecularFormula[]::new);
        Arrays.sort(this.formulas);
    }

    @Override
    @Nullable
    public Reader getReaderFor(MolecularFormula formula) throws IOException {
        Blob blob = bucket.get(formula.toString() + format);
        if (blob == null || !blob.exists())
            return null;

        if (compressed) {
            return new InputStreamReader(new GZIPInputStream(Channels.newInputStream(blob.reader())), StandardCharsets.UTF_8);
        } else {
            return Channels.newReader(blob.reader(), StandardCharsets.UTF_8);
        }
    }
}
