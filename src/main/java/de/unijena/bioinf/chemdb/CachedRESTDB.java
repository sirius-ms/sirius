package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.WebAPI;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    protected HashMap<String, FileCompoundStorage> customDatabases;

    protected final WebAPI api;
    private VersionsInfo versionInfoCache = null;

    public CachedRESTDB(WebAPI api, File dir) {
        this.api = api;
        this.directory = dir;
        this.customDatabases = new HashMap<>();
    }


    public void checkCache() throws IOException {
        if (isOutdated())
            destroyCache();
    }

    protected VersionsInfo versionInfo() {
        if (versionInfoCache == null)
            versionInfoCache = api.getVersionInfo();
        return versionInfoCache;
    }

    public boolean isOutdated() {
        final File f = new File(directory, "version");
        if (f.exists()) {
            try {
                final List<String> content = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                if (content.size() > 0 && !versionInfo().databaseOutdated(content.get(0))) return false;
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        }
        return true;
    }

    public FingerblastSearchEngine getSearchEngine(SearchableDatabase db) {
        return new FingerblastSearchEngine(this, db);
    }


    public synchronized void destroyCache() throws IOException {

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
        try (BufferedWriter bw = Files.newBufferedWriter(new File(directory, "version").toPath(), StandardCharsets.UTF_8)) {
            bw.write(versionInfo().databaseDate);
        }
    }

    public List<FingerprintCandidate> loadCompoundsByFormula(MolecularFormula formula, SearchableDatabase db) throws IOException {
        final List<FingerprintCandidate> candidates = new ArrayList<>();
        try {
            if (db.searchInPubchem()) {
                search(api, formula, candidates, false);
            } else if (db.searchInBio()) {
                search(api, formula, candidates, true);
            }
            if (db.isCustomDb())
                search(formula, candidates, getCustomDb(db));
        } catch (ChemicalDatabaseException e) {
            throw new IOException("Could not lookup formula: " + formula.toString(), e);
        }
        return mergeCompounds(candidates);
    }

    protected FileCompoundStorage getCustomDb(SearchableDatabase db) throws IOException {
        if (customDatabases.containsKey(db.name()))
            return customDatabases.get(db.name());
        else {
            final FileCompoundStorage custom = new FileCompoundStorage(db.getDatabasePath(), api.getCDKChemDBFingerprintVersion());
            customDatabases.put(db.name(), custom);
            return custom;
        }
    }

    private synchronized void search(WebAPI webAPI, MolecularFormula formula, List<FingerprintCandidate> candidates, boolean isBio) throws IOException, ChemicalDatabaseException {
        final File f = new File(isBio ? getBioDirectory(directory) : getNonBioDirectory(directory), formula.toString() + ".json.gz");
        if (f.exists()) {

            synchronized (this) {
                try {
                    parseJson(f, candidates);
                    return;
                } catch (Throwable e) {
                    LoggerFactory.getLogger(CachedRESTDB.class).error(e.getMessage(), e);
                    f.delete();
                }
            }
        }
        try (final RESTDatabase restDb = webAPI.getRESTDb(isBio ? BioFilter.ONLY_BIO : BioFilter.ONLY_NONBIO, directory)) {
            candidates.addAll(restDb.lookupStructuresAndFingerprintsByFormula(formula));
        }
    }

    private void search(MolecularFormula formula, List<FingerprintCandidate> candidates, FileCompoundStorage db) throws IOException, ChemicalDatabaseException {
        candidates.addAll(db.lookupStructuresAndFingerprintsByFormula(formula));
    }

    private void parseJson(File f, List<FingerprintCandidate> candidates) throws IOException {
        try (final BufferedReader reader = FileUtils.getReader(f)) {
            final JsonObject obj = Json.createReader(reader).readObject();
            final JsonArray array = obj.getJsonArray("compounds");
            for (int i = 0; i < array.size(); ++i) {
                candidates.add(FingerprintCandidate.fromJSON(api.getCDKChemDBFingerprintVersion(), array.getJsonObject(i)));
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
