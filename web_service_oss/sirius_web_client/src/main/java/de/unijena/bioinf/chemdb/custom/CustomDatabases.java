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
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.BlobStorages;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bioinf.storage.blob.CompressibleBlobStorage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static de.unijena.bioinf.chemdb.custom.CustomDataSources.PROP_KEY;
import static de.unijena.bioinf.chemdb.custom.CustomDataSources.getCustomDatabaseDirectory;
import static de.unijena.bioinf.storage.blob.Compressible.TAG_COMPRESSION;

@Slf4j
public class CustomDatabases {
    private CustomDatabases() {}

    public static final String PROPERTY_PREFIX = "de.unijena.bioinf.stores.custom";

    public static final String CUSTOM_DB_SUFFIX = ".siriusdb";

    public static String sanitizeDbName(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    //todo we should cache blob database as well if the use caching here.
    private final static Map<String, NoSQLCustomDatabase<?, ?>> NOSQL_LIBRARIES = new ConcurrentHashMap<>();

    private static NoSQLCustomDatabase<?, ?> getNoSQLibrary(String location, CdkFingerprintVersion version) throws IOException {
        synchronized (NOSQL_LIBRARIES) {
            if (!NOSQL_LIBRARIES.containsKey(location)) {
                try {
                    NoSQLCustomDatabase<?, ?> db = new NoSQLCustomDatabase<>(new ChemicalNitriteDatabase(Path.of(location), version));
                    NOSQL_LIBRARIES.put(location, db);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
            return NOSQL_LIBRARIES.get(location);
        }
    }



    public static CustomDatabase getCustomDatabaseByNameOrThrow(@NotNull String name, CdkFingerprintVersion version) {
        return getCustomDatabaseByName(name, version).
                orElseThrow(() -> new IllegalArgumentException("Database with name: " + name + " does not exist!"));
    }

    @NotNull
    public static Optional<CustomDatabase> getCustomDatabaseByName(@NotNull String name, CdkFingerprintVersion version) {
        try {
            @NotNull List<CustomDatabase> custom = getCustomDatabases(version);
            for (CustomDatabase customDatabase : custom)
                if (customDatabase.name().equalsIgnoreCase(name))
                    return Optional.of(customDatabase);
        } catch (Exception e) {
            log.error("Error when loading custom database with name: {}", name, e);
        }
        return Optional.empty();
    }

    @NotNull
    public static Optional<CustomDatabase> getCustomDatabaseByPath(@NotNull Path dbDir, CdkFingerprintVersion version) {
        try {
            return Optional.of(getCustomDatabaseByPathOrThrow(dbDir, version));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(CustomDatabases.class).error(e.getMessage(), e.getCause());
            return Optional.empty();
        }
    }

    public static @NotNull Optional<CustomDatabase> getCustomDatabase(@NotNull String nameOrPath, CdkFingerprintVersion version) {
        Optional<CustomDatabase> it = getCustomDatabaseByName(nameOrPath, version);
        if (it.isEmpty())
            it = getCustomDatabaseByPath(Path.of(nameOrPath), version);
        return it;
    }


    @NotNull
    public static CustomDatabase getCustomDatabaseByPathOrThrow(@NotNull Path dbDir, CdkFingerprintVersion version) {
        return getCustomDatabaseByPathOrThrow(dbDir, true, version);
    }
    @NotNull
    public static CustomDatabase getCustomDatabaseByPathOrThrow(@NotNull Path dbDir, boolean up2date, CdkFingerprintVersion version) {
        try {
            return open(dbDir.toAbsolutePath().toString(), up2date, version);
        } catch (IOException e) {
            throw new RuntimeException("Could not load DB from path: " + dbDir, e);
        }
    }

    @NotNull
    public static CustomDatabase getCustomDatabaseBySource(@NotNull CustomDataSources.CustomSource db, CdkFingerprintVersion version) {
        return getCustomDatabaseByPathOrThrow(Path.of(db.location()), version);
    }
    public static CustomDatabase getCustomDatabaseBySource(@NotNull CustomDataSources.CustomSource db, boolean up2date, CdkFingerprintVersion version) {
        return getCustomDatabaseByPathOrThrow(Path.of(db.location()), up2date, version);
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases(CdkFingerprintVersion version) {
        return getCustomDatabases(true, version);
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases(final boolean up2date, CdkFingerprintVersion version) {
        return openAll(up2date, version);
    }

    @NotNull
    public static List<CustomDatabase> openAll(boolean up2date, CdkFingerprintVersion version) {
        final List<CustomDatabase> databases = new ArrayList<>();
        final Path custom = getCustomDatabaseDirectory();

        String customDBs = PropertyManager.getProperty(PROP_KEY);
        if (customDBs != null && !customDBs.isBlank()) {
            for (String bucketLocation : customDBs.split("\\s*,\\s*")) {
                if (!bucketLocation.contains("/"))
                    bucketLocation = custom.resolve(bucketLocation).toAbsolutePath().toString();

                if (!Files.exists(Path.of(bucketLocation))) {
                    LoggerFactory.getLogger(CustomDatabase.class).warn(
                            "Database location '{}' does not exist. Database will not be available in SIRIUS.",
                            bucketLocation);
                    continue;
                }

                try {
                    final CustomDatabase db = open(bucketLocation, version);
                    if (up2date && db.needsUpgrade())
                        throw new OutdatedDBExeption("DB '" + db.name() + "' is outdated (DB-Version: " + db.getDatabaseVersion() + " vs. ReqVersion: " + CustomDatabase.CUSTOM_DATABASE_SCHEMA + ") . PLease reimport the structures. ");

                    databases.add(db);
                } catch (IOException e) {
                    LoggerFactory.getLogger(CustomDatabase.class).error(e.getMessage(), e);
                }
            }
        }
        return databases.stream().distinct().collect(Collectors.toList());
    }

    @NotNull
    public static CustomDatabase open(String bucketLocation, boolean up2date, CdkFingerprintVersion version) throws IOException {
        final CustomDatabase db = open(bucketLocation, version);
        if (!up2date || !db.needsUpgrade())
            return db;
        throw new OutdatedDBExeption("DB '" + db.name() + "' is outdated (DB-Version: " + db.getDatabaseVersion() + " vs. ReqVersion: " + CustomDatabase.CUSTOM_DATABASE_SCHEMA + ") . PLease reimport the structures. ");
    }

    public static CustomDatabase open(String location, CdkFingerprintVersion version) throws IOException {
        CustomDatabase db;
        if (location.endsWith(CUSTOM_DB_SUFFIX)) {
            db = getNoSQLibrary(location, version);
        } else {
            db = new BlobCustomDatabase<>(CompressibleBlobStorage.of(BlobStorages.openDefault(PROPERTY_PREFIX, location)), version);
        }

        db.getSettings(); //readsSetting only if not exists...
        CustomDataSources.addCustomSourceIfAbsent(db);
        return db;
    }

    public static CustomDatabase createOrOpen(String location, CustomDatabaseSettings config, CdkFingerprintVersion version) throws IOException {
        final Path custom = getCustomDatabaseDirectory();
        if (!location.contains("/"))
            location = custom.resolve(location).toAbsolutePath().toString();

        if (location.endsWith(CUSTOM_DB_SUFFIX) && Files.isRegularFile(Path.of(location))) {
            return open(location, version);
        } else if (BlobStorages.exists(PROPERTY_PREFIX, location)) {
            return open(location, version);
        }
        return create(location, config, version);
    }

    public static CustomDatabase create(String location, CustomDatabaseSettings config, CdkFingerprintVersion version) throws IOException {
        //sanitize db name:
        if (!config.getName().equals(sanitizeDbName(config.getName())))
            throw new IllegalArgumentException("Unsupported databse name. Name was: '" + config.getName() + "'. Allowed would be: " + sanitizeDbName(config.getName()));

        CustomDatabase db;
        if (location.endsWith(CUSTOM_DB_SUFFIX)) {
            Path dir = Path.of(location).getParent();
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
            db = getNoSQLibrary(location, version);
        } else {
            BlobStorage bs = BlobStorages.createDefault(PROPERTY_PREFIX, location);
            bs.setTags(Map.of(TAG_COMPRESSION, Compressible.Compression.GZIP.name()));
            db = new BlobCustomDatabase<>(CompressibleBlobStorage.of(bs), version);
        }
        db.writeSettings(config);
        CustomDataSources.addCustomSourceIfAbsent(db);
        return db;
    }

    public static boolean remove(String dbId) {
        return CustomDataSources.removeCustomSource(dbId);
    }
    public static void remove(CustomDatabase db, boolean delete) {
        if (delete) {
            try {
                delete(db);
            } catch (IOException e) {
                LoggerFactory.getLogger(CustomDatabases.class).error("Error deleting database.", e);
            }
        } else {
            CustomDataSources.removeCustomSource(db.name());
        }
    }

    public static void delete(CustomDatabase database) throws IOException {
        try {
            if (database instanceof NoSQLCustomDatabase<?, ?>) {
                synchronized (NOSQL_LIBRARIES) {
                    if (!NOSQL_LIBRARIES.containsKey(database.storageLocation())) {
                        throw new IllegalArgumentException("Unknown library: " + database.storageLocation());
                    }
                    NoSQLCustomDatabase<?, ?> db = NOSQL_LIBRARIES.remove(database.storageLocation());
                    db.database.close();
                    Files.delete(Path.of(db.storageLocation()));
                }
            } else if (database instanceof BlobCustomDatabase<?>) {
                database.deleteDatabase();
            } else {
                throw new IllegalArgumentException();
            }
        } finally {
            CustomDataSources.removeCustomSource(database.name());
        }
    }
}
