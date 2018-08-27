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

import com.google.gson.Gson;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.RESTDatabase;
import de.unijena.bioinf.fingerid.blast.CovarianceScoring;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.fingeriddb.WorkerList;
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
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
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
import java.util.stream.Collectors;

/*
 * Frontend WebAPI class, corresponding to backend class
 * de.unijena.bioinf.fingerid.WebAPIServlet in csi_fingerid/frontend
 */

@ThreadSafe
public class WebAPI implements Closeable {
    private static final BasicNameValuePair UID = new BasicNameValuePair("uid", SystemInformation.generateSystemKey());
    private static final Logger LOG = LoggerFactory.getLogger(WebAPI.class);

    public static final String SIRIUS_DOWNLOAD = "https://bio.informatik.uni-jena.de/software/sirius/";
    public static final String FINGERID_WEB_API = FingerIDProperties.fingeridWebHost();


    public static final WebAPI INSTANCE = new WebAPI();

    private CloseableHttpClient client;


    private WebAPI() {
        client = ProxyManager.getSirirusHttpClient();
    }

    @Override
    public void close() throws IOException {
        LOG.info("Closing Web Connection");
        client.close();
    }

    public boolean isConnected() {
        if (client == null || checkConnection() == 0) {
            LOG.warn("No Connection, try to reconnect");
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
                LOG.error("Could not close Existing connection!", e);
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


    public static boolean canConnect() {
        return WebAPI.INSTANCE.checkConnection() == ProxyManager.OK_STATE;
    }

    @Nullable
    public VersionsInfo getVersionInfo() {
        VersionsInfo v = null;
        try {
            v = getVersionInfo(new HttpGet(getFingerIdVersionURI(getFingerIdBaseURI()).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build()));
            if (v == null) {
                LOG.warn("Could not reach fingerid root url for version verification. Try to reach version specific url");
                v = getVersionInfo(new HttpGet(getFingerIdVersionURI(getFingerIdURI(null)).build()));
            }
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
        if (v != null)
            LOG.debug(v.toString());
        return v;
    }

    @Nullable
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
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    @Nullable
    public WorkerList getWorkerInfo() {
        WorkerList wl = null;
        try {
            HttpGet get = new HttpGet(getWorInfoURI(getFingerIdBaseURI()).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build());
            final int timeoutInSeconds = 8000;
            get.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
            try (CloseableHttpResponse response = client.execute(get)) {
                wl = new Gson().fromJson(new InputStreamReader(response.getEntity().getContent()), WorkerList.class);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
        }

        return wl;
    }

    static URIBuilder getFingerIdURI(String path) {
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


    private static URIBuilder getWorInfoURI(URIBuilder baseBuilder) {
        if (ProxyManager.DEBUG) {
            baseBuilder = baseBuilder.setPath("/frontend/webapi/workers.json");
        } else {
            baseBuilder = baseBuilder.setPath("/webapi/workers.json");
        }
        return baseBuilder;
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
            LOG.warn("Illegal fingerid URI -> Fallback to RestDB Default URI", e);
        }
        return new RESTDatabase(cacheDir, bioFilter, host, client);
    }

    public boolean deleteJobOnServer(FingerIdJob job) throws URISyntaxException {
        final HttpGet get = new HttpGet(getFingerIdURI("/webapi/delete-job").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
        int reponsecode = Integer.MIN_VALUE;
        String responseReason = null;
        try (CloseableHttpResponse response = client.execute(get)) {
            reponsecode = response.getStatusLine().getStatusCode();
            responseReason = response.getStatusLine().getReasonPhrase();
            if (reponsecode == 200) {
                return true;
            }
            LOG.error("Could not delete Job! Response Code: " + reponsecode + "Reason: " + response.getStatusLine().getReasonPhrase());
        } catch (Throwable t) {
            LOG.error("Error when doing job deletion request " + job.jobId + " Response error code: " + reponsecode + " - Reason: " + responseReason, t);
        }
        return false;
    }

    public boolean updateJobStatus(FingerIdJob job) throws URISyntaxException {
        final HttpGet get = new HttpGet(getFingerIdURI("/webapi/job.json").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
        int reponsecode = Integer.MIN_VALUE;
        String responseReason = null;
        try (CloseableHttpResponse response = client.execute(get)) {
            reponsecode = response.getStatusLine().getStatusCode();
            responseReason = response.getStatusLine().getReasonPhrase();
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
            LOG.error("Error when updating job #" + job.jobId + " Response error code: " + reponsecode + " - Reason: " + responseReason, t);
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
        int status = Integer.MIN_VALUE;
        String reason = null;
        try (CloseableHttpResponse response = client.execute(post)) {
            status = response.getStatusLine().getStatusCode();
            reason = response.getStatusLine().getReasonPhrase();
            if (response.getStatusLine().getStatusCode() == 200) {
                try (final JsonReader json = Json.createReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent(), ContentType.getOrDefault(response.getEntity()).getCharset())))) {
                    final JsonObject obj = json.readObject();
                    securityToken = obj.getString("securityToken");
                    jobId = obj.getInt("jobId");
                    return new FingerIdJob(jobId, securityToken, version);
                }
            }
            throw new HttpResponseException(status, "Response Status Code: " + status + " - Expected: 200");
        } catch (Throwable t) {
            RuntimeException re = new RuntimeException("Error during job submission - Code: " + status + " Reason: " + reason, t);
            LOG.debug("Submitting Job failed", re);
            throw re;
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
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
            LOG.error(e.getMessage(), e);
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
                    LOG.info(m);
                } else {
                    LOG.error(m);
                }
                return m;
            } else {
                RuntimeException e = new RuntimeException(response.getStatusLine().getReasonPhrase());
                LOG.error("Could not send error report! Bad http return Value: " + response.getStatusLine().getStatusCode(), e);
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
