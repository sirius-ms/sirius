package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.WebAPI;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SearchableDatabases {
    private SearchableDatabases() {
    }


    public static File getDatabaseDirectory() {
        final String val = PropertyManager.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        return Paths.get(val).toFile();
    }

    public static File getBioDirectory() {
        return CachedRESTDB.getBioDirectory(getDatabaseDirectory());
    }

    public static File getNonBioDirectory() {
        return CachedRESTDB.getNonBioDirectory(getDatabaseDirectory());
    }

    public static File getCustomDatabaseDirectory() {
        return CachedRESTDB.getCustomDatabaseDirectory(getDatabaseDirectory());
    }

    @NotNull
    public static SearchableDatabase getDatabaseByName(@NotNull String name) {
        final DataSource source = DatasourceService.getSourceFromName(name);
        if (source != null)
            return new SearchableDbOnDisc(source.realName, getBioDirectory(), true, source.isBio(), false);

        @NotNull List<CustomDatabase> custom = getCustomDatabases();
        for (CustomDatabase customDatabase : custom)
            if (customDatabase.name().equalsIgnoreCase(name))
                return customDatabase;

        throw new IllegalArgumentException("Database with name: " + name + " does not exist!");
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases() {
        return getCustomDatabases(true);
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases(final boolean up2date) {
        return CustomDatabase.loadCustomDatabases(up2date);
    }

    public static CachedRESTDB makeCachedRestDB(WebAPI webAPI) {
        return new CachedRESTDB(webAPI, getDatabaseDirectory());
    }

    @NotNull
    public static List<SearchableDatabase> getAvailableDatabases() {
        final List<SearchableDatabase> db = new ArrayList<>();
        db.add(getPubchemDb());
        db.add(getBioDb());
        db.addAll(getCustomDatabases());

        return db;
    }

    public static SearchableDatabase getPubchemDb() {
        return getDatabaseByName(DataSource.PUBCHEM.realName);
    }

    public static SearchableDatabase getBioDb() {
        return getDatabaseByName(DataSource.BIO.realName);
    }

}
