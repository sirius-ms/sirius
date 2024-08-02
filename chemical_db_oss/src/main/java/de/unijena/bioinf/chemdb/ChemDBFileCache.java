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
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.storage.blob.AbstractCompressible;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import de.unijena.bioinf.storage.blob.memory.InMemoryBlobStorage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class to read-cache any kind of {@link SearchStructureByFormula} using a {@link BlobStorage}.
 * Using a local {@link FileBlobStorage} or an {@link InMemoryBlobStorage} as cache is recommended
 * to ensure that the cache is faster enough to have positive impact on performance compared to the  actual resource.
 */
public class ChemDBFileCache extends AbstractCompressible {

    protected final BlobStorage cacheStorage;
    protected final SearchStructureByFormula structureProvider;

    public ChemDBFileCache(@NotNull BlobStorage cacheStorage, @NotNull SearchStructureByFormula structureProvider1) {
        this(cacheStorage, structureProvider1, Compression.GZIP);
    }

    public ChemDBFileCache(@NotNull BlobStorage cacheStorage, @NotNull SearchStructureByFormula structureProvider1, Compression compression) {
        super(compression);
        this.cacheStorage = cacheStorage;
        this.structureProvider = structureProvider1;
    }

    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, long filter) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(formula).stream().filter(ChemDBs.inFilter((it) -> it.bitset, filter)).collect(Collectors.toList());
    }

    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        Path blobKey = Path.of(formula.toString() + ".json.gz");

        try {
            List<FingerprintCandidate> fpcs = new ArrayList<>();
            if (cacheStorage.hasBlob(blobKey)) {
                try {
                    try(InputStream i = cacheStorage.reader(blobKey)){
                        try (final CloseableIterator<FingerprintCandidate> fciter = new CompoundJsonMapper().readFingerprints(CdkFingerprintVersion.getDefault(),
                                Compressible.decompressRawStream(i, getCompression()).get())) {
                            while (fciter.hasNext())
                                fpcs.add(fciter.next());
                        }
                    }
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when searching for " + formula + " in file database. Deleting cache file '" + blobKey + "' an try fetching from Server");
                    fpcs = requestFormulaAndCache(blobKey, formula);
                }
            } else {
                fpcs = requestFormulaAndCache(blobKey, formula);
            }

            return fpcs;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    private List<FingerprintCandidate> requestFormulaAndCache(final @NotNull Path relative, MolecularFormula formula) throws IOException {
        //get unfiltered list from server to write cache.
        final List<FingerprintCandidate> fpcs = structureProvider.lookupStructuresAndFingerprintsByFormula(formula);

        // write cache in background -> cache has to be unfiltered
        SiriusJobs.runInBackgroundIO(() ->
                cacheStorage.withWriter(relative, w -> Compressible.withCompression(w, getCompression(), cw -> CompoundJsonMapper.toJSONList(fpcs, cw))));

        return fpcs.stream().map(FingerprintCandidate::new).toList(); //we do a copy since writing cache is async and the candidate might get modified after returning it.
    }
}


