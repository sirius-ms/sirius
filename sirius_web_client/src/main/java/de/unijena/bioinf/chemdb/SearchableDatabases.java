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

import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.OutdatedDBExeption;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchableDatabases {
    //todo should be configurable
    public static final String REST_CACHE_DIR = "rest-cache"; //chache directory for all rest dbs
    public static final String CUSTOM_DB_DIR = "custom";

    private SearchableDatabases() {
    }

    public static File getCustomDatabaseDirectory(){
        return new File(getDatabaseDirectory(),CUSTOM_DB_DIR);
    }

    public static File getRESTDatabaseCacheDirectory() {
        return new File(getDatabaseDirectory(),REST_CACHE_DIR);
    }

    public static File getDatabaseDirectory() {
        final String val = PropertyManager.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        return Paths.get(val).toFile();
    }

    public static CustomDatabase getCustomDatabaseByNameOrThrow(@NotNull String name) {
        return getCustomDatabaseByName(name).
                orElseThrow(() -> new IllegalArgumentException("Database with name: " + name + " does not exist!"));
    }

    @NotNull
    public static Optional<CustomDatabase> getCustomDatabaseByName(@NotNull String name) {
        @NotNull List<CustomDatabase> custom = getCustomDatabases();
        for (CustomDatabase customDatabase : custom)
            if (customDatabase.name().equalsIgnoreCase(name))
                return Optional.of(customDatabase);
        return Optional.empty();
    }

    @NotNull
    public static Optional<CustomDatabase> getCustomDatabaseByPath(@NotNull Path dbDir) {
        if (!Files.isDirectory(dbDir))
            return Optional.empty();

        try {
            return Optional.of(getCustomDatabaseByPathOrThrow(dbDir));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(SearchableDatabases.class).error(e.getMessage(), e.getCause());
            return Optional.empty();
        }
    }

    @NotNull
    public static CustomDatabase getCustomDatabaseByPathOrThrow(@NotNull Path dbDir) {
        try {
            return loadCustomDatabaseFromLocation(dbDir.toFile(), true);
        } catch (IOException e) {
            throw new RuntimeException("Could not load DB from path: " + dbDir.toString(), e);
        }
    }


    @NotNull
    public static SearchableDatabase getDatabaseByNameOrThrow(@NotNull String name) {
        return getDatabaseByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Database with name: " + name + " does not exist!"));
    }

    @NotNull
    public static Optional<? extends SearchableDatabase> getDatabaseByName(@NotNull String name) {
        final DataSource source = DataSources.getSourceFromNameOrNull(name);
        if (source != null)
            return Optional.of(new SearchableRestDB(source.realName, source.flag()));
        return getCustomDatabaseByName(name);
    }

    @NotNull
    public static Optional<? extends SearchableDatabase> getDatabase(@NotNull String nameOrPath) {
        Optional<? extends SearchableDatabase> it = getDatabaseByName(nameOrPath);
        if (it.isEmpty())
            it = getCustomDatabaseByPath(Path.of(nameOrPath));
        return it;
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases() {
        return getCustomDatabases(true);
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases(final boolean up2date) {
        return loadCustomDatabases(up2date);
    }

    public static RestWithCustomDatabase makeRestWithCustomDB(WebAPI webAPI) {
        return new RestWithCustomDatabase(webAPI, getDatabaseDirectory(), REST_CACHE_DIR, CUSTOM_DB_DIR);
    }

    @NotNull
    public static List<SearchableDatabase> getAvailableDatabases() {
        final List<SearchableDatabase> db = Stream.of(DataSource.values()).map(DataSource::realName).map(SearchableDatabases::getDatabaseByNameOrThrow).collect(Collectors.toList());
        Collections.swap(db, 2, DataSource.BIO.ordinal()); //just to put bio on index 3
        db.addAll(getCustomDatabases());
        return db;
    }

    @NotNull
    public static List<CustomDatabase> loadCustomDatabases(boolean up2date) {
        final List<CustomDatabase> databases = new ArrayList<>();
        final File custom = getCustomDatabaseDirectory();
        if (!custom.exists()) {
            return databases;
        }
        for (File subDir : custom.listFiles()) {
            try {
                final CustomDatabase db = loadCustomDatabaseFromLocation(subDir, up2date);
                databases.add(db);
            } catch (IOException e) {
                LoggerFactory.getLogger(CustomDatabase.class).error(e.getMessage(), e);
            }
        }
        return databases;
    }

    @NotNull
    public static CustomDatabase loadCustomDatabaseFromLocation(File dbDir, boolean up2date) throws IOException {
        if (dbDir.isDirectory()) {
            final CustomDatabase db = new CustomDatabase(dbDir.getName(), dbDir);
            db.readSettings();
            if (!up2date || !db.needsUpgrade())
                return db;
            throw new OutdatedDBExeption("DB '" + db.name() + "' is outdated (DB-Version: " + db.getDatabaseVersion() + " vs. ReqVersion: " + VersionsInfo.CUSTOM_DATABASE_SCHEMA + ") . PLease reimport the structures. ");
        }
        throw new IOException("Illegal DB location '" + dbDir.getAbsolutePath() + "'. DB location needs to be a directory.");
    }

    public static SearchableDatabase getAllDb() {
        return getDatabaseByNameOrThrow(DataSource.ALL.name());
    }

    public static SearchableDatabase getBioDb() {
        return getDatabaseByNameOrThrow(DataSource.BIO.name());
    }
}
