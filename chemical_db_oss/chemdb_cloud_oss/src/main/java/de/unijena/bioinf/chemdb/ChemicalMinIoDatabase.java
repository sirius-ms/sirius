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
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.storage.blob.minio.MinIoS3BlobStorage;
import de.unijena.bioinf.storage.blob.minio.MinIoUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/*
    A file based database based on Google cloud storage consists of a directory of files (either .csv or .json), each file contains compounds from the
    same molecular formula. The filenames consists of the molecular formula strings.
 */
public class ChemicalMinIoDatabase extends ChemicalBlobDatabase<MinIoS3BlobStorage> {

    public ChemicalMinIoDatabase() throws IOException {
        this(PropertyManager.getProperty("de.unijena.bioinf.chemdb.s3.bucket"));
    }

    public ChemicalMinIoDatabase(String bucketName) throws IOException {
        this(USE_EXTENDED_FINGERPRINTS ? CdkFingerprintVersion.getExtended() : CdkFingerprintVersion.getDefault(), bucketName);
    }

    public ChemicalMinIoDatabase(FingerprintVersion version, String bucketName) throws IOException {
        this(version, MinIoUtils.openDefaultS3Storage("de.unijena.bioinf.chemdb",bucketName));
    }

    public ChemicalMinIoDatabase(FingerprintVersion version, MinIoS3BlobStorage store) throws IOException {
        super(version, store);
    }


    @Override
    protected void init() throws IOException {
        Map<String, String> labels = storage.getTags();

        format = Optional.ofNullable(labels.get("chemdb-format")).map(String::toUpperCase).map(Format::valueOf)
                .orElseThrow(() -> new IOException("Could not determine database file format."));
        compression = Optional.ofNullable(labels.get("chemdb-compression")).map(String::toUpperCase).map(Compression::valueOf)
                .orElseGet(() -> {
                    LoggerFactory.getLogger(getClass()).warn("Could not determine compressions type. Assuming uncompressed data!");
                    return Compression.NONE;
                });

        this.reader = format == Format.CSV ? new CSVReader() : new JSONReader();

        LoggerFactory.getLogger(getClass()).debug("Loading molecular formulas to memory...");
        try (Reader r = getReader("formulas").orElseThrow(() -> new IOException("Formula index not found!"))) {
            final Map<String, String> map = new ObjectMapper().readValue(r, new TypeReference<Map<String, String>>() {
            });

            this.formulas = new MolecularFormula[map.size()];
            final AtomicInteger i = new AtomicInteger(0);
            formulaFlags.clear();
            map.entrySet().stream().parallel().forEach(e -> {
                final MolecularFormula mf = MolecularFormula.parseOrThrow(e.getKey());
                final long flag = Long.parseLong(e.getValue());
                this.formulas[i.getAndIncrement()] = mf;
                synchronized (formulaFlags) {
                    this.formulaFlags.put(mf, flag);
                }
            });
            Arrays.sort(this.formulas);
        }
    }
}
