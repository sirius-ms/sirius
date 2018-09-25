package de.unijena.bioinf.fingerid.db;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.net.VersionsInfo;
import de.unijena.bioinf.fingerid.net.WebAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is still in progress. Should at some point replace the database code in CLI and GUI.
 */
public class CachedRESTDB {
    public static final String BIO_DB_DIR = "bio";
    public static final String NON_BIO_DB_DIR = "not-bio";
    public static final String CUSTOM_DB_DIR = "custom";

    protected static Logger logger = LoggerFactory.getLogger(CachedRESTDB.class);

    protected File directory;
    protected MaskedFingerprintVersion fingerprintVersion;
    protected HashMap<String, FileCompoundStorage> customDatabases;
    protected VersionsInfo versionsInfo;

    public CachedRESTDB(VersionsInfo versionsInfo, MaskedFingerprintVersion fingerprintVersion, File dir) {
        this.versionsInfo = versionsInfo;
        this.fingerprintVersion = fingerprintVersion;
        this.directory = dir;
        this.customDatabases = new HashMap<>();
    }


    public void checkCache() throws IOException {
        if (isOutdated())
            destroyCache();
    }

    public boolean isOutdated() {
        final File f = new File(directory, "version");
        if (f.exists()) {
            try {
                final List<String> content = Files.readAllLines(f.toPath(), Charset.forName("UTF-8"));
                if (content.size() > 0 && !versionsInfo.databaseOutdated(content.get(0))) return false;
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        }
        return true;
    }

    public FingerblastSearchEngine getSearchEngine(SearchableDatabase db) {
        return new FingerblastSearchEngine(this, db);
    }


    public void destroyCache() throws IOException {
        final File bio = getBioDirectory(directory);
        final File nonBio = getNonBioDirectory(directory);
        if (bio.exists() || nonBio.exists()) {
            logger.info("Destroy database cache, due to updated online database.");
        }
        if (bio.exists()) {
            for (File f : bio.listFiles()) {
                Files.deleteIfExists(f.toPath());
            }
        }
        if (nonBio.exists()) {
            for (File f : nonBio.listFiles()) {
                Files.deleteIfExists(f.toPath());
            }
        }
        if (!directory.exists()) {
            directory.mkdirs();
            bio.mkdirs();
            nonBio.mkdirs();
        }
        try (BufferedWriter bw = Files.newBufferedWriter(new File(directory, "version").toPath(), Charset.forName("UTF-8"))) {
            bw.write(versionsInfo.databaseDate);
        }
    }

    public List<FingerprintCandidate> loadCompoundsByFormula(MolecularFormula formula, SearchableDatabase db) throws IOException {
        final List<FingerprintCandidate> candidates = new ArrayList<>();
        try {
            if (db.searchInPubchem()) {
                search(WebAPI.INSTANCE, formula, candidates, true);
                search(WebAPI.INSTANCE, formula, candidates, false);
            } else if (db.searchInBio()) {
                search(WebAPI.INSTANCE, formula, candidates, true);
            }
            if (db.isCustomDb())
                search(formula, candidates, getCustomDb(db));
        } catch (DatabaseException e) {
            throw new IOException(e);
        }
        return mergeCompounds(candidates);
    }

    protected FileCompoundStorage getCustomDb(SearchableDatabase db) throws IOException {
        if (customDatabases.containsKey(db.name()))
            return customDatabases.get(db.name());
        else {
            final FileCompoundStorage custom = new FileCompoundStorage(db.getDatabasePath(), fingerprintVersion.getMaskedFingerprintVersion());
            customDatabases.put(db.name(), custom);
            return custom;
        }
    }

    private void search(WebAPI webAPI, MolecularFormula formula, List<FingerprintCandidate> candidates, boolean isBio) throws IOException, DatabaseException {
        final File f = new File(isBio ? getBioDirectory(directory) : getNonBioDirectory(directory), formula.toString() + ".json.gz");
        if (f.exists()) {
            try {
                parseJson(f, candidates);
                return;
            } catch (Exception e) {
                LoggerFactory.getLogger(CachedRESTDB.class).error(e.getMessage(), e);
                f.delete();
            }
        }
        try (final RESTDatabase restDb = webAPI.getRESTDb(isBio ? BioFilter.ONLY_BIO : BioFilter.ONLY_NONBIO, directory)) {
            candidates.addAll(restDb.lookupStructuresAndFingerprintsByFormula(formula));
        }
    }

    private void search(MolecularFormula formula, List<FingerprintCandidate> candidates, FileCompoundStorage db) throws IOException, DatabaseException {
        candidates.addAll(db.lookupStructuresAndFingerprintsByFormula(formula));
    }

    private void parseJson(File f, List<FingerprintCandidate> candidates) throws IOException {
        try (final BufferedReader reader = FileUtils.getReader(f)) {
            final JsonObject obj = Json.createReader(reader).readObject();
            final JsonArray array = obj.getJsonArray("compounds");
            for (int i = 0; i < array.size(); ++i) {
                candidates.add(FingerprintCandidate.fromJSON(fingerprintVersion.getMaskedFingerprintVersion(), array.getJsonObject(i)));
            }
        }
    }


    public void setDirectory(File directory) {
        this.directory = directory;
    }

    /**
     * merge compounds with same InChIKey
     */
    private List<FingerprintCandidate> mergeCompounds(List<FingerprintCandidate> compounds) {
        final HashMap<String, FingerprintCandidate> cs = new HashMap<>();
        for (FingerprintCandidate c : compounds) {
            FingerprintCandidate x = cs.get(c.getInchiKey2D());
            if (x != null) {
                // TODO: merge database links
            } else {
                cs.put(c.getInchi().key2D(), c);
            }
        }
        return new ArrayList<>(cs.values());
    }

    public static File getBioDirectory(final File root) {
        return new File(root, CachedRESTDB.BIO_DB_DIR);
    }

    public static File getNonBioDirectory(final File root) {
        return new File(root, CachedRESTDB.NON_BIO_DB_DIR);
    }

    public static File getCustomDatabaseDirectory(final File root) {
        //todo make user changeable?
        return new File(root, CUSTOM_DB_DIR);
    }


}
