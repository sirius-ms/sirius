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

import de.unijena.bioinf.chemdb.ChemicalNoSQLDBs;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.BlobStorages;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bioinf.storage.blob.CompressibleBlobStorage;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static de.unijena.bioinf.storage.blob.Compressible.TAG_COMPRESSION;

public class CustomDatabaseFactory {

    public static final String PROPERTY_PREFIX = "de.unijena.bioinf.stores.custom";

    public static final String NOSQL_SUFFIX = ".db";

    private final static Map<String, NoSQLCustomDatabase<?>> NOSQL_LIBRARIES = new ConcurrentHashMap<>();

    private static NoSQLCustomDatabase<?> getNoSQLibrary(String location) throws IOException {
        synchronized (NOSQL_LIBRARIES) {
            if (!NOSQL_LIBRARIES.containsKey(location)) {
                NOSQL_LIBRARIES.put(location, new NoSQLCustomDatabase<>(new ChemicalNitriteDatabase(Path.of(location))));
            }
            return NOSQL_LIBRARIES.get(location);
        }
    }

    public static CustomDatabase create(String location, Compressible.Compression compression, CustomDatabaseSettings config) throws IOException {
        CustomDatabase db;
        if (location.endsWith(NOSQL_SUFFIX)) {
            db = getNoSQLibrary(location);
        } else {
            BlobStorage bs = BlobStorages.createDefault(PROPERTY_PREFIX, location);
            bs.setTags(Map.of(TAG_COMPRESSION, compression.name()));
            db = new BlobCustomDatabase<>(CompressibleBlobStorage.of(bs));
        }
        db.writeSettings(config);
        CustomDataSources.addCustomSourceIfAbsent(db.name(), db.storageLocation());
        return db;
    }

    public static CustomDatabase open(String location) throws IOException {
        CustomDatabase db;
        Path dbPath = Path.of(location);
        if (Files.isRegularFile(dbPath)) {
            db = getNoSQLibrary(location);
        } else {
            db = new BlobCustomDatabase<>(CompressibleBlobStorage.of(BlobStorages.openDefault(PROPERTY_PREFIX, location)));
        }
        db.readSettings();
        CustomDataSources.addCustomSourceIfAbsent(db.name(), db.storageLocation());
        return db;
    }

    public static CustomDatabase createOrOpen(String location, Compressible.Compression compression, CustomDatabaseSettings config) throws IOException {
        if (BlobStorages.exists(PROPERTY_PREFIX, location)) {
            return open(location);
        }
        return create(location, compression, config);
    }

    public static void delete(CustomDatabase database) throws IOException {
        if (database instanceof NoSQLCustomDatabase<?>) {
            synchronized (NOSQL_LIBRARIES) {
                if (!NOSQL_LIBRARIES.containsKey(database.storageLocation())) {
                    throw new IllegalArgumentException("Unknown library: " + database.storageLocation());
                }
                NoSQLCustomDatabase<?> db = NOSQL_LIBRARIES.remove(database.storageLocation());
                db.database.close();
                Files.delete(Path.of(db.storageLocation()));
            }
        } else if (database instanceof BlobCustomDatabase<?>) {
            database.deleteDatabase();
        } else {
            throw new IllegalArgumentException();
        }
    }

//    public static CustomDatabase createAndImportDatabase(
//            String location,
//            Compressible.Compression compression,
//            CustomDatabaseSettings config,
//            List<File> files,
//            @Nullable CustomDatabaseImporter.Listener listener,
//            @NotNull WebAPI<?> api,
//            int bufferSize) throws IOException {
//        CustomDatabase db = create(location, compression, config);
//
//        try {
//            db.importToDatabase(files, listener, api, bufferSize);
//        } catch (CDKException e) {
//            throw new IOException("Error when loading CDK features during database import.", e);
//        }
//        return db;
//    }

}
