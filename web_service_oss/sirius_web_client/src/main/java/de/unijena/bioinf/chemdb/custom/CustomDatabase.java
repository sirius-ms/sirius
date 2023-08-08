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

package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDBs;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.io.ParsingIterator;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.webapi.WebAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class CustomDatabase implements SearchableDatabase {
    protected static Logger logger = LoggerFactory.getLogger(CustomDatabase.class);


    public abstract void deleteDatabase();

    protected CustomDatabaseSettings settings;

    public int getDatabaseVersion() {
        return settings.getSchemaVersion();
    }

    public boolean needsUpgrade() {
        return settings.getSchemaVersion() != VersionsInfo.CUSTOM_DATABASE_SCHEMA;
    }

    public abstract Compressible.Compression compression();

    public abstract String storageLocation();

    @Override
    public boolean isRestDb() {
        return settings.isInheritance();
    }

    @Override
    public long getFilterFlag() {
        return settings.getFilter();
    }

    @Override
    public boolean isCustomDb() {
        return true;
    }

    @Override
    public String toString() {
        return name();
    }

    public abstract void readSettings() throws IOException;

    public abstract void writeSettings(CustomDatabaseSettings settings) throws IOException;

    public void writeSettings() throws IOException {
        writeSettings(settings);
    }

    public CustomDatabaseSettings getSettings() {
        return settings;
    }

    protected synchronized void setSettings(CustomDatabaseSettings config) {
        settings = config;
    }

    public CustomDatabaseSettings.Statistics getStatistics() {
        return settings.getStatistics();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomDatabase that)) return false;
        return storageLocation().equals(that.storageLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageLocation());
    }

    public abstract AbstractChemicalDatabase toChemDBOrThrow(CdkFingerprintVersion version) throws IOException;

    public Optional<AbstractChemicalDatabase> toChemDB(CdkFingerprintVersion version) {
        try {
            return Optional.of(toChemDBOrThrow(version));
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Could not create ChemDB from Custom database'" + name() + "'", e);
            return Optional.empty();
        }
    }

    public void importToDatabase(
            List<File> spectrumFiles,
            List<File> structureFiles,
            @Nullable CustomDatabaseImporter.Listener listener,
            CustomDatabaseImporter importer,
            int bufferSize
    ) throws IOException, CDKException {
        if (listener != null)
            importer.addListener(listener);
        if (!spectrumFiles.isEmpty() && !(this instanceof NoSQLCustomDatabase<?>)) {
            throw new IllegalArgumentException();
        }
        Map<String, Pair<String, String>> spectrumSmiles = new HashMap<>();
        if (!spectrumFiles.isEmpty()) {
            SpectralNoSQLDatabase<?> db = ((NoSQLCustomDatabase<?>) this).database;
            Iterator<Ms2Experiment> iterator = new ParsingIterator(spectrumFiles.iterator());
            SpectralNoSQLDBs.importSpectraFromMs2Experiments(db, () -> iterator, bufferSize);
            db.getStorage().findAllStr(Ms2ReferenceSpectrum.class).forEach(
                    ref -> {
                        String smiles = ref.getSmiles();
                        if (!spectrumSmiles.containsKey(smiles)) {
                            spectrumSmiles.put(smiles, Pair.of(ref.getLibraryId(), ref.getName()));
                        }
                    }
            );
            for (Map.Entry<String, Pair<String, String>> entry : spectrumSmiles.entrySet()) {
                importer.importFromString(entry.getKey(), entry.getValue().getLeft(), entry.getValue().getRight());
            }
            importStructuresToDatabase(structureFiles, importer);
            for (Map.Entry<String, String> entry : importer.inchiCache.entrySet()) {
                List<Ms2ReferenceSpectrum> spectra = db.getStorage().findStr(Filter.build().eq("smiles", entry.getKey()), Ms2ReferenceSpectrum.class, "spectrum").peek(
                        ref -> ref.setCandidateInChiKey(entry.getValue())
                ).toList();
                db.getStorage().upsertAll(spectra);
            }
            getStatistics().compounds().set(db.getStorage().countAll(FingerprintCandidateWrapper.class));
            getStatistics().formulas().set(db.getStorage().findAllStr(FingerprintCandidateWrapper.class).map(FingerprintCandidateWrapper::getFormula).distinct().count());
            getStatistics().spectra().set(db.countAllSpectra());
            writeSettings();
        } else {
            importStructuresToDatabase(structureFiles, importer);
        }
    }

    private void importStructuresToDatabase(List<File> structureFiles, CustomDatabaseImporter importer) throws IOException {
        for (File f : structureFiles) {
            importer.importFrom(f);
        }
        importer.flushBuffer();
    }

    public JJob<Boolean> importToDatabaseJob(
            List<File> spectrumFiles,
            List<File> structureFiles,
            @Nullable CustomDatabaseImporter.Listener listener,
            @NotNull WebAPI<?> api,
            int bufferSize
    ) {
        return new BasicJJob<Boolean>() {
            CustomDatabaseImporter importer;

            @Override
            protected Boolean compute() throws Exception {
                importer = new CustomDatabaseImporter(CustomDatabase.this, api.getCDKChemDBFingerprintVersion(), api, bufferSize);
                importToDatabase(spectrumFiles, structureFiles, listener, importer, bufferSize);
                return true;
            }

            @Override
            public void cancel(boolean mayInterruptIfRunning) {
                if (importer != null)
                    importer.cancel();
                super.cancel(mayInterruptIfRunning);
            }
        }.asCPU();
    }
}
