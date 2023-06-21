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

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintWrapper;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDBs;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralData;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import de.unijena.bioinf.storage.db.nosql.Database;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.Null;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

public class ChemicalNoSQLDBs extends SpectralNoSQLDBs {

    public static AbstractChemicalDatabase getLocalChemDB(Path file) throws IOException {
        return new ChemicalNitriteDatabase(file);
    }

    public static AbstractChemicalDatabase getLocalChemDB(Path file, FingerprintVersion version) throws IOException {
        return new ChemicalNitriteDatabase(file, version);
    }

    public static void importCompoundsAndFingerprintsLazy(
            @NotNull Database<?> database,
            @NotNull Iterable<FingerprintCandidate> candidates,
            @Nullable Iterable<Pair<Ms2SpectralMetadata, Ms2SpectralData>> spectra,
            @NotNull String dbDate, @Nullable String dbFlavor, int fpId,
            int chunkSize
    ) throws ChemicalDatabaseException {
        try {
            // set metainfo tags
            database.insert(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_DATE, dbDate));
            if (dbFlavor != null && !dbFlavor.isBlank())
                database.insert(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_FLAVOR, dbFlavor));
            database.insert(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_FP_ID, String.valueOf(fpId)));

//            database.findAll(SpectralNoSQLDatabase.Tag.class).forEach(System.out::println);

            StreamSupport.stream(Iterables.partition(candidates, chunkSize).spliterator(), false)
                    .forEach(chunk -> {
                        try {
                            //candidates
                            database.insertAll(chunk);
                            //fingerprints
                            database.insertAll(
                                    chunk.stream().map(FingerprintWrapper::of).toList());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

           /* System.out.println("Show compound candidates");
            database.findAll(FingerprintCandidate.class).forEach(c -> System.out.println(c.getInchiKey2D()));
            System.out.println();
            System.out.println("Show FingerprintWrapper");
            database.findAll(FingerprintWrapper.class).forEach(c -> System.out.println(c.getInchikey()));*/

            //spectra
            if (spectra != null)
                importSpectra(database, spectra, chunkSize);
        } catch (RuntimeException e) {
            throw new ChemicalDatabaseException(e.getCause());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }
}
