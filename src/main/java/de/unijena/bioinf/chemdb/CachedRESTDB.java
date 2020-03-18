package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.WebAPI;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is still in progress. Should at some point replace the database code in CLI and GUI.
 */
public class CachedRESTDB {
    public static final String REST_CACHE_DIR = "rest"; //chache directory for all rest dbs
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

    public FingerblastSearchEngine makeSearchEngine(SearchableDatabase db) {
        return makeSearchEngine(List.of(db));
    }

    public FingerblastSearchEngine makeSearchEngine(Collection<SearchableDatabase> dbs) {
        return new FingerblastSearchEngine(this, dbs);
    }


    public synchronized void destroyCache() throws IOException {
        final File all = getRESTDatabaseCacheDirectory(directory);
        if (all.exists()) {
            for (File f : all.listFiles()) {
                Files.deleteIfExists(f.toPath());
            }
        }

        if (!directory.exists()) {
            directory.mkdirs();
            all.mkdirs();
        }
        try (BufferedWriter bw = Files.newBufferedWriter(new File(directory, "version").toPath(), StandardCharsets.UTF_8)) {
            bw.write(versionInfo().databaseDate);
        }
    }

    public List<FingerprintCandidate> loadCompoundsByFormula(MolecularFormula formula, Collection<SearchableDatabase> dbs) throws IOException {
        if (dbs == null || dbs.isEmpty())
            throw new IllegalArgumentException("No search DB given!");

        try {
            List<FingerprintCandidate> result = new ArrayList<>();

            final long filter = dbs.stream().filter(SearchableDatabase::isRestDb).
                    mapToLong(SearchableDatabase::getFilterFlag).reduce((a, b) -> a |= b).orElse(-1);

            if (filter >= 0)
                result.addAll(search(api, formula, filter));

            for (CustomDatabase cdb : dbs.stream().filter(SearchableDatabase::isCustomDb).distinct().map(it -> (CustomDatabase) it).collect(Collectors.toList()))
                result.addAll(search(formula, getCustomDb(cdb)));

            return mergeCompounds(result);
        } catch (ChemicalDatabaseException e) {
            throw new IOException("Could not lookup formula: " + formula.toString(), e);
        }
    }

    protected FileCompoundStorage getCustomDb(CustomDatabase db) throws IOException {
        if (customDatabases.containsKey(db.name()))
            return customDatabases.get(db.name());
        else {
            final FileCompoundStorage custom = new FileCompoundStorage(db.getDatabasePath(), api.getCDKChemDBFingerprintVersion());
            customDatabases.put(db.name(), custom);
            return custom;
        }
    }

    private List<FingerprintCandidate> search(WebAPI webAPI, MolecularFormula formula, long filterFlags) throws IOException {
        try (final RESTDatabase restDb = webAPI.getRESTDb(filterFlags, directory)) {
            return restDb.lookupStructuresAndFingerprintsByFormula(formula);
        }
    }

    private List<FingerprintCandidate> search(MolecularFormula formula, FileCompoundStorage db) throws IOException {
        return db.lookupStructuresAndFingerprintsByFormula(formula);
    }

    /**
     * merge compounds with same InChIKey
     */
    public static List<FingerprintCandidate> mergeCompounds(List<FingerprintCandidate> compounds) {
        final HashMap<String, FingerprintCandidate> cs = new HashMap<>();
        for (FingerprintCandidate c : compounds) {
            FingerprintCandidate x = cs.get(c.getInchiKey2D());
            if (x != null) {
                x.mergeDBLinks(c.links);
                x.mergeBits(c.bitset);
            } else {
                cs.put(c.getInchi().key2D(), c);
            }
        }
        return new ArrayList<>(cs.values());
    }
    /*public static List<FingerprintCandidate> mergeCompounds(List<FingerprintCandidate> compounds) {
        HashMap<String, FingerprintCandidate> it = new HashMap<>();
        mergeCompounds(compounds, it);
        return new ArrayList<>(it.values());
    }

    public static Set<String> mergeCompounds(List<FingerprintCandidate> compounds, final HashMap<String, FingerprintCandidate> mergeMap) {
        final Set<String> addedInChIs = new HashSet<>(compounds.size());
        for (FingerprintCandidate c : compounds) {
            final String key = c.getInchiKey2D();
            addedInChIs.add(key);
            FingerprintCandidate x = mergeMap.get(key);

            if (x != null) {
                x.mergeDBLinks(c.links);
                x.mergeBits(c.bitset);
            } else {
                mergeMap.put(c.getInchi().key2D(), c);
            }
        }
        return addedInChIs;
    }

    protected static class CandidateResult {
        final HashMap<String, FingerprintCandidate> cs = new HashMap<>();
        final HashMap<String, Set<String>> customInChIs = new HashMap<>();
        final Set<String> restDbInChIs;
        final long requestFilter;
        final long restFilter;

        protected CandidateResult() {
            restDbInChIs = Collections.emptySet();
            restFilter = -1;
            requestFilter = -1;
        }

        protected CandidateResult(List<FingerprintCandidate> compounds, long appliedFilter, long requestFilter) {
            restFilter = appliedFilter;
            this.requestFilter = requestFilter;
            restDbInChIs = mergeCompounds(compounds, cs);
        }

        private void addCustom(String name, List<FingerprintCandidate> compounds) {
            if (customInChIs.containsKey(name))
                throw new IllegalArgumentException("Custom db already exists: '" + name + "'");
            customInChIs.put(name, mergeCompounds(compounds, cs));
        }

        public List<FingerprintCandidate> getCombined() {
            return new ArrayList<>(cs.values());
        }

        public List<FingerprintCandidate> getRequestedOnly() {
            if (requestFilter > -1) {
                if (requestFilter == restFilter)
                    return getCombined();
                else
                    return Stream.concat(restDbInChIs.stream(), customInChIs.values().stream().flatMap(Set::stream)).
                            distinct().map(cs::get).collect(Collectors.toList());
            } else {
                return customInChIs.values().stream().flatMap(Set::stream).
                        distinct().map(cs::get).collect(Collectors.toList());
            }
        }

        public Optional<List<FingerprintCandidate>> getAllDbOnly() {
            if (!containsAllDb())
                return Optional.empty();
            return Optional.of(restDbInChIs.stream().map(cs::get).collect(Collectors.toList()));
        }

        public boolean containsAllDb() {
            return restFilter == 0;
        }
    }*/


    @NotNull
    public static File getRESTDatabaseCacheDirectory(final File root) {
        return new File(root, REST_CACHE_DIR);
    }

    @NotNull
    public static File getCustomDatabaseDirectory(final File root) {
        return new File(root, CUSTOM_DB_DIR);
    }


}
