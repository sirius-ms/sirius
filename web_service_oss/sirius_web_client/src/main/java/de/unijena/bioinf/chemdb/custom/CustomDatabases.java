/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.BlobStorages;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bioinf.storage.blob.CompressibleBlobStorage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static de.unijena.bioinf.storage.blob.Compressible.TAG_COMPRESSION;

@Slf4j
public class CustomDatabases {
    private CustomDatabases() {}

    public static final String PROPERTY_PREFIX = "de.unijena.bioinf.stores.custom";

    public static final String CUSTOM_DB_SUFFIX = ".siriusdb";

    public static String sanitizeDbName(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private final static Map<String, CustomDatabase> CUSTOM_DATABASES = new ConcurrentHashMap<>();

    @NotNull
    public static Optional<CustomDatabase> getCustomDatabaseByName(@NotNull String name) {
        try {
            return CUSTOM_DATABASES.values().stream()
                    .filter(db -> db.name().equalsIgnoreCase(name))
                    .findFirst();
        } catch (Exception e) {
            log.error("Error when loading custom database with name: {}", name, e);
        }
        return Optional.empty();
    }

    @NotNull
    public static Optional<CustomDatabase> getCustomDatabaseByPath(@NotNull String location) {
        return Optional.ofNullable(CUSTOM_DATABASES.get(location));
    }

    public static CustomDatabase getCustomDatabaseBySource(@NotNull CustomDataSources.CustomSource db) {
        return getCustomDatabaseByPath(db.location()).orElseThrow();
    }

    @NotNull
    public static CustomDatabase open(String location, boolean up2date, CdkFingerprintVersion version) throws IOException {
        final CustomDatabase db = open(location, version);
        if (!up2date || !db.needsUpgrade())
            return db;
        throw new OutdatedDBExeption("DB '" + db.name() + "' is outdated (DB-Version: " + db.getDatabaseVersion() + " vs. ReqVersion: " + CustomDatabase.CUSTOM_DATABASE_SCHEMA + ") . PLease reimport the structures.");
    }

    public static CustomDatabase open(String location, CdkFingerprintVersion version) throws IOException {
        CustomDatabase cached = CUSTOM_DATABASES.get(location);
        if (cached != null) {
            return cached;
        }

        if (!Files.exists(Path.of(location))) {
            throw new FileNotFoundException("Trying to open a custom DB at non-existing location " + location);
        }

        CustomDatabase db;
        if (location.endsWith(CUSTOM_DB_SUFFIX)) {
            ChemicalNitriteDatabase nitriteDb = new ChemicalNitriteDatabase(Path.of(location), version);
            db = new NoSQLCustomDatabase<>(nitriteDb);
        } else {
            CompressibleBlobStorage<BlobStorage> blobDb = CompressibleBlobStorage.of(BlobStorages.openDefault(PROPERTY_PREFIX, location));
            db = new BlobCustomDatabase<>(blobDb, version);
        }
        try {
            db.getSettings();
        } finally {
            db.close();
        }
        String dbName = db.name();
        if (CustomDataSources.containsDB(dbName)) {
            db.close();
            throw new RuntimeException("Datasource with name " + dbName + " already exists.");
        }
        CustomDataSources.addCustomSourceIfAbsent(db);
        CUSTOM_DATABASES.put(location, db);
        return db;
    }

    public static CustomDatabase create(String location, CustomDatabaseSettings config, CdkFingerprintVersion version) throws IOException {
        //sanitize db name:
        if (!config.getName().equals(sanitizeDbName(config.getName())))
            throw new IllegalArgumentException("Unsupported database name '" + config.getName() + "'. Allowed would be: " + sanitizeDbName(config.getName()));
        if (CustomDataSources.containsDB(config.getName())) {
            throw new RuntimeException("Datasource with name " + config.getName() + " already exists.");
        }

        CustomDatabase db;
        if (location.endsWith(CUSTOM_DB_SUFFIX)) {
            Path dir = Path.of(location).getParent();
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
            ChemicalNitriteDatabase storage = new ChemicalNitriteDatabase(Path.of(location), version);
            db = new NoSQLCustomDatabase<>(storage);
            db.writeSettings(config);
            storage.getStorage().flush();
        } else {
            BlobStorage bs = BlobStorages.createDefault(PROPERTY_PREFIX, location);
            bs.setTags(Map.of(TAG_COMPRESSION, Compressible.Compression.GZIP.name()));
            db = new BlobCustomDatabase<>(CompressibleBlobStorage.of(bs), version);
            db.writeSettings(config);
        }
        CustomDataSources.addCustomSourceIfAbsent(db);
        CUSTOM_DATABASES.put(location, db);
        return db;
    }

    public static void remove(CustomDatabase db, boolean delete) {
        CUSTOM_DATABASES.remove(db.storageLocation());
        CustomDataSources.removeCustomSource(db.name());
        db.close();
        if (delete) {
            db.deleteDatabase();
        }
    }
}
