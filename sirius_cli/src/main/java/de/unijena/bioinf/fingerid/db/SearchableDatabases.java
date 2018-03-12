package de.unijena.bioinf.fingerid.db;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.fingerid.net.VersionsInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SearchableDatabases {
    private static SearchableDbOnDisc pubchem = null;
    private static SearchableDbOnDisc bio = null;

    private SearchableDatabases() {
    }


    public static File getDatabaseDirectory() {
        final String val = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
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
    public static List<SearchableDatabase> getAvailableDatabases() {
        final List<SearchableDatabase> db = new ArrayList<>();
        db.add(getPubchemDb());
        db.add(getBioDb());
        db.addAll(getCustomDatabases());

        return db;
    }

    @NotNull
    public static SearchableDatabase getBioDb() {
        if (bio == null)
            bio = new SearchableDbOnDisc("biological database", getBioDirectory(), false, true, false);

        return bio;
    }

    @NotNull
    public static SearchableDatabase getPubchemDb() {
        if (pubchem == null)
            pubchem = new SearchableDbOnDisc("PubChem", getBioDirectory(), true, true, false);

        return pubchem;
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases() {
        return getCustomDatabases(true);
    }

    @NotNull
    public static List<CustomDatabase> getCustomDatabases(final boolean up2date) {
        return CustomDatabase.customDatabases(up2date);
    }

    public static CachedRESTDB makeCachedRestDB(VersionsInfo versionsInfo, MaskedFingerprintVersion fingerprintVersion) {
        return new CachedRESTDB(versionsInfo, fingerprintVersion, getDatabaseDirectory());
    }

    public static void invalidateCache() {
        pubchem = null;
        bio = null;
    }


}
