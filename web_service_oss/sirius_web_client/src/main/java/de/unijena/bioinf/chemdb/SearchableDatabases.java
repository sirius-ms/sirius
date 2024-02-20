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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseFactory;
import de.unijena.bioinf.chemdb.custom.OutdatedDBExeption;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unijena.bioinf.chemdb.custom.CustomDataSources.PROP_KEY;
import static de.unijena.bioinf.chemdb.custom.CustomDataSources.getCustomDatabaseDirectory;

public class SearchableDatabases {
    SearchableDatabases() {

    }

    public static CustomDatabase getCustomDatabaseByNameOrThrow(@NotNull String name, FingerprintVersion version) {
        return getCustomDatabaseByName(name, version).
                orElseThrow(() -> new IllegalArgumentException("Database with name: " + name + " does not exist!"));
    }

    @NotNull
    public static Optional<CustomDatabase> getCustomDatabaseByName(@NotNull String name, FingerprintVersion version) {
        @NotNull List<CustomDatabase> custom = getCustomDatabases(version);
        for (CustomDatabase customDatabase : custom)
            if (customDatabase.name().equalsIgnoreCase(name))
                return Optional.of(customDatabase);
        return Optional.empty();
    }

    @NotNull
    public static Optional<CustomDatabase> getCustomDatabaseByPath(@NotNull Path dbDir, FingerprintVersion version) {
        try {
            return Optional.of(getCustomDatabaseByPathOrThrow(dbDir, version));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(SearchableDatabases.class).error(e.getMessage(), e.getCause());
            return Optional.empty();
        }
    }

    public static @NotNull Optional<CustomDatabase> getCustomDatabase(@NotNull String nameOrPath, FingerprintVersion version) {
        Optional<CustomDatabase> it = getCustomDatabaseByName(nameOrPath, version);
        if (it.isEmpty())
            it = getCustomDatabaseByPath(Path.of(nameOrPath), version);
        return it;
    }


    @NotNull
    public static CustomDatabase getCustomDatabaseByPathOrThrow(@NotNull Path dbDir, FingerprintVersion version) {
        try {
            return loadCustomDatabaseFromLocation(dbDir.toAbsolutePath().toString(), true, version);
        } catch (IOException e) {
            throw new RuntimeException("Could not load DB from path: " + dbDir, e);
        }
    }


    @NotNull
    public static SearchableDatabase getDatabaseByNameOrThrow(@NotNull String name, FingerprintVersion version) {
        return getDatabaseByName(name, version)
                .orElseThrow(() -> new IllegalArgumentException("Database with name: " + name + " does not exist!"));
    }

    @NotNull
    public static Optional<? extends SearchableDatabase> getDatabaseByName(@NotNull String name, FingerprintVersion version) {
        final DataSource source = DataSources.getSourceFromNameOrNull(name);
        if (source != null)
            return Optional.of(new SearchableWebDB(source.realName, source.flag()));
        return getCustomDatabaseByName(name, version);
    }

    @NotNull
    public static Optional<? extends SearchableDatabase> getDatabase(@NotNull String nameOrPath, FingerprintVersion version) {
        Optional<? extends SearchableDatabase> it = getDatabaseByName(nameOrPath, version);
        if (it.isEmpty())
            it = getCustomDatabaseByPath(Path.of(nameOrPath), version);
        return it;
    }

    @NotNull
    public static CustomDatabase getCustomDatabaseBySource(@NotNull CustomDataSources.Source db, FingerprintVersion version) {
        return getCustomDatabaseByPathOrThrow(Path.of(db.id()), version);
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases(FingerprintVersion version) {
        return getCustomDatabases(true, version);
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases(final boolean up2date, FingerprintVersion version) {
        return loadCustomDatabases(up2date, version);
    }

    @NotNull
    public static List<SearchableDatabase> getAvailableDatabases(FingerprintVersion version) {
        final List<SearchableDatabase> db = Stream.of(DataSource.values())
                .map(DataSource::realName)
                .map(name -> getDatabaseByNameOrThrow(name, version)).collect(Collectors.toList());

        Collections.swap(db, 2, DataSource.BIO.ordinal()); //just to put bio on index 3
        db.addAll(getCustomDatabases(version));
        return db;
    }

    @NotNull
    public static List<CustomDatabase> loadCustomDatabases(boolean up2date, FingerprintVersion version) {
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
                    final CustomDatabase db = CustomDatabaseFactory.open(bucketLocation, version);
                    if (up2date && db.needsUpgrade())
                        throw new OutdatedDBExeption("DB '" + db.name() + "' is outdated (DB-Version: " + db.getDatabaseVersion() + " vs. ReqVersion: " + VersionsInfo.CUSTOM_DATABASE_SCHEMA + ") . PLease reimport the structures. ");

                    databases.add(db);
                } catch (IOException e) {
                    LoggerFactory.getLogger(CustomDatabase.class).error(e.getMessage(), e);
                }
            }
        }
        return databases.stream().distinct().collect(Collectors.toList());
    }

    @NotNull
    public static CustomDatabase loadCustomDatabaseFromLocation(String bucketLocation, boolean up2date, FingerprintVersion version) throws IOException {
        final CustomDatabase db = CustomDatabaseFactory.open(bucketLocation, version);
        if (!up2date || !db.needsUpgrade())
            return db;
        throw new OutdatedDBExeption("DB '" + db.name() + "' is outdated (DB-Version: " + db.getDatabaseVersion() + " vs. ReqVersion: " + VersionsInfo.CUSTOM_DATABASE_SCHEMA + ") . PLease reimport the structures. ");
    }
}
