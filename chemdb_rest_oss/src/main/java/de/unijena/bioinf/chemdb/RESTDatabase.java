package de.unijena.bioinf.chemdb;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.rest.chemdb.ChemDBClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonException;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RESTDatabase extends AbstractChemicalDatabase {

    private static final boolean IS_USING_ECFP = false;
    private final CloseableHttpClient client;
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
        SHUT_UP_STUPID_LOGGING();
    }


    /*protected URIBuilder getFingerIdURI(String path) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(uri);
        if (path != null && !path.isEmpty())
            builder.setPath(uri.getPath() + path);
        else
            builder.setPath(uri.getPath());
        return builder;
    }*/

    protected BioFilter bioFilter;
    protected File cacheDir;
    protected ChemDBClient chemDBClient;


    public static File defaultCacheDir() {
        final String val = System.getenv("CSI_FINGERID_STORAGE");
        if (val != null) return new File(val);
        return new File(System.getProperty("user.home"), "csi_fingerid_cache");
    }


    public RESTDatabase(@NotNull File cacheDir, @NotNull BioFilter bioFilter, @Nullable URI host, @NotNull CloseableHttpClient client) {
        this.bioFilter = bioFilter;
        this.cacheDir = cacheDir;
        this.chemDBClient = new ChemDBClient(host);
        this.client = client;
    }

    public RESTDatabase(File cacheDir, BioFilter bioFilter, String host, CloseableHttpClient client) {
        this(cacheDir, bioFilter, URI.create(host), client);
    }

    public RESTDatabase(File cacheDir, BioFilter bioFilter, String host) {
        this(cacheDir, bioFilter, host, HttpClients.createDefault());
    }

    public RESTDatabase(File cacheDir, BioFilter bioFilter, URI host) {
        this(cacheDir, bioFilter, host, HttpClients.createDefault());
    }

    public RESTDatabase(File cacheDir, BioFilter bioFilter) {
        this(cacheDir, bioFilter, (URI) null);
    }

    public RESTDatabase(BioFilter bioFilter) {
        this(defaultCacheDir(), bioFilter, (URI) null);
    }

    public BioFilter getBioFilter() {
        return bioFilter;
    }

    public void setBioFilter(BioFilter bioFilter) {
        this.bioFilter = bioFilter;
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        try {
            return chemDBClient.getFormulasDB(mass, deviation, ionType, bioFilter, client);
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    protected FingerprintCandidate wrap(FingerprintCandidate c) {
        return c;
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final ArrayList<CompoundCandidate> candidates = new ArrayList<>();
        for (CompoundCandidate c : lookupStructuresAndFingerprintsByFormula(formula))
            candidates.add(new CompoundCandidate(c));
        return candidates;
    }

    private List<FingerprintCandidate> requestFormula(File output, MolecularFormula formula, BioFilter bioFilter) throws IOException {
        final HttpGet get;
        try {
            if (bioFilter == BioFilter.ALL) throw new IllegalArgumentException();
            get = new HttpGet(getFingerIdURI("/api/compounds/" + formula.toString()).setParameter("db", bioFilter.name()).build());
            get.setConfig(RequestConfig.custom().setConnectTimeout(60000).setContentCompressionEnabled(true).build());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        output.getParentFile().mkdirs();
        final ArrayList<FingerprintCandidate> compounds = new ArrayList<>(100);
        try (CloseableHttpResponse response = client.execute(get)) {

            final File tempFile = File.createTempFile("sirius_formula",".json.gz", output.getParentFile());
            try (final GZIPOutputStream fout = new GZIPOutputStream(new FileOutputStream(tempFile))) {
                try (MultiplexerFileAndIO io = new MultiplexerFileAndIO(response.getEntity().getContent(), fout)) {
                    try (CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(CdkFingerprintVersion.getDefault(), new InputStreamReader(io))) {
                        while (fciter.hasNext())
                            compounds.add(fciter.next());
                    }
                }
            }
            // move tempFile
            if (!output.exists()) {
                if (!tempFile.renameTo(output)) {
                    tempFile.delete();
                }
            }
        }
        return compounds;
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        if (bioFilter != BioFilter.ALL)
            return lookupStructuresAndFingerprintsByFormula(formula, fingerprintCandidates, bioFilter);
        else {
            lookupStructuresAndFingerprintsByFormula(formula, fingerprintCandidates, BioFilter.ONLY_BIO);
            return lookupStructuresAndFingerprintsByFormula(formula, fingerprintCandidates, BioFilter.ONLY_NONBIO);
        }
    }

    protected <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates, BioFilter bioFilter) throws ChemicalDatabaseException {

        final File stfile = new File(cacheDir, (bioFilter == BioFilter.ONLY_BIO ? "bio/" : (bioFilter == BioFilter.ONLY_NONBIO) ? "not-bio/" : "") + formula.toString() + ".json.gz");
        if (stfile.exists()) {
            try {
                final GZIPInputStream zin = new GZIPInputStream(new BufferedInputStream(new FileInputStream(stfile)));
                try (final CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(CdkFingerprintVersion.getDefault(), new InputStreamReader(zin))) {
                    while (fciter.hasNext()) fingerprintCandidates.add(wrap(fciter.next()));
                }
                return fingerprintCandidates;
            } catch (IOException | JsonException e) {
                LoggerFactory.getLogger(RESTDatabase.class).error("Error when searching for " + formula.toString() + " in " + bioFilter.name() + "file database. Deleting cache file '" + stfile.getAbsolutePath() + "' an try fetching from Server");
                stfile.delete();
            }
        }

        try {
            for (FingerprintCandidate fc : requestFormula(stfile, formula, bioFilter)) {
                fingerprintCandidates.add(wrap(fc));
            }
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }

        return fingerprintCandidates;
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        final int n = Iterables.size(inchi_keys);
        final ArrayList<FingerprintCandidate> compounds = new ArrayList<>(Iterables.size(inchi_keys));
        final Iterator<String> keyIter = inchi_keys.iterator();
        for (int i = 0; i < n; i += 1000) {
            try {
                final HttpPost post = new HttpPost(getFingerIdURI("/api/compounds").build());
                StringBuilder buffer = new StringBuilder(Math.min(n - i, 1000) * 15);
                for (int k = 0; keyIter.hasNext() && k < 1000; ++k) {
                    buffer.append(keyIter.next()).append('\n');
                }
                post.setEntity(new StringEntity(buffer.toString(), Charset.forName("UTF-8")));
                try (CloseableHttpResponse response = client.execute(post)) {
                    try (CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(getFingerprintVersion(), new InputStreamReader(response.getEntity().getContent()))) {
                        while (fciter.hasNext())
                            compounds.add(fciter.next());
                    } catch (JsonParsingException e) {
                        final BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                        final StringBuilder buf = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) buf.append(line).append('\n');
                        logger.debug(buf.toString());
                        logger.error(e.getMessage(), e);
                    }

                } catch (IOException e) {
                    throw new ChemicalDatabaseException(e);
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return compounds;
    }

    private FingerprintVersion getFingerprintVersion() {
        if (IS_USING_ECFP) return CdkFingerprintVersion.withECFP();
        else return CdkFingerprintVersion.getDefault();
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


    private static class MultiplexerFileAndIO extends InputStream implements Closeable {

        private final byte[] buffer;
        private final InputStream stream;
        private final OutputStream writer;
        private int offset, limit;
        private boolean closed = false;

        private MultiplexerFileAndIO(InputStream stream, OutputStream writer) throws IOException {
            this.buffer = new byte[1024 * 512];
            this.stream = stream;
            this.writer = writer;
            this.offset = 0;
            this.limit = 0;
            fillCache();
        }

        private boolean fillCache() throws IOException {
            this.limit = stream.read(buffer, 0, buffer.length);
            this.offset = 0;
            if (limit <= 0) return false;
            writer.write(buffer, offset, limit);
            return true;
        }

        @Override
        public int read() throws IOException {
            if (offset >= limit) {
                if (!fillCache()) return -1;
            }
            return buffer[offset++];
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int written = 0;
            while (true) {
                final int bytesAvailable = limit - offset;
                if (bytesAvailable <= 0) {
                    if (!fillCache()) return written;
                }
                final int bytesToRead = len - off;
                if (bytesToRead == 0) return written;
                final int bytesToWrite = Math.min(bytesAvailable, bytesToRead);
                System.arraycopy(buffer, offset, b, off, bytesToWrite);
                written += bytesToWrite;
                off += bytesToWrite;
                offset += bytesToWrite;
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            boolean finished;
            do {
                finished = fillCache();
            } while (finished);
            stream.close();
            writer.close();
            closed = true;
        }
    }

   /* public static void main(String[] args) {
        RESTDatabase rest = new RESTDatabase(BioFilter.ALL);
        System.out.println(rest.chemDBClient.getHost());
        System.out.println(rest.chemDBClient.getPath());
        rest.testConnection();
    }
*/}
