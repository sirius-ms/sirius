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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageOptions;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.gc.GCSUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/*
    A file based database based on Google cloud storage consists of a directory of files (either .csv or .json), each file contains compounds from the
    same molecular formula. The filenames consists of the molecular formula strings.
 */
public class GCSDatabase extends AbstractBlobBasedDatabase {

    private final Bucket bucket;
    protected Map<String, String> bucketLabels;

    public GCSDatabase() throws IOException {
        this(FingerIDProperties.gcsChemDBBucketName());
    }

    public GCSDatabase(String bucketName) throws IOException {
        this(USE_EXTENDED_FINGERPRINTS ? CdkFingerprintVersion.getExtended() : CdkFingerprintVersion.getDefault(), bucketName);
    }

    public GCSDatabase(FingerprintVersion version, String bucketName) throws IOException {
        this(version, GCSUtils.storageOptions(FingerIDProperties.gcsChemDBCredentialsPath()).getService().get(bucketName));
    }

    public GCSDatabase(FingerprintVersion version, Bucket bucket) throws IOException {
        super(version, bucket.getName());
        this.bucket = bucket;
        refresh();
    }


    @Override
    protected void refresh() throws IOException {
        if (!bucket.exists())
            throw new IOException("Database bucket seems to be not existent or you have not the correct permissions");
        bucketLabels = bucket.getLabels();

        if (bucketLabels.containsKey("chemdb-format"))
            format = "." + bucketLabels.get("chemdb-format").replace('-', '.').toUpperCase(Locale.US);
        long time = System.currentTimeMillis();
        System.out.println("before");
        if (format == null) {
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
//                final String form = name.substring(0, name.length() - format.length());
//                MolecularFormula.parseAndExecute(form, formulas::add);

                if (format != null)
                    break;
            }
        }

        if (format == null) throw new IOException("Couldn't find any compounds in given database");

        format = format.toLowerCase();
        this.reader = format.equals(".json") || format.equals(".json.gz") ? new JSONReader() : new CSVReader();
        this.compressed = format.endsWith(".gz");

//        System.out.println("start parsing after: " + ((double) (System.currentTimeMillis() - time)) / 1000d);

        try (Reader r = getReader("formulas").orElseThrow(() -> new IOException("Formula index not found!"))) {
            final Map<String, String> map = new ObjectMapper().readValue(r, new TypeReference<Map<String, String>>() {
            });
//            System.out.println("stop parsing after: " + ((double) (System.currentTimeMillis() - time)) / 1000d);

            this.formulas = new MolecularFormula[map.size()];
            final AtomicInteger i = new AtomicInteger(0);
            formulaFlags.clear();
            map.entrySet().stream().parallel().forEach(e -> {
                final MolecularFormula mf = MolecularFormula.parseOrThrow(e.getKey());
                final long flag = Long.parseLong(e.getValue());
                this.formulas[i.getAndIncrement()] = mf;
                synchronized (formulaFlags){
                    this.formulaFlags.put(mf, flag);
                }
            });
//            System.out.println("Filled map after: " + ((double) (System.currentTimeMillis() - time)) / 1000d);
            Arrays.sort(this.formulas);
//            System.out.println("Sorted after: " + ((double)(System.currentTimeMillis() - time))/1000d);
        }
    }



    @Override
    public @NotNull Optional<Reader> getReader(@NotNull String name) throws IOException {
        if (compressed) {
            return getStream(name).map(inputStream -> new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } else {
            return getByteChannel(name).map(s -> Channels.newReader(s, StandardCharsets.UTF_8));
        }
    }

    /**
     * Returns stream for the filename without handling decompression
     *
     * @param name resource name
     * @return Optional of Stream of the resource
     */
    @Override
    public @NotNull Optional<InputStream> getRawStream(@NotNull String name) {
        return getByteChannel(name).map(Channels::newInputStream);
    }

    public Optional<ReadableByteChannel> getByteChannel(@NotNull String name) {
        Blob blob = bucket.get(name + format);
        if (blob == null || !blob.exists())
            return Optional.empty();

        return Optional.of(blob.reader());
    }
}
