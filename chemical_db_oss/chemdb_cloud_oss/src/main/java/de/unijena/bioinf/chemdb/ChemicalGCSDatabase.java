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
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.gc.GCSUtils;
import de.unijena.bioinf.storage.blob.gcs.GCSBlobStorage;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/*
    A file based database based on Google cloud storage consists of a directory of files (either .csv or .json), each file contains compounds from the
    same molecular formula. The filenames consists of the molecular formula strings.
 */
public class ChemicalGCSDatabase extends ChemicalBlobDatabase<GCSBlobStorage> {

    public ChemicalGCSDatabase() throws IOException {
        this(FingerIDProperties.gcsChemDBBucketName());
    }

    public ChemicalGCSDatabase(String bucketName) throws IOException {
        this(USE_EXTENDED_FINGERPRINTS ? CdkFingerprintVersion.getExtended() : CdkFingerprintVersion.getDefault(), bucketName);
    }

    public ChemicalGCSDatabase(FingerprintVersion version, String bucketName) throws IOException {
        this(version, new GCSBlobStorage(GCSUtils.storageOptions(FingerIDProperties.gcsChemDBCredentialsPath()).getService().get(bucketName)));
    }

    public ChemicalGCSDatabase(FingerprintVersion version, GCSBlobStorage store) throws IOException {
        super(version, store);
    }


    @Override
    protected void init() throws IOException {
        format = storage.getLabel("chemdb-format").map(String::toUpperCase).map(Format::valueOf)
                .orElseThrow(() -> new IOException("Could not determine database file format."));
        compression = storage.getLabel("chemdb-compression").map(String::toUpperCase).map(Compression::valueOf)
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
                synchronized (formulaFlags){
                    this.formulaFlags.put(mf, flag);
                }
            });
            Arrays.sort(this.formulas);
        }
    }
}
