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

package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.BlobStorages;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bioinf.storage.blob.CompressibleBlobStorage;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static de.unijena.bioinf.chemdb.custom.CustomDataSources.getCustomDatabaseDirectory;
import static de.unijena.bioinf.storage.blob.Compressible.TAG_COMPRESSION;

public class CustomDatabaseFactory {

    public static final String PROPERTY_PREFIX = "de.unijena.bioinf.stores.custom";

    public static final String NOSQL_SUFFIX = ".db";

    private final static Map<String, NoSQLCustomDatabase<?, ?>> NOSQL_LIBRARIES = new ConcurrentHashMap<>();

    private static NoSQLCustomDatabase<?, ?> getNoSQLibrary(String location, FingerprintVersion version) throws IOException {
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

    public static CustomDatabase create(String location, Compressible.Compression compression, CustomDatabaseSettings config, FingerprintVersion version) throws IOException {
        CustomDatabase db;
        if (location.endsWith(NOSQL_SUFFIX)) {
            Path dir = Path.of(location).getParent();
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
            db = getNoSQLibrary(location, version);
        } else {
            BlobStorage bs = BlobStorages.createDefault(PROPERTY_PREFIX, location);
            bs.setTags(Map.of(TAG_COMPRESSION, compression.name()));
            db = new BlobCustomDatabase<>(CompressibleBlobStorage.of(bs), version);
        }
        db.writeSettings(config);
        CustomDataSources.addCustomSourceIfAbsent(db.name(), db.storageLocation());
        return db;
    }

    public static CustomDatabase open(String location, FingerprintVersion version) throws IOException {
        CustomDatabase db;
        if (location.endsWith(NOSQL_SUFFIX)) {
            db = getNoSQLibrary(location, version);
        } else {
            db = new BlobCustomDatabase<>(CompressibleBlobStorage.of(BlobStorages.openDefault(PROPERTY_PREFIX, location)), version);
        }
        db.readSettings();
        CustomDataSources.addCustomSourceIfAbsent(db.name(), db.storageLocation());
        return db;
    }

    public static CustomDatabase createOrOpen(String location, Compressible.Compression compression, CustomDatabaseSettings config, FingerprintVersion version) throws IOException {
        final Path custom = getCustomDatabaseDirectory();
        if (!location.contains("/"))
            location = custom.resolve(location).toAbsolutePath().toString();

        if (location.endsWith(NOSQL_SUFFIX) && Files.isRegularFile(Path.of(location))) {
            return open(location, version);
        } else if (BlobStorages.exists(PROPERTY_PREFIX, location)) {
            return open(location, version);
        }
        return create(location, compression, config, version);
    }

    public static void remove(CustomDatabase db, boolean delete) {
        if (delete) {
            try {
                CustomDatabaseFactory.delete(db);
            } catch (IOException e) {
                LoggerFactory.getLogger(CustomDatabaseFactory.class).error("Error deleting database.", e);
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
