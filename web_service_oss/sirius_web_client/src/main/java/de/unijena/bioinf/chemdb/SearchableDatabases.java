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

import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.OutdatedDBExeption;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchableDatabases {
    public final static Set<String> NON_SLECTABLE_LIST = Set.of(DataSource.ADDITIONAL.realName, DataSource.TRAIN.realName, DataSource.LIPID.realName(), DataSource.ALL.realName, DataSource.ALL_BUT_INSILICO.realName,
            DataSource.PUBCHEMANNOTATIONBIO.realName, DataSource.PUBCHEMANNOTATIONDRUG.realName, DataSource.PUBCHEMANNOTATIONFOOD.realName, DataSource.PUBCHEMANNOTATIONSAFETYANDTOXIC.realName,
            DataSource.SUPERNATURAL.realName
    );

    //todo should be configurable
    public static final String WEB_CACHE_DIR = "web-cache"; //cache directory for all remote (web) dbs
    public static final String CUSTOM_DB_DIR = "custom";
    public static final String PROP_KEY = "de.unijena.bioinf.chemdb.custom.source";

    private SearchableDatabases() {
    }

    @NotNull
    public static Path getCustomDatabaseDirectory() {
        return getDatabaseDirectory().resolve(CUSTOM_DB_DIR);
    }

    @NotNull
    public static Path getWebDatabaseCacheDirectory() {
        return getDatabaseDirectory().resolve(WEB_CACHE_DIR);
    }

    public static FileBlobStorage getWebDatabaseCacheStorage() {
        try {
            Files.createDirectories(getWebDatabaseCacheDirectory());
            return new FileBlobStorage(getWebDatabaseCacheDirectory());
        } catch (IOException e) {
            throw new RuntimeException("Could not create cache directories!", e);
        }
    }

    public static Path getDatabaseDirectory() {
        final String val = PropertyManager.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        return Paths.get(val);
    }

    public static CustomDatabase<?> getCustomDatabaseByNameOrThrow(@NotNull String name) {
        return getCustomDatabaseByName(name).
                orElseThrow(() -> new IllegalArgumentException("Database with name: " + name + " does not exist!"));
    }

    @NotNull
    public static Optional<CustomDatabase<?>> getCustomDatabaseByName(@NotNull String name) {
        @NotNull List<CustomDatabase<?>> custom = getCustomDatabases();
        for (CustomDatabase<?> customDatabase : custom)
            if (customDatabase.name().equalsIgnoreCase(name))
                return Optional.of(customDatabase);
        return Optional.empty();
    }

    @NotNull
    public static Optional<CustomDatabase<?>> getCustomDatabaseByPath(@NotNull Path dbDir) {
        if (!Files.isDirectory(dbDir))
            return Optional.empty();

        try {
            return Optional.of(getCustomDatabaseByPathOrThrow(dbDir));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(SearchableDatabases.class).error(e.getMessage(), e.getCause());
            return Optional.empty();
        }
    }

    public static @NotNull Optional<CustomDatabase<?>> getCustomDatabase(@NotNull String nameOrPath) {
        Optional<CustomDatabase<?>> it = getCustomDatabaseByName(nameOrPath);
        if (it.isEmpty())
            it = getCustomDatabaseByPath(Path.of(nameOrPath));
        return it;
    }


    @NotNull
    public static CustomDatabase<?> getCustomDatabaseByPathOrThrow(@NotNull Path dbDir) {
        try {
            return loadCustomDatabaseFromLocation(dbDir.toAbsolutePath().toString(), true);
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
            return Optional.of(new SearchableWebDB(source.realName, source.flag()));
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
    public static List<CustomDatabase<?>> getCustomDatabases() {
        return getCustomDatabases(true);
    }

    @NotNull
    public static List<CustomDatabase<?>> getCustomDatabases(final boolean up2date) {
        return loadCustomDatabases(up2date);
    }

    public static WebWithCustomDatabase makeWebWithCustomDB(WebAPI<?> webAPI) {
        return new WebWithCustomDatabase(webAPI, getDatabaseDirectory(), getWebDatabaseCacheStorage());
    }

    @NotNull
    public static List<SearchableDatabase> getAvailableDatabases() {
        final List<SearchableDatabase> db = Stream.of(DataSource.values()).map(DataSource::realName).map(SearchableDatabases::getDatabaseByNameOrThrow).collect(Collectors.toList());
        Collections.swap(db, 2, DataSource.BIO.ordinal()); //just to put bio on index 3
        db.addAll(getCustomDatabases());
        return db;
    }

    @NotNull
    public static List<CustomDatabase<?>> loadCustomDatabases(boolean up2date) {
        final List<CustomDatabase<?>> databases = new ArrayList<>();
        final Path custom = getCustomDatabaseDirectory();

        String customDBs = PropertyManager.getProperty(PROP_KEY);
        if (customDBs != null && !customDBs.isBlank()) {
            for (String bucketLocation : customDBs.split("\\s*,\\s*")) {
                if (!bucketLocation.contains("/"))
                    bucketLocation = custom.resolve(bucketLocation).toAbsolutePath().toString();

                try {
                    final CustomDatabase<?> db = CustomDatabase.open(bucketLocation);//new CustomDatabase(dbDir.getName(), dbDir);
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
    public static CustomDatabase<?> loadCustomDatabaseFromLocation(String bucketLocation, boolean up2date) throws IOException {
        final CustomDatabase<?> db = CustomDatabase.open(bucketLocation);
        if (!up2date || !db.needsUpgrade())
            return db;
        throw new OutdatedDBExeption("DB '" + db.name() + "' is outdated (DB-Version: " + db.getDatabaseVersion() + " vs. ReqVersion: " + VersionsInfo.CUSTOM_DATABASE_SCHEMA + ") . PLease reimport the structures. ");
    }

    public static List<SearchableDatabase> getAllSelectableDbs() {
        return getAvailableDatabases().stream().
                filter(db -> !NON_SLECTABLE_LIST.contains(db.name()))
                .collect(Collectors.toList());
    }

    public static List<SearchableDatabase> getNonInSilicoSelectableDbs() {
        return Arrays.stream(DataSource.valuesNoALLNoMINES()).map(DataSource::realName)
                .filter(s -> !NON_SLECTABLE_LIST.contains(s))
                .map(SearchableDatabases::getDatabase).flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    public static List<CustomDataSources.Source> getNonInSilicoSelectableSources() {
        return CustomDataSources.getSourcesFromNames(
                Arrays.stream(DataSource.valuesNoALLNoMINES()).map(DataSource::realName)
                        .filter(s -> !NON_SLECTABLE_LIST.contains(s)).collect(Collectors.toList()));
    }
}
