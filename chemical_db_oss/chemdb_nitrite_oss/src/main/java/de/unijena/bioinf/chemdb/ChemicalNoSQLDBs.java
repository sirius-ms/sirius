/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDBs;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.storage.db.nosql.Database;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static de.unijena.bioinf.chemdb.SpectralUtils.importSpectra;

public class ChemicalNoSQLDBs extends SpectralNoSQLDBs {

    public static AbstractChemicalDatabase getLocalChemDB(Path file) throws IOException {
        return new ChemicalNitriteDatabase(file);
    }

    public static AbstractChemicalDatabase getLocalChemDB(Path file, FingerprintVersion version) throws IOException {
        return new ChemicalNitriteDatabase(file, version);
    }

    public static void importCandidatesAndSpectra(
            @NotNull ChemicalNoSQLDatabase<?> database,
            @NotNull Map<MolecularFormula, ? extends Collection<FingerprintCandidate>> candidates,
            @Nullable Iterable<Ms2ReferenceSpectrum> spectra,
            @NotNull String dbDate, @Nullable String dbFlavor, int fpId,
            int chunkSize
    ) throws ChemicalDatabaseException {
        try {
            insertTags(database.getStorage(), dbDate, dbFlavor, fpId);

            importCandidates(database.getStorage(), candidates, chunkSize);

            if (spectra != null)
                importSpectra(database, spectra, chunkSize);

        } catch (RuntimeException e) {
            throw new ChemicalDatabaseException(e.getCause());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    private static void importCandidates(@NotNull Database<?> database, @NotNull Map<MolecularFormula, ? extends Collection<FingerprintCandidate>> candidates, int chunkSize) {
        List<FingerprintCandidateWrapper> candidateWrappers = candidates.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(c -> FingerprintCandidateWrapper.of(e.getKey(), c))).toList();


        Partition.ofSize(candidateWrappers, chunkSize).forEach(
                chunk -> {
                    try {
                        database.insertAll(chunk);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void insertTags(Database<?> database, @NotNull String dbDate, @Nullable String dbFlavor, int fpId) throws IOException {
        database.insert(ChemicalNoSQLDatabase.Tag.of(ChemDbTags.TAG_DATE, dbDate));
        if (dbFlavor != null && !dbFlavor.isBlank())
            database.insert(ChemicalNoSQLDatabase.Tag.of(ChemDbTags.TAG_FLAVOR, dbFlavor));
        database.insert(ChemicalNoSQLDatabase.Tag.of(ChemDbTags.TAG_FP_ID, String.valueOf(fpId)));

    }
}
