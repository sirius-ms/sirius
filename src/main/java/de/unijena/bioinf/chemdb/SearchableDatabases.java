package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.properties.PropertyManager;
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
    private SearchableDatabases() {
    }

    public static File getDatabaseDirectory() {
        final String val = PropertyManager.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        return Paths.get(val).toFile();
    }

    public static File getRESTDatabaseCacheDirectory() {
        return RestWithCustomDatabase.getRestDBCacheDir(getDatabaseDirectory());
    }

    public static File getCustomDatabaseDirectory() {
        return RestWithCustomDatabase.getCustomDBDirectory(getDatabaseDirectory());
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
            return CustomDatabase.loadCustomDatabaseFromLocation(dbDir.toFile(), true);
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
        return CustomDatabase.loadCustomDatabases(up2date);
    }

    public static RestWithCustomDatabase makeRestWithCustomDB(WebAPI webAPI) {
        return new RestWithCustomDatabase(webAPI, getDatabaseDirectory());
    }

    @NotNull
    public static List<SearchableDatabase> getAvailableDatabases() {
        final List<SearchableDatabase> db = Stream.of(DataSource.values()).map(DataSource::realName).map(SearchableDatabases::getDatabaseByNameOrThrow).collect(Collectors.toList());
        Collections.swap(db,2, DataSource.BIO.ordinal()); //just to put bio on index 3
        db.addAll(getCustomDatabases());
        return db;
    }

    public static SearchableDatabase getAllDb() {
        return getDatabaseByNameOrThrow(DataSource.ALL.name());
    }

    public static SearchableDatabase getBioDb() {
        return getDatabaseByNameOrThrow(DataSource.BIO.name());
    }
}
