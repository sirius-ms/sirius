package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.WebAPI;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CustomDatabase implements SearchableDatabase {
    protected static Logger logger = LoggerFactory.getLogger(CustomDatabase.class);

    protected String name;
    protected File path;


    // statistics
    protected long numberOfCompounds, numberOfFormulas, megabytes;

    public void deleteDatabase() {
        synchronized (this) {
            if (path.exists()) {
                for (File f : path.listFiles()) {
                    f.delete();
                }
                path.delete();
                CustomDataSourceService.removeCustomSource(name);
            }
        }
    }


    protected boolean deriveFromPubchem, deriveFromBioDb;
    protected CdkFingerprintVersion version;
    protected int databaseVersion;

    public static CustomDatabase createNewDatabase(String name, File path, CdkFingerprintVersion version) {
        CustomDatabase db = new CustomDatabase(name, path);
        db.databaseVersion = VersionsInfo.CUSTOM_DATABASE_SCHEMA;
        db.version = version;
        return db;
    }

    @NotNull
    public static List<CustomDatabase> loadCustomDatabases(boolean up2date) {
        final List<CustomDatabase> databases = new ArrayList<>();
        final File custom = SearchableDatabases.getCustomDatabaseDirectory();
        if (!custom.exists()) {
            return databases;
        }
        for (File subDir : custom.listFiles()) {
            try {
                final CustomDatabase db = loadCustomDatabaseFromLocation(subDir, up2date);
                databases.add(db);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
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
            throw new OutdatedDBExeption("DB '" + db.name + "' is outdated (DB-Version: " + db.databaseVersion + " vs. ReqVersion: " + VersionsInfo.CUSTOM_DATABASE_SCHEMA + ") . PLease reimport the structures. ");
        }
        throw new IOException("Illegal DB location. DB location needs to be a directory.");
    }

    public CustomDatabase(String name, File path) {
        this.name = name;
        this.path = path;
        CustomDataSourceService.addCustomSourceIfAbsent(this.name);
    }

    public boolean needsUpgrade() {
        return databaseVersion != VersionsInfo.CUSTOM_DATABASE_SCHEMA;
    }

    public void inheritMetadata(File otherDb) throws IOException {
        // should be done automatically
    }

    public boolean isDeriveFromPubchem() {
        return deriveFromPubchem;
    }

    public void setDeriveFromPubchem(boolean deriveFromPubchem) {
        this.deriveFromPubchem = deriveFromPubchem;
    }

    public boolean isDeriveFromBioDb() {
        return deriveFromBioDb;
    }

    public void setDeriveFromBioDb(boolean deriveFromBioDb) {
        this.deriveFromBioDb = deriveFromBioDb;
    }

    public void readSettings() throws IOException {
        synchronized (this) {
            if (settingsFile().exists()) {
                deriveFromPubchem = false;
                deriveFromBioDb = false;
                try (FileReader r = new FileReader(settingsFile())) {
                    JsonObject o = Json.createReader(r).readObject();
                    JsonArray ary = o.getJsonArray("inheritance");
                    if (ary != null) {
                        for (JsonValue v : ary) {
                            if (v instanceof JsonString) {
                                final String s = ((JsonString) v).getString();
                                if (s.equals(DataSource.PUBCHEM.realName))
                                    deriveFromPubchem = true;
                                if (s.equals(DataSource.BIO.realName))
                                    deriveFromBioDb = true;
                            }
                        }
                    }
                    JsonArray fpAry = o.getJsonArray("fingerprintVersion");
                    if (fpAry == null) {
                        this.version = CdkFingerprintVersion.getDefault();
                    } else {
                        final List<CdkFingerprintVersion.USED_FINGERPRINTS> usedFingerprints = new ArrayList<>();
                        for (JsonValue v : fpAry) {
                            if (v instanceof JsonString) {
                                try {
                                    usedFingerprints.add(CdkFingerprintVersion.USED_FINGERPRINTS.valueOf(((JsonString) v).getString().toUpperCase()));
                                } catch (IllegalArgumentException e) {
                                    throw new RuntimeException("Unknown fingerprint type '" + ((JsonString) v).getString() + "'");
                                }
                            }
                        }
                        this.version = new CdkFingerprintVersion(usedFingerprints.toArray(new CdkFingerprintVersion.USED_FINGERPRINTS[usedFingerprints.size()]));
                    }
                    JsonNumber num = o.getJsonNumber("schemaVersion");
                    if (num == null) {
                        this.databaseVersion = 0;
                    } else {
                        this.databaseVersion = num.intValue();
                    }
                    JsonObject stats = o.getJsonObject("statistics");
                    if (stats != null) {
                        JsonNumber nc = stats.getJsonNumber("compounds");
                        if (nc != null)
                            this.numberOfCompounds = nc.intValue();
                    }
                }
                // number of formulas and file size
                long filesize = 0;
                int ncompounds = 0;
                if (getDatabasePath().exists()) {
                    for (File f : getDatabasePath().listFiles()) {
                        filesize += Files.size(f.toPath());
                        ncompounds += 1;
                    }
                    --ncompounds;
                }
                this.megabytes = Math.round((filesize / 1024d) / 1024d);
                this.numberOfFormulas = ncompounds;
            }
        }
    }

    public CustomDatabaseImporter getImporter(@NotNull final WebAPI api, int bufferSize) {
        return new CustomDatabaseImporter(this, version, api, bufferSize);
    }

    protected File settingsFile() {
        return new File(path, "settings.json");
    }

    public void setFingerprintVersion(CdkFingerprintVersion version) {
        this.version = version;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean searchInPubchem() {
        return deriveFromPubchem;
    }

    @Override
    public boolean searchInBio() {
        return deriveFromBioDb || deriveFromPubchem;
    }

    @Override
    public boolean isCustomDb() {
        return true;
    }

    @Override
    public File getDatabasePath() {
        return path;
    }

    @Override
    public String toString() {
        return name;
    }

    public long getNumberOfCompounds() {
        return numberOfCompounds;
    }

    public long getNumberOfFormulas() {
        return numberOfFormulas;
    }

    public long getMegabytes() {
        return megabytes;
    }

    public void buildDatabase(List<File> files, CustomDatabaseImporter.Listener listener, @NotNull WebAPI api, int bufferSize) throws IOException, CDKException {
        final CustomDatabaseImporter importer = getImporter(api, bufferSize);
        importer.init();
        importer.addListener(listener);
        for (File f : files) {
            importer.importFrom(f);
        }
        importer.flushBuffer();
    }

    static class Comp {
        String inchikey;
        IAtomContainer molecule;
        FingerprintCandidate candidate;

        Comp(String inchikey) {
            this.inchikey = inchikey;
        }
    }
}
