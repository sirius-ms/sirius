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
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintWrapper;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDBs;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.storage.db.nosql.Database;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.StreamSupport;

public class ChemicalNoSQLDBs extends SpectralNoSQLDBs {

    private static class FlatmapIterable<K, V> implements Iterable<Pair<K, V>> {

        final Map<K, Iterable<V>> source;

        private FlatmapIterable(Map<K, Iterable<V>> source) {
            this.source = source;
        }

        @NotNull
        @Override
        public Iterator<Pair<K, V>> iterator() {
            return new FlatmapIterator();
        }

        private class FlatmapIterator implements Iterator<Pair<K, V>> {

        K key;
        Iterator<K> keyIt;
        Iterator<V> valueIt;

            public FlatmapIterator() {
                this.key = null;
                this.keyIt = source.keySet().iterator();
                this.valueIt = null;
            }

            @Override
            public boolean hasNext() {
                if (valueIt == null || !valueIt.hasNext()) {
                    if (keyIt.hasNext()) {
                        key = keyIt.next();
                        valueIt = source.get(key).iterator();
                    } else {
                        key = null;
                        valueIt = null;
                    }
                }
                if (valueIt == null)
                    return false;
                return valueIt.hasNext();
            }

            @Override
            public Pair<K, V> next() {
                if (valueIt == null)
                    return Pair.of(null, null);
                return Pair.of(key, valueIt.next());
            }

        }

    }

    public static AbstractChemicalDatabase getLocalChemDB(Path file) throws IOException {
        return new ChemicalNitriteDatabase(file);
    }

    public static AbstractChemicalDatabase getLocalChemDB(Path file, FingerprintVersion version) throws IOException {
        return new ChemicalNitriteDatabase(file, version);
    }

    public static void importCompoundsAndFingerprintsLazy(
            @NotNull Database<?> database,
            @NotNull Map<MolecularFormula, Iterable<FingerprintCandidate>> candidates,
            @Nullable Iterable<Ms2ReferenceSpectrum> spectra,
            @NotNull String dbDate, @Nullable String dbFlavor, int fpId,
            int chunkSize
    ) throws ChemicalDatabaseException {
        try {
            // set metainfo tags
            database.insert(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_DATE, dbDate));
            if (dbFlavor != null && !dbFlavor.isBlank())
                database.insert(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_FLAVOR, dbFlavor));
            database.insert(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_FP_ID, String.valueOf(fpId)));

            StreamSupport.stream(Iterables.partition(new FlatmapIterable<>(candidates), chunkSize).spliterator(), false)
                    .forEach(chunk -> {
                        try {
                            //candidates
                            database.insertAll(chunk.stream().map(c -> FingerprintCandidateWrapper.of(c.getLeft(), c.getRight())).toList());
                            //fingerprints
                            database.insertAll(chunk.stream().map(c -> FingerprintWrapper.of(c.getRight())).toList());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

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
