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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.chemdb.ChemicalBlobDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.BlobStorages;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bioinf.storage.blob.CompressibleBlobStorage;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.unijena.bioinf.storage.blob.Compressible.TAG_COMPRESSION;

public class CustomDatabase<Storage extends BlobStorage> implements SearchableDatabase {
    protected static Logger logger = LoggerFactory.getLogger(CustomDatabase.class);
    public static final String PROPERTY_PREFIX = "de.unijena.bioinf.stores.custom";

    public synchronized void deleteDatabase() {
        try {
            storage.deleteBucket();
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when deleting data storage bucked. Please remove manually at your storage provider. ");
        } finally {
            CustomDataSources.removeCustomSource(storage.getName());
        }
    }


    protected final CompressibleBlobStorage<Storage> storage;
    protected CustomDatabaseSettings settings;


    public static CustomDatabase<?> createAndImportDatabase(
            String bucketLocation,
            Compressible.Compression compression,
            CustomDatabaseSettings config,
            List<File> files,
            @Nullable CustomDatabaseImporter.Listener listener,
            @NotNull WebAPI<?> api,
            int bufferSize) throws IOException {
        CustomDatabase<?> db = createNewDatabase(bucketLocation, compression, config);

        try {
            db.importToDatabase(files, listener, api, bufferSize);
        } catch (CDKException e) {
            throw new IOException("Error when loading CDK features during database import.", e);
        }
        return db;
    }

    public static CustomDatabase<?> createNewDatabase(String bucketLocation, Compressible.Compression compression, CustomDatabaseSettings config) throws IOException {
        BlobStorage bs = BlobStorages.createDefault(PROPERTY_PREFIX, bucketLocation);
        bs.setTags(Map.of(TAG_COMPRESSION, compression.name()));
        CustomDatabase<?> db = new CustomDatabase<>(CompressibleBlobStorage.of(bs));
        db.writeSettings(config);
        CustomDataSources.addCustomSourceIfAbsent(db.name(), db.storageLocation());
        return db;
    }

    public static CustomDatabase<?> openDatabase(String bucketLocation) throws IOException {
        CustomDatabase<?> db = new CustomDatabase<>(CompressibleBlobStorage.of(BlobStorages.openDefault(PROPERTY_PREFIX, bucketLocation)));
        db.readSettings();
        CustomDataSources.addCustomSourceIfAbsent(db.name(), db.storageLocation());
        return db;
    }


    private CustomDatabase(CompressibleBlobStorage<Storage> storage) {
        this.storage = storage;
    }

    public int getDatabaseVersion() {
        return settings.getSchemaVersion();
    }

    public boolean needsUpgrade() {
        return settings.getSchemaVersion() != VersionsInfo.CUSTOM_DATABASE_SCHEMA;
    }

    protected synchronized void readSettings() throws IOException {
        if (storage.hasBlob(settingsBlob())) {
            try (InputStream r = storage.rawReader(settingsBlob())) {
                setSettings(new ObjectMapper().readValue(r, CustomDatabaseSettings.class));
            }
        }
        throw new IOException("Custom DB settings file not found! Please reimport.");
    }

    protected synchronized void writeSettings(CustomDatabaseSettings settings) throws IOException {
        setSettings(settings);
        storage.withRawWriter(settingsBlob(), w -> new ObjectMapper().writeValue(w, settings));
    }

    protected synchronized void writeSettings() throws IOException {
        storage.withWriter(settingsBlob(), w -> new ObjectMapper().writeValue(w, settings));
    }

    private synchronized void setSettings(CustomDatabaseSettings config) {
        settings = config;
    }

    protected Path settingsBlob() {
        return Path.of(ChemicalBlobDatabase.BLOB_SETTINGS);
    }

    @Override
    public String name() {
        return storage.getName();
    }

    public String storageLocation() {
        return storage.getBucketLocation();
    }

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


    public CustomDatabaseSettings getSettings() {
        return settings;
    }

    public CustomDatabaseSettings.Statistics getStatistics() {
        return settings.getStatistics();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomDatabase)) return false;
        CustomDatabase<?> that = (CustomDatabase<?>) o;
        return storage.equals(that.storage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storage);
    }

    public ChemicalBlobDatabase<Storage> toChemDBOrThrow(CdkFingerprintVersion version) throws IOException {
        return new ChemicalBlobDatabase<>(version, storage.getRawStorage());
    }

    public Optional<ChemicalBlobDatabase<Storage>> toChemDB(CdkFingerprintVersion version) {
        try {
            return Optional.of(toChemDBOrThrow(version));
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Could not create ChemDB from Custom database'" + name() + "'", e);
            return Optional.empty();
        }
    }

    public void importToDatabase(List<File> files, @Nullable CustomDatabaseImporter.Listener listener, @NotNull WebAPI<?> api, int bufferSize) throws IOException, CDKException {
        importToDatabase(files, listener, new CustomDatabaseImporter(this, api.getCDKChemDBFingerprintVersion(), api, bufferSize));
    }

    public void importToDatabase(List<File> files, @Nullable CustomDatabaseImporter.Listener listener, CustomDatabaseImporter importer) throws IOException, CDKException {
        if (listener != null)
            importer.addListener(listener);
        for (File f : files) {
            importer.importFrom(f);
        }
        importer.flushBuffer();
    }

    public JJob<Boolean> importToDatabaseJob(List<File> files, @Nullable CustomDatabaseImporter.Listener listener, @NotNull WebAPI<?> api, int bufferSize) {
        return new BasicJJob<Boolean>() {
            CustomDatabaseImporter importer;

            @Override
            protected Boolean compute() throws Exception {
                importer = new CustomDatabaseImporter(CustomDatabase.this, api.getCDKChemDBFingerprintVersion(), api, bufferSize);
                importToDatabase(files, listener, importer);
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
