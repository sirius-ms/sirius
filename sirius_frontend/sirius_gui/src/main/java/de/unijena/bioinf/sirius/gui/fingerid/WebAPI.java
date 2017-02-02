/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.fingerid;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ConfidenceScore.QueryPredictor;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.RESTDatabase;
import de.unijena.bioinf.sirius.gui.dialogs.News;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import net.iharder.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParser;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPOutputStream;

public class WebAPI implements Closeable {

    protected final static boolean DEBUG = false;
    public static final String SIRIUS_DOWNLOAD = "https://bio.informatik.uni-jena.de/software/sirius/";


    protected static Logger logger = LoggerFactory.getLogger(WebAPI.class);

    public static PrecursorIonType[] positiveIons = Iterables.toArray(PeriodicTable.getInstance().getKnownLikelyPrecursorIonizations(1), PrecursorIonType.class);
    public static PrecursorIonType[] negativeIons = Iterables.toArray(PeriodicTable.getInstance().getKnownLikelyPrecursorIonizations(-1), PrecursorIonType.class);


    public static RESTDatabase getRESTDb(BioFilter bioFilter) {
        return new RESTDatabase(null, bioFilter, DEBUG ? "http://localhost:8080/frontend" : null);
    }


    public static final String VERSION = "3.4"; //todo get from properties file
    public static final String DATE = "2017-02-02";

    public VersionsInfo needsUpdate() {
        final HttpGet get;
        try {
            get = new HttpGet(getFingerIdURI("/webapi/version.json").build());
            try (CloseableHttpResponse response = client.execute(get)) {
                try (final JsonReader r = Json.createReader(new InputStreamReader(response.getEntity().getContent()))) {

                    JsonObject o = r.readObject();
                    JsonObject gui = o.getJsonObject("SIRIUS GUI");

                    final String id = gui.getString("version");
                    final String date = gui.getString("date");
                    String database = o.getJsonObject("database").getString("version");

                    List<News> newsList = Collections.emptyList();
                    if (o.containsKey("news")){
                        final String newsJson = o.getJsonArray("news").toString();
                        newsList = News.parseJsonNews(newsJson);
                    }

                    return new VersionsInfo(id, date, database, newsList);
                }
            } catch (ClientProtocolException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }
        return null;
    }

    private final CloseableHttpClient client;

    public WebAPI() {
        HttpClientBuilder clientBuilder = HttpClients.custom();
        BasicCredentialsProvider clientCredentials = new BasicCredentialsProvider();
        clientBuilder.setDefaultCredentialsProvider(clientCredentials);

        if (Boolean.valueOf(System.getProperty("de.unijena.bioinf.sirius.proxy"))) {
            HttpHost proxy = new HttpHost(
                    System.getProperty("de.unijena.bioinf.sirius.proxy.hostname"),
                    Integer.valueOf(System.getProperty("de.unijena.bioinf.sirius.proxy.port")),
                    System.getProperty("de.unijena.bioinf.sirius.proxy.scheme"));
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            clientBuilder.setRoutePlanner(routePlanner);

            if (Boolean.getBoolean(System.getProperty("de.unijena.bioinf.sirius.proxy.credentials"))) {
                clientCredentials.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(System.getProperty("de.unijena.bioinf.sirius.proxy.credentials.user"), System.getProperty("de.unijena.bioinf.sirius.proxy.credentials.pw")));
            }
        }
        client = clientBuilder.build();
    }

   /* public static void SHUT_UP_STUPID_LOGGING() {
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");
    }

    static {
        SHUT_UP_STUPID_LOGGING();
    }*/

    protected static URIBuilder getFingerIdURI(String path) {
        URIBuilder b = new URIBuilder().setScheme("http").setHost(DEBUG ? "localhost" : "www.csi-fingerid.org");
        if (DEBUG) b = b.setPort(8080).setPath("/frontend" + path);
        else b.setPath(path);
        return b;
    }

    /*
    public List<Compound> getCompounds(List<String> inchikeys) {
        final URIBuilder b = getFingerIdURI("/webapi/compounds.json");
        try {
            final HttpPost post = new HttpPost(b.build());
            // TODO: implement
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
    */

    public boolean updateJobStatus(FingerIdJob job) throws URISyntaxException, IOException {
        final HttpGet get = new HttpGet(getFingerIdURI("/webapi/job.json").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
        try (CloseableHttpResponse response = client.execute(get)) {
            try (final JsonReader json = Json.createReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent(), ContentType.getOrDefault(response.getEntity()).getCharset())))) {
                final JsonObject obj = json.readObject();
                if (obj.containsKey("prediction")) {
                    final byte[] bytes = Base64.decode(obj.getString("prediction"));
                    final TDoubleArrayList platts = new TDoubleArrayList(2000);
                    final ByteBuffer buf = ByteBuffer.wrap(bytes);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    while (buf.position() < buf.limit()) {
                        platts.add(buf.getDouble());
                    }
                    job.prediction = new ProbabilityFingerprint(job.version, platts.toArray());
                    return true;
                } else {
                    job.state = obj.containsKey("state") ? obj.getString("state") : "SUBMITTED";
                }
            }
        } catch (Throwable t) {
            logger.error("Error when updating job #" + job.jobId, t);
            throw(t);
        }
        return false;
    }

    public FingerIdJob submitJob(final Ms2Experiment experiment, final FTree ftree, MaskedFingerprintVersion version) throws IOException, URISyntaxException {
        final HttpPost post = new HttpPost(getFingerIdURI("/webapi/predict.json").build());
        final String stringMs, jsonTree;
        {
            final JenaMsWriter writer = new JenaMsWriter();
            final StringWriter sw = new StringWriter();
            try (final BufferedWriter bw = new BufferedWriter(sw)) {
                writer.write(bw, experiment);
            }
            stringMs = sw.toString();
        }
        {
            final FTJsonWriter writer = new FTJsonWriter();
            final StringWriter sw = new StringWriter();
            writer.writeTree(sw, ftree);
            jsonTree = sw.toString();
        }

        final NameValuePair ms = new BasicNameValuePair("ms", stringMs);
        final NameValuePair tree = new BasicNameValuePair("ft", jsonTree);

        final UrlEncodedFormEntity params = new UrlEncodedFormEntity(Arrays.asList(ms, tree));
        post.setEntity(params);

        final String securityToken;
        final long jobId;
        // SUBMIT JOB
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                try (final JsonReader json = Json.createReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent(), ContentType.getOrDefault(response.getEntity()).getCharset())))) {
                    final JsonObject obj = json.readObject();
                    securityToken = obj.getString("securityToken");
                    jobId = obj.getInt("jobId");
                    return new FingerIdJob(jobId, securityToken, version);
                }
            } else {
                throw new RuntimeException(response.getStatusLine().getReasonPhrase());
            }
        }
    }

    public Future<ProbabilityFingerprint> predictFingerprint(ExecutorService service, final Ms2Experiment experiment, final FTree tree, final MaskedFingerprintVersion version) {
        return service.submit(new Callable<ProbabilityFingerprint>() {
            @Override
            public ProbabilityFingerprint call() throws Exception {
                final FingerIdJob job = submitJob(experiment, tree, version);
                // RECEIVE RESULTS
                final HttpGet get = new HttpGet(getFingerIdURI("/webapi/job.json").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
                for (int k = 0; k < 600; ++k) {
                    Thread.sleep(3000 + 30 * k);
                    if (updateJobStatus(job)) {
                        return job.prediction;
                    } else if (job.state == "CRASHED") {
                        throw new RuntimeException("Job crashed");
                    }
                }
                throw new TimeoutException("Reached timeout");
            }
        });
    }


    /**
     * make statistics of fingerprints and write the used indizes of fingerprints into the
     * given TIntArrayList (as this property is not contained in FingerprintStatistics)
     *
     * @param fingerprintIndizes
     * @return
     * @throws IOException
     */
    public PredictionPerformance[] getStatistics(TIntArrayList fingerprintIndizes) throws IOException {
        fingerprintIndizes.clear();
        final HttpGet get;
        try {
            get = new HttpGet(getFingerIdURI("/webapi/statistics.csv").build());
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        final TIntArrayList[] lists = new TIntArrayList[5];
        ArrayList<PredictionPerformance> performances = new ArrayList<>();
        try (CloseableHttpResponse response = client.execute(get)) {
            HttpEntity e = response.getEntity();
            final BufferedReader br = new BufferedReader(new InputStreamReader(e.getContent(), ContentType.getOrDefault(e).getCharset()));
            String line; //br.readLine();
            while ((line = br.readLine()) != null) {
                String[] tabs = line.split("\t");
                final int index = Integer.parseInt(tabs[0]);
                PredictionPerformance p = new PredictionPerformance(
                        Double.parseDouble(tabs[1]),
                        Double.parseDouble(tabs[2]),
                        Double.parseDouble(tabs[3]),
                        Double.parseDouble(tabs[4])
                );
                performances.add(p);
                fingerprintIndizes.add(index);
            }
        }
        return performances.toArray(new PredictionPerformance[performances.size()]);
    }

    public List<Compound> getCompoundsFor(MolecularFormula formula, File output, MaskedFingerprintVersion version, boolean bio) throws IOException {
        final HttpGet get;
        try {
            get = new HttpGet(getFingerIdURI("/webapi/compounds/" + (bio ? "bio/" : "not-bio/") + formula.toString() + ".json").build());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final ArrayList<Compound> compounds = new ArrayList<>(100);
        try (CloseableHttpResponse response = client.execute(get)) {
            try (MultiplexerFileAndIO io = new MultiplexerFileAndIO(response.getEntity().getContent(), new GZIPOutputStream(new FileOutputStream(output)))) {
                try (final JsonParser parser = Json.createParser(io)) {
                    return Compound.parseCompounds(version, compounds, parser);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    public QueryPredictor getConfidenceScore(boolean bio) {
        final HttpGet get;
        try {
            get = new HttpGet(getFingerIdURI("/webapi/confidence.json").setParameter("bio", String.valueOf(bio)).build());
            try (CloseableHttpResponse response = client.execute(get)) {
                final BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
                final QueryPredictor qp = QueryPredictor.loadFromStream(br);
                br.close();
                return qp;
            } catch (ClientProtocolException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                return null;
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                return null;
            }
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public <T extends ErrorReport> String reportError(T report, String SOFTWARE_NAME) throws IOException, URISyntaxException {
        final HttpPost request = new HttpPost(getFingerIdURI("/webapi/report.json").build());
        final String json = ErrorReport.toJson(report);

        final NameValuePair reportValue = new BasicNameValuePair("report", json);
        final NameValuePair softwareName = new BasicNameValuePair("name", SOFTWARE_NAME);
        final UrlEncodedFormEntity params = new UrlEncodedFormEntity(Arrays.asList(reportValue, softwareName));
        request.setEntity(params);

        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                final BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
                com.google.gson.JsonObject o = new com.google.gson.JsonParser().parse(br.readLine()).getAsJsonObject();

                boolean suc = o.get("success").getAsBoolean();
                String m = o.get("message").getAsString();

                if (suc) {
                    LoggerFactory.getLogger(this.getClass()).info(m);
                } else {
                    LoggerFactory.getLogger(this.getClass()).error(m);
                }
                return m;
            } else {
                RuntimeException e = new RuntimeException(response.getStatusLine().getReasonPhrase());
                LoggerFactory.getLogger(this.getClass()).error("Could not send error report! Bad http return Value: " + response.getStatusLine().getStatusCode(), e);
                throw e;
            }
        }
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

}
