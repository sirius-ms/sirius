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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.storage.blob.gcs.GCSBlobStorage;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class CachedChemicalGCSDatabase extends ChemicalGCSDatabase {

    protected final ChemDBFileCache cache;
    protected final long filter;

    public CachedChemicalGCSDatabase(@Nullable File cacheDir, long filter) throws IOException {
        super();
        this.cache = makeCache(cacheDir);
        this.filter = filter;
    }

    public CachedChemicalGCSDatabase(String bucketName, @Nullable File cacheDir, long filter) throws IOException {
        super(bucketName);
        this.cache = makeCache(cacheDir);
        this.filter = filter;
    }

    public CachedChemicalGCSDatabase(FingerprintVersion version, String bucketName, @Nullable File cacheDir, long filter) throws IOException {
        super(version, bucketName);
        this.cache = makeCache(cacheDir);
        this.filter = filter;
    }

    public CachedChemicalGCSDatabase(FingerprintVersion version, GCSBlobStorage store, @Nullable File cacheDir, long filter) throws IOException {
        super(version, store);
        this.cache = makeCache(cacheDir);
        this.filter = filter;
    }

    public ChemDBFileCache makeCache(File cacheDir) {
        return new ChemDBFileCache(cacheDir, CachedChemicalGCSDatabase.super::lookupStructuresAndFingerprintsByFormula);
    }


    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        fingerprintCandidates.addAll(cache.lookupStructuresAndFingerprintsByFormula(formula));
        return fingerprintCandidates;
    }
}
