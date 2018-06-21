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

package de.unijena.bioinf.fingerid.net;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.RESTDatabase;
import de.unijena.bioinf.fingerid.blast.CovarianceScoring;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.net.ProxyManager;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import de.unijena.bioinf.utils.systemInfo.SystemInformation;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import net.iharder.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class WebAPI implements Closeable {
    private static final LinkedHashSet<WebAPI> INSTANCES = new LinkedHashSet<>();
    private static final BasicNameValuePair UID = new BasicNameValuePair("uid", SystemInformation.generateSystemKey());

    public static final DefaultArtifactVersion VERSION = new DefaultArtifactVersion(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.version"));
    public static final String SIRIUS_DOWNLOAD = "https://bio.informatik.uni-jena.de/software/sirius/";
    public static final String FINGERID_WEB_API = FingerIDProperties.fingeridWebHost();


    public static WebAPI newInstance() {
        WebAPI i = new WebAPI();
        INSTANCES.add(i);
        return i;
    }

    public static void reconnectAllInstances() {
        for (WebAPI api : INSTANCES) {
            api.reconnect();
        }
    }

    private CloseableHttpClient client;

    private WebAPI() {
        client = ProxyManager.getSirirusHttpClient();
    }

    @Override
    public void close() throws IOException {
        client.close();
        INSTANCES.remove(this);
    }

    public boolean isConnected() {
        if (client == null || checkConnection() == 0) {
            LoggerFactory.getLogger(this.getClass()).warn("No Connection, try to reconnect");
            reconnect();
            return checkConnection() == 0;
        }
        return true;
    }

    public void reconnect() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("Could not close Existing connection!", e);
            }
        }
        client = ProxyManager.getSirirusHttpClient();
    }


    private boolean checkFingerIDConnection() {
        return getRESTDb(BioFilter.ALL, null).testConnection();
    }

    //6 csi web api for this version is not reachable because it is outdated
    //5 csi web api for this version is not reachable
    //4 csi server not reachable
    //3 no connection to bioinf web site
    //2 no connection to uni jena
    //1 no connection to internet (google/microft/ubuntu????)
    //0 everything is fine
    public static final int MAX_STATE = 6;

    public int checkConnection() {
        VersionsInfo v = getVersionInfo();
        if (v == null) {
            int error = ProxyManager.checkInternetConnection(client);
            if (error > 0) return error;
            else return 4;
        } else if (v.outdated()) {
            return MAX_STATE;
        } else if (checkFingerIDConnection()) {
            return 0;
        } else {
            return 5;
        }
    }

    public static int checkFingerIDConnectionStatic() {
        int errorcode = 1;
        try (final WebAPI web = WebAPI.newInstance()) {
            errorcode = web.checkConnection();
        } catch (IOException e) {
            LoggerFactory.getLogger(WebAPI.class).error("Unexpected WebAPI error during connection check", e);
        }
        return errorcode;
    }

    public static boolean canConnect() {
        return checkFingerIDConnectionStatic() == ProxyManager.OK_STATE;
    }

    public VersionsInfo getVersionInfo() {
        VersionsInfo v = null;
        try {
            v = getVersionInfo(new HttpGet(getFingerIdVersionURI(getFingerIdBaseURI()).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build()));
            if (v == null) {
                LoggerFactory.getLogger(this.getClass()).warn("Could not reach fingerid root url for version verification. Try to reach version specific url");
                v = getVersionInfo(new HttpGet(getFingerIdVersionURI(getFingerIdURI(null)).build()));
            }
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }
        if (v != null)
            LoggerFactory.getLogger(this.getClass()).debug(v.toString());
        return v;
    }

    private VersionsInfo getVersionInfo(final HttpGet get) {
        final int timeoutInSeconds = 8000;
        get.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
        try (CloseableHttpResponse response = client.execute(get)) {
            try (final JsonReader r = Json.createReader(new InputStreamReader(response.getEntity().getContent()))) {
                JsonObject o = r.readObject();
                JsonObject gui = o.getJsonObject("SIRIUS GUI");

                final String version = gui.getString("version");
//                    final String date = gui.getString("date");
                String database = o.getJsonObject("database").getString("version");

                boolean expired = true;
                Timestamp accept = null;
                Timestamp finish = null;

                if (o.containsKey("expiry dates")) {
                    JsonObject expiryInfo = o.getJsonObject("expiry dates");
                    expired = expiryInfo.getBoolean("isExpired");
                    if (expiryInfo.getBoolean("isAvailable")) {
                        accept = Timestamp.valueOf(expiryInfo.getString("acceptJobs"));
                        finish = Timestamp.valueOf(expiryInfo.getString("finishJobs"));
                    }
                }

                List<News> newsList = Collections.emptyList();
                if (o.containsKey("news")) {
                    final String newsJson = o.getJsonArray("news").toString();
                    newsList = News.parseJsonNews(newsJson);
                }
                return new VersionsInfo(version, database, expired, accept, finish, newsList);
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }
        return null;
    }

    public static URIBuilder getFingerIdURI(String path) {
        if (path == null)
            path = "";
        URIBuilder b = null;
        try {
            b = getFingerIdBaseURI();
            if (ProxyManager.DEBUG) {
                b = b.setPath("/frontend" + path);
            } else {
                b = b.setPath("/csi-fingerid-" + FingerIDProperties.fingeridVersion() + path);
            }
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(WebAPI.class).error("Unacceptable URI for CSI:FingerID", e);
        }
        return b;
    }

    private static URIBuilder getFingerIdVersionURI(URIBuilder baseBuilder) {
        if (ProxyManager.DEBUG) {
            baseBuilder = baseBuilder.setPath("/frontend/webapi/version.json");
        } else {
            baseBuilder = baseBuilder.setPath("/webapi/version.json");
        }
        return baseBuilder;
    }

    private static URIBuilder getFingerIdBaseURI() throws URISyntaxException {
        URIBuilder b;
        if (ProxyManager.DEBUG) {
            b = new URIBuilder().setScheme(ProxyManager.HTTP_SCHEME).setHost("localhost");
            b = b.setPort(8080);
        } else {
            b = new URIBuilder(FINGERID_WEB_API);
        }
        return b;
    }

    public RESTDatabase getRESTDb(BioFilter bioFilter, File cacheDir) {
        URI host = null;
        try {
            host = getFingerIdURI(null).build();
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(this.getClass()).warn("Illegal fingerid URI -> Fallback to RestDB Default URI", e);
        }
        return new RESTDatabase(cacheDir, bioFilter, host, client);
    }


    public boolean updateJobStatus(FingerIdJob job) throws URISyntaxException, IOException {
        final HttpGet get = new HttpGet(getFingerIdURI("/webapi/job.json").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
        try (CloseableHttpResponse response = client.execute(get)) {
            try (final JsonReader json = Json.createReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent(), ContentType.getOrDefault(response.getEntity()).getCharset())))) {
                final JsonObject obj = json.readObject();
                if (obj.containsKey("prediction")) {
                    final byte[] plattBytes = Base64.decode(obj.getString("prediction"));
                    final double[] platts = parseBinaryToDoubles(plattBytes);
                    job.prediction = new ProbabilityFingerprint(job.version, platts);

                    if (obj.containsKey("iokrVector")) {
                        final byte[] iokrBytes = Base64.decode(obj.getString("iokrVector"));
                        job.iokrVerctor = parseBinaryToDoubles(iokrBytes);
                    }

                    return true;
                } else {
                    job.state = obj.containsKey("state") ? obj.getString("state") : "SUBMITTED";
                }
                if (obj.containsKey("errors")) {
                    job.errorMessage = obj.getString("errors");
                }
            }
        } catch (Throwable t) {
            LoggerFactory.getLogger(this.getClass()).error("Error when updating job #" + job.jobId, t);
            throw (t);
        }
        return false;
    }

    double[] parseBinaryToDoubles(byte[] bytes) {
        final TDoubleArrayList data = new TDoubleArrayList(2000);
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        while (buf.position() < buf.limit()) {
            data.add(buf.getDouble());
        }
        return data.toArray();
    }

    public FingerIdJob submitJob(final Ms2Experiment experiment, final FTree ftree, MaskedFingerprintVersion version, @NotNull EnumSet<PredictorType> types) throws IOException, URISyntaxException {
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

        if (types.isEmpty())
            types = EnumSet.of(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(experiment.getPrecursorIonType()));
        final NameValuePair predictor = new BasicNameValuePair("predictors", PredictorType.getBitsAsString(types));

        final UrlEncodedFormEntity params = new UrlEncodedFormEntity(Arrays.asList(ms, tree, predictor, UID));
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
                RuntimeException re = new RuntimeException(response.getStatusLine().getReasonPhrase());
                LoggerFactory.getLogger(this.getClass()).debug("Submitting Job failed", re);
                throw re;
            }
        }
    }

    public Future<ProbabilityFingerprint> predictFingerprint(@NotNull ExecutorService service, final Ms2Experiment experiment, final FTree tree, final MaskedFingerprintVersion version, final EnumSet<PredictorType> predicors) {
        return service.submit(new PredictionJJob(experiment, null, tree, version, predicors));
    }

    public PredictionJJob predictFingerprint(@NotNull JobManager manager, final Ms2Experiment experiment, final FTree tree, final MaskedFingerprintVersion version, final EnumSet<PredictorType> predicors) {
        return predictFingerprint(manager, experiment, tree, version, predicors);
    }

    public PredictionJJob predictFingerprint(@NotNull JobManager manager, final Ms2Experiment experiment, final IdentificationResult result, final MaskedFingerprintVersion version, final EnumSet<PredictorType> predicors) {
        return predictFingerprint(manager, experiment, result, result.getResolvedTree(), version, predicors);
    }

    public PredictionJJob predictFingerprint(@NotNull JobManager manager, final Ms2Experiment experiment, final IdentificationResult result, final FTree tree, final MaskedFingerprintVersion version, final EnumSet<PredictorType> predicors) {
        PredictionJJob jjob = makePredictionJob(experiment, result, tree, version, predicors);
        manager.submitJob(jjob);
        return jjob;
    }

    public PredictionJJob makePredictionJob(final Ms2Experiment experiment, final IdentificationResult result, final FTree tree, final MaskedFingerprintVersion version, final Collection<UserDefineablePredictorType> predicors) {
        return makePredictionJob(experiment, result, tree, version, UserDefineablePredictorType.toPredictorTypes(result.getPrecursorIonType(), predicors));
    }

    public PredictionJJob makePredictionJob(final Ms2Experiment experiment, final IdentificationResult result, final FTree tree, final MaskedFingerprintVersion version, final EnumSet<PredictorType> predicors) {
        return new PredictionJJob(experiment, result, tree, version, predicors);

    }

    public static FingerprintVersion getFingerprintVersion() {
        // TODO: implement as web request
        return CdkFingerprintVersion.withECFP();
    }

    public class PredictionJJob extends BasicJJob<ProbabilityFingerprint> {
        public final Ms2Experiment experiment;
        public final FTree ftree;
        public final IdentificationResult result;
        public final MaskedFingerprintVersion version;
        private final EnumSet<PredictorType> predicors;

        public PredictionJJob(final Ms2Experiment experiment, final IdentificationResult result, final FTree ftree, MaskedFingerprintVersion version, EnumSet<PredictorType> predicors) {
            super(JobType.WEBSERVICE);
            this.experiment = experiment;
            this.ftree = ftree;
            this.result = result;
            this.version = version;
            this.predicors = predicors;
        }

        @Override
        public ProbabilityFingerprint compute() throws Exception {
            final FingerIdJob job = submitJob(experiment, ftree, version, predicors);
            // RECEIVE RESULTS
            new HttpGet(getFingerIdURI("/webapi/job.json").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
            for (int k = 0; k < 600; ++k) {
                Thread.sleep(3000 + 30 * k);
                if (updateJobStatus(job)) {
                    return job.prediction;
                } else if (Objects.equals(job.state, "CRASHED")) {
                    throw new RuntimeException("Job crashed: " + (job.errorMessage != null ? job.errorMessage : ""));
                }
            }
            throw new TimeoutException("Reached timeout");
        }
    }


    /**
     * make statistics of fingerprints and write the used indizes of fingerprints into the
     * given TIntArrayList (as this property is not contained in FingerprintStatistics)
     *
     * @param fingerprintIndizes
     * @return
     * @throws IOException
     */
    public PredictionPerformance[] getStatistics(PredictorType predictorType, final TIntArrayList fingerprintIndizes) throws IOException {
        fingerprintIndizes.clear();
        final HttpGet get;
        try {
            get = new HttpGet(getFingerIdURI("/webapi/statistics.csv").setParameter("predictor", predictorType.toBitsAsString()).build());
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

    public CovarianceScoring getCovarianceScoring(FingerprintVersion fpVersion, double alpha) throws IOException {
        final HttpGet get;
        try {
            get = new HttpGet(getFingerIdURI("/webapi/covariancetree.csv").build());
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        CovarianceScoring covarianceScoring;
        try (CloseableHttpResponse response = client.execute(get)) {
            if (!isSuccessful(response)) throw new IOException("Cannot get covariance scoring tree information.");
            HttpEntity e = response.getEntity();
            covarianceScoring = CovarianceScoring.readScoring(e.getContent(), ContentType.getOrDefault(e).getCharset(), fpVersion, alpha);
        }
        return covarianceScoring;
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

    private boolean isSuccessful(HttpResponse response) {
        return response.getStatusLine().getStatusCode() < 400;
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
