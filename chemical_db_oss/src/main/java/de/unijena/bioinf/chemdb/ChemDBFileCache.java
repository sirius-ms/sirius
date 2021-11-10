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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.json.JsonException;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

//todo it would be nice if this would use the FileBasedChemDB API
public class ChemDBFileCache {

    protected final File cacheDir;
    protected final SearchStructureByFormula structureProvider;

    public ChemDBFileCache(@Nullable File cacheDir, @NotNull SearchStructureByFormula structureProvider1) {
        this.cacheDir = cacheDir != null ? cacheDir : defaultCacheDir();
        this.structureProvider = structureProvider1;
    }

    public static File defaultCacheDir() {
        final String val = System.getenv("CSI_FINGERID_STORAGE");
        if (val != null) return new File(val);
        return new File(System.getProperty("user.home"), "csi_fingerid_cache");
    }


    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, long filter) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(formula).stream().filter(ChemDBs.inFilter((it) -> it.bitset, filter)).collect(Collectors.toList());
    }

    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final File stfile = new File(cacheDir, "/" + formula.toString() + ".json.gz");
        try {
            List<FingerprintCandidate> fpcs = new ArrayList<>();
            if (stfile.exists()) {
                try {
                    final GZIPInputStream zin = new GZIPInputStream(new BufferedInputStream(new FileInputStream(stfile)));
                    try (final CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(CdkFingerprintVersion.getDefault(), new InputStreamReader(zin))) {
                        while (fciter.hasNext())
                            fpcs.add(fciter.next());
                    }
                } catch (IOException | JsonException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when searching for " + formula + " in file database. Deleting cache file '" + stfile.getAbsolutePath() + "' an try fetching from Server");
                    stfile.delete();
                    fpcs = requestFormulaAndCache(stfile, formula);
                }
            } else {
                fpcs = requestFormulaAndCache(stfile, formula);
            }

            return fpcs;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    private List<FingerprintCandidate> requestFormulaAndCache(final @NotNull File output, MolecularFormula formula) throws IOException {
        //get unfiltered list from server to write cache.
        final List<FingerprintCandidate> fpcs = structureProvider.lookupStructuresAndFingerprintsByFormula(formula);

        // write cache in background -> cache has to be unfiltered
        SiriusJobs.runInBackground(() -> {
            output.getParentFile().mkdirs();
            final File tempFile = File.createTempFile("sirius_formula", ".json.gz", output.getParentFile());
            try {
                try (final GZIPOutputStream fout = new GZIPOutputStream(new FileOutputStream(tempFile))) {
                    try (final BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fout))) {
                        FingerprintCandidate.toJSONList(fpcs, br);
                    }
                }

                // move tempFile is canonical on same fs
                if (output.exists() || !tempFile.renameTo(output))
                    tempFile.delete();

                return true;
            } finally {
                Files.deleteIfExists(tempFile.toPath());
            }
        });

        return fpcs;
    }


}


