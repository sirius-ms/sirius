package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.ms.rest.client.chemdb.ChemDBClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonException;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RESTDatabase extends AbstractChemicalDatabase {
    private static Logger logger = LoggerFactory.getLogger(RESTDatabase.class);


    public static void SHUT_UP_STUPID_LOGGING() {
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");
    }


    static {
        FingerIDProperties.fingeridVersion();
//        SHUT_UP_STUPID_LOGGING();
    }


    private final CloseableHttpClient client;
    protected ChemDBClient chemDBClient;
    protected File cacheDir;

    protected long filter;


    public static File defaultCacheDir() {
        final String val = System.getenv("CSI_FINGERID_STORAGE");
        if (val != null) return new File(val);
        return new File(System.getProperty("user.home"), "csi_fingerid_cache");
    }


    public RESTDatabase(@Nullable File cacheDir, long filter, @NotNull ChemDBClient chemDBClient, @NotNull CloseableHttpClient client) {
        this.filter = filter;
        this.cacheDir = cacheDir != null ? cacheDir : defaultCacheDir();
        this.chemDBClient = chemDBClient;
        this.client = client;
    }

    public RESTDatabase(@NotNull File cacheDir, long filter, @Nullable URI host, @NotNull CloseableHttpClient client) {
        this(cacheDir, filter, new ChemDBClient(host), client);
    }

    public RESTDatabase(File cacheDir, long filter, String host, CloseableHttpClient client) {
        this(cacheDir, filter, URI.create(host), client);
    }

    public RESTDatabase(File cacheDir, long filter, String host) {
        this(cacheDir, filter, host, HttpClients.createDefault());
    }

    public RESTDatabase(File cacheDir, long filter, URI host) {
        this(cacheDir, filter, host, HttpClients.createDefault());
    }

    public RESTDatabase(File cacheDir, long filter) {
        this(cacheDir, filter, (URI) null);
    }

    public RESTDatabase(long filter) {
        this(defaultCacheDir(), filter, (URI) null);
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        try {
            return chemDBClient.getFormulasDB(mass, deviation, ionType, filter, client);
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final ArrayList<CompoundCandidate> candidates = new ArrayList<>();
        for (CompoundCandidate c : lookupStructuresAndFingerprintsByFormula(formula))
            candidates.add(new CompoundCandidate(c));
        return candidates;
    }


    /*@Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        if (bioFilter == BioFilter.ALL) {
            lookupStructuresAndFingerprintsByFormula(formula, fingerprintCandidates, BioFilter.ONLY_BIO);
            return lookupStructuresAndFingerprintsByFormula(formula, fingerprintCandidates, BioFilter.ONLY_NONBIO);
        } else {
            return lookupStructuresAndFingerprintsByFormula(formula, fingerprintCandidates, bioFilter);
        }
    }*/

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        final File stfile = new File(cacheDir, "/" + formula.toString() + ".json.gz");
        try {
            List<FingerprintCandidate> fpcs = new ArrayList<>();
            if (stfile.exists()) {
                try {
                    final GZIPInputStream zin = new GZIPInputStream(new BufferedInputStream(new FileInputStream(stfile)));
                    try (final CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(CdkFingerprintVersion.getDefault(), new InputStreamReader(zin))) {
                        while (fciter.hasNext())
                            fpcs.add(fciter.next());
                    }
                } catch (IOException | JsonException e) {
                    LoggerFactory.getLogger(RESTDatabase.class).error("Error when searching for " + formula.toString() + " in file database. Deleting cache file '" + stfile.getAbsolutePath() + "' an try fetching from Server");
                    stfile.delete();
                    fpcs = requestFormula(stfile, formula);
                }
            } else {
                fpcs = requestFormula(stfile, formula);
            }

            fingerprintCandidates.addAll(
                    fpcs.stream().filter(ChemDBs.inFilter((it)-> it.bitset,filter)).collect(Collectors.toList()));
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    /*protected <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates, EnumSet<DataSource> filter) throws ChemicalDatabaseException {
        final File stfile = new File(cacheDir, "all/" + formula.toString() + ".json.gz");
//        switch (bioFilter) {
//            case ONLY_BIO:
//                stfile = new File(cacheDir, "bio/" + formula.toString() + ".json.gz");
//                break;
//            case ONLY_NONBIO:
//                stfile = new File(cacheDir, "not-bio/" + formula.toString() + ".json.gz");
//                break;
//            default:
//                throw new IllegalArgumentException("Only 'BioFilter.ONLY_BIO' and 'BioFilter.ONLY_NONBIO' are not allowed here!");
//        }

        //return result from cach
        if (stfile.exists()) {
            try {
                final GZIPInputStream zin = new GZIPInputStream(new BufferedInputStream(new FileInputStream(stfile)));
                try (final CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(CdkFingerprintVersion.getDefault(), new InputStreamReader(zin))) {
                    while (fciter.hasNext())
                        fingerprintCandidates.add(fciter.next());
                }
                return fingerprintCandidates;
            } catch (IOException | JsonException e) {
                LoggerFactory.getLogger(RESTDatabase.class).error("Error when searching for " + formula.toString() + " in " + bioFilter.name() + "file database. Deleting cache file '" + stfile.getAbsolutePath() + "' an try fetching from Server");
                stfile.delete();
            }
        }else {

        }

        try {
            fingerprintCandidates.addAll(requestFormula(stfile, formula, bioFilter));
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
        return fingerprintCandidates;
    }*/

    private List<FingerprintCandidate> requestFormula(final @NotNull File output, MolecularFormula formula) throws IOException {
        //get unfiltered list from server to write cache.
        final List<FingerprintCandidate> fpcs = chemDBClient.getCompounds(formula, DataSource.ALL.flag(), client);

        // write cache in background -> cache has to be unfiltered
        SiriusJobs.runInBackground(() -> {
            output.getParentFile().mkdirs();
            final File tempFile = File.createTempFile("sirius_formula", ".json.gz", output.getParentFile());
            try {
                try (final GZIPOutputStream fout = new GZIPOutputStream(new FileOutputStream(tempFile))) {
                    try (final BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fout))) {
                        FingerprintCandidate.toJSONList(fpcs, br);
                    }
                }

                // move tempFile is canonical on same fs
                if (!output.exists())
                    if (!tempFile.renameTo(output))
                        tempFile.delete();

                return true;
            } finally {
                Files.deleteIfExists(tempFile.toPath());
            }
        });

        return fpcs;
    }


    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        final Partition<String> keyParts = Partition.ofSize(inchi_keys, ChemDBClient.MAX_NUM_OF_INCHIS);
        final ArrayList<FingerprintCandidate> compounds = new ArrayList<>(keyParts.numberOfElements());

        try {
            for (List<String> inchiKeys : keyParts)
                compounds.addAll(chemDBClient.postCompounds(inchiKeys, client));
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
        return compounds;
    }



    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return lookupFingerprintsByInchis(inchi_keys);
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
        // already annotated
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
