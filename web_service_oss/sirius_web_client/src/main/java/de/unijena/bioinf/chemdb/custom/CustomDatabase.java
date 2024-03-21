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

import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.WriteableChemicalDatabase;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.WriteableSpectralLibrary;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class CustomDatabase implements SearchableDatabase {
    protected static Logger logger = LoggerFactory.getLogger(CustomDatabase.class);

    public abstract void deleteDatabase();

    protected CustomDatabaseSettings settings;

    public int getDatabaseVersion() {
        return settings.getSchemaVersion();
    }

    @Override
    public String name() {
        return Optional.ofNullable(settings).map(CustomDatabaseSettings::getName).orElse(null);
    }

    @Override
    public String displayName() {
        return Optional.ofNullable(settings).map(CustomDatabaseSettings::getDisplayName).orElse(name());
    }

    public boolean needsUpgrade() {
        //todo nightsky: check fingerprint compatibility?
        return settings.getSchemaVersion() != VersionsInfo.CUSTOM_DATABASE_SCHEMA;
    }

    public abstract String storageLocation();

    @Override
    public boolean isRestDb() {
        return false;
    }

    @Override
    public long getFilterFlag() {
        return CustomDataSources.getFlagFromName(name());
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

    public abstract AbstractChemicalDatabase toChemDBOrThrow() throws IOException;

    public Optional<AbstractChemicalDatabase> toChemDB() {
        return toOptional(this::toChemDBOrThrow, AbstractChemicalDatabase.class);
    }

    public abstract WriteableChemicalDatabase toWriteableChemDBOrThrow() throws IOException;

    public Optional<WriteableChemicalDatabase> toWriteableChemDB() {
        return toOptional(this::toWriteableChemDBOrThrow, WriteableChemicalDatabase.class);
    }

    public abstract SpectralLibrary toSpectralLibraryOrThrow() throws IOException;

    public Optional<SpectralLibrary> toSpectralLibrary() {
        return toOptional(this::toSpectralLibraryOrThrow, SpectralLibrary.class);
    }

    public abstract WriteableSpectralLibrary toWriteableSpectralLibraryOrThrow() throws IOException;

    public Optional<WriteableSpectralLibrary> toWriteableSpectralLibrary() {
        return toOptional(this::toWriteableSpectralLibraryOrThrow, WriteableSpectralLibrary.class);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {

        T get() throws IOException;

    }

    private <S> Optional<S> toOptional(ThrowingSupplier<S> supplier, Class<S> type) {
        try {
            return Optional.of(supplier.get());
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Could not create " + type + " from Custom database'" + name() + "'", e);
            return Optional.empty();
        }
    }

    public static void importToDatabase(
            List<InputResource<?>> spectrumFiles,
            List<InputResource<?>> structureFiles,
            @Nullable CustomDatabaseImporter.Listener listener,
            CustomDatabaseImporter importer
    ) throws IOException {
        if (listener != null)
            importer.addListener(listener);

        try {
            if (structureFiles != null && !structureFiles.isEmpty())
                importer.importStructuresFromResources(structureFiles);

            if (spectrumFiles != null && !spectrumFiles.isEmpty())
                importer.importSpectraFromResources(spectrumFiles);

        } finally {
            // update tags & statistics
            importer.flushAllAndUpdateStatistics();
        }
    }



    public  JJob<Boolean> importToDatabaseJob(
            List<InputResource<?>> spectrumFiles,
            List<InputResource<?>> structureFiles,
            @Nullable CustomDatabaseImporter.Listener listener,
            @NotNull WebAPI<?> api,
            int bufferSize
    ) {
        return new BasicJJob<Boolean>() {
            CustomDatabaseImporter importer;

            @Override
            protected Boolean compute() throws Exception {
                importer = new CustomDatabaseImporter((NoSQLCustomDatabase<?, ?>) CustomDatabase.this, api.getCDKChemDBFingerprintVersion(), api, bufferSize);
                importToDatabase(spectrumFiles, structureFiles, listener, importer);
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
