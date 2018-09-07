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
import de.unijena.bioinf.ChemistryBase.chem.InChI;
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
import de.unijena.bioinf.fingerworker.WorkerList;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.net.ProxyManager;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import de.unijena.bioinf.utils.systemInfo.SystemInformation;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import net.iharder.Base64;
import net.sf.jniinchi.INCHI_KEY;
import net.sf.jniinchi.JniInchiException;
import net.sf.jniinchi.JniInchiOutputKey;
import net.sf.jniinchi.JniInchiWrapper;
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

/*
 * Frontend WebAPI class, corresponding to backend class
 * de.unijena.bioinf.fingerid.WebAPIServlet in csi_fingerid/frontend
 */

@ThreadSafe
public class WebAPI {
    private static final BasicNameValuePair UID = new BasicNameValuePair("uid", SystemInformation.generateSystemKey());
    private static final Logger LOG = LoggerFactory.getLogger(WebAPI.class);

    // Singelton instance for the sirius_frontend
    public static final WebAPI INSTANCE = new WebAPI();


    private WebAPI() {/*prevent instantiation*/}


    //6 csi web api for this version is not reachable because it is outdated
    //5 csi web api for this version is not reachable
    //4 csi server not reachable
    //3 no connection to bioinf web site
    //2 no connection to uni jena
    //1 no connection to internet (google/microft/ubuntu????)
    //0 everything is fine
    public static final int MAX_STATE = 6;

    public int checkConnection() {
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
            VersionsInfo v = getVersionInfo(client);
            if (v == null) {
                int error = ProxyManager.checkInternetConnection(client);
                if (error > 0) return error;
                else return 4;
            } else if (v.outdated()) {
                return MAX_STATE;
            } else if (getRESTDb(BioFilter.ALL, null, client).testConnection()) {
                return 0;
            } else {
                return 5;
            }
        } catch (Exception e) {
            LOG.error("Error during connection check", e);
            return MAX_STATE;
        }
    }

    @Nullable
    public VersionsInfo getVersionInfo() {
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
            return getVersionInfo(client);
        }
    }

    @Nullable
    private VersionsInfo getVersionInfo(final ProxyManager.LockedClosableHttpClient client) {
        VersionsInfo v = null;

        try {
            v = getVersionInfo(new HttpGet(buildVersionLessFingerIdWebapiURI(WEBAPI_VERSION_JSON).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build())
                    , client
            );
            if (v == null) {
                LOG.warn("Could not reach fingerid root url for version verification. Try to reach version specific url");
                v = getVersionInfo(new HttpGet(buildVersionSpecificFingerIdWebapiURI(WEBAPI_VERSION_JSON).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build())
                        , client
                );
            }
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
        if (v != null)
            LOG.debug(v.toString());
        return v;
    }

    @Nullable
    private VersionsInfo getVersionInfo(final HttpGet get, final ProxyManager.LockedClosableHttpClient client) {
        final int timeoutInSeconds = 8000;
        get.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
//        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
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
        } catch (Exception e) {
            LOG.error("Unknown error when fetching VERSION information from webservice!", e);
        }
//        }
        return null;
    }

    @Nullable
    public WorkerList getWorkerInfo() {
        try {
            HttpGet get = new HttpGet(buildVersionSpecificFingerIdWebapiURI(WEBAPI_WORKER_JSON).build());
            final int timeoutInSeconds = 8000;
            get.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
            try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
                try (CloseableHttpResponse response = client.execute(get)) {
                    return new Gson().fromJson(new InputStreamReader(response.getEntity().getContent()), WorkerList.class);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Unknown error when fetching WORKER information from webservice!", e);
        }
        return null;
    }

    private RESTDatabase getRESTDb(BioFilter bioFilter, File cacheDir, final ProxyManager.LockedClosableHttpClient client) {
        URI host = null;
        try {
            host = getFingerIdBaseURI(null, true).build();
        } catch (URISyntaxException e) {
            LOG.warn("Illegal fingerid URI -> Fallback to RestDB Default URI", e);
        }
        return new RESTDatabase(cacheDir, bioFilter, host, client);
    }

    public RESTDatabase getRESTDb(BioFilter bioFilter, File cacheDir) {
        return getRESTDb(bioFilter, cacheDir, ProxyManager.client());
    }

    public boolean deleteJobOnServer(FingerIdJob job) throws URISyntaxException {
        final HttpGet get = new HttpGet(buildVersionSpecificFingerIdWebapiURI("/delete-job").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
        int reponsecode = Integer.MIN_VALUE;
        String responseReason = null;
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
            try (CloseableHttpResponse response = client.execute(get)) {
                reponsecode = response.getStatusLine().getStatusCode();
                responseReason = response.getStatusLine().getReasonPhrase();
                if (reponsecode == 200) {
                    return true;
                }
                LOG.error("Could not delete Job! Response Code: " + reponsecode + " Reason: " + response.getStatusLine().getReasonPhrase());
            } catch (Throwable t) {
                LOG.error("Error when doing job deletion request " + job.jobId + " Response error code: " + reponsecode + " - Reason: " + responseReason, t);
            }
        }
        return false;
    }

    public boolean updateJobStatus(FingerIdJob job) throws URISyntaxException {
        final HttpGet get = new HttpGet(buildVersionSpecificFingerIdWebapiURI("/job.json").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
        int reponsecode = Integer.MIN_VALUE;
        String responseReason = null;
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
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
        }
        return false;
    }

    private double[] parseBinaryToDoubles(byte[] bytes) {
        final TDoubleArrayList data = new TDoubleArrayList(2000);
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        while (buf.position() < buf.limit()) {
            data.add(buf.getDouble());
        }
        return data.toArray();
    }

    public FingerIdJob submitJob(final Ms2Experiment experiment, final FTree ftree, MaskedFingerprintVersion version, @NotNull EnumSet<PredictorType> types) throws IOException, URISyntaxException {
        final HttpPost post = new HttpPost(buildVersionSpecificFingerIdWebapiURI("/predict.json").build());
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
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
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
            get = new HttpGet(buildVersionSpecificFingerIdWebapiURI("/statistics.csv").setParameter("predictor", predictorType.toBitsAsString()).build());
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        ArrayList<PredictionPerformance> performances = new ArrayList<>();
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
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
        }
        return performances.toArray(new PredictionPerformance[performances.size()]);
    }

    public CovarianceScoring getCovarianceScoring(FingerprintVersion fpVersion, double alpha) throws IOException {
        final HttpGet get;
        try {
            get = new HttpGet(buildVersionSpecificFingerIdWebapiURI("/covariancetree.csv").build());
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        CovarianceScoring covarianceScoring;
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
            try (CloseableHttpResponse response = client.execute(get)) {
                if (!isSuccessful(response)) throw new IOException("Cannot get covariance scoring tree information.");
                HttpEntity e = response.getEntity();
                covarianceScoring = CovarianceScoring.readScoring(e.getContent(), ContentType.getOrDefault(e).getCharset(), fpVersion, alpha);
            }
        }
        return covarianceScoring;
    }


    public InChI[] getTrainingStructures(PredictorType predictorType) throws IOException {
        final HttpGet get;
        try {
            get = new HttpGet(buildVersionSpecificFingerIdWebapiURI("/trainingstructures.csv").setParameter("predictor", predictorType.toBitsAsString()).build());
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        ArrayList<InChI> inchis = new ArrayList<>();
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
            CloseableHttpResponse response = client.execute(get);
            HttpEntity e = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 404) {
                //todo remove hack if not necessary anymore
                response.close();
                response = getResponseHack(client, predictorType);
                e = response.getEntity();
                if (response.getStatusLine().getStatusCode() == 404) {
                    throw new RuntimeException("Error retrieving training structures: " + response.getStatusLine().getReasonPhrase());
                }
//            throw new RuntimeException("Error retrieving training structures: "+response.getStatusLine().getReasonPhrase());
            }

            final BufferedReader br = new BufferedReader(new InputStreamReader(e.getContent(), ContentType.getOrDefault(e).getCharset()));
            String line;
            while ((line = br.readLine()) != null) {
                String[] tabs = line.split("\t");
                InChI inChI;
                if (tabs.length == 1) {
                    //no InChiKeys contained. Compute them.
                    String inchi = tabs[0];
                    String key = inchi2inchiKey(inchi);
                    inChI = new InChI(key, inchi);
                } else {
                    inChI = new InChI(tabs[0], tabs[1]);
                }
                inchis.add(inChI);
            }
            response.close();
        }
        return inchis.toArray(new InChI[0]);
    }


    ////////////////////////////////////////
    //todo Helper methods, remove if not necessary anymore
    private static CloseableHttpResponse getResponseHack(CloseableHttpClient client, PredictorType predictorType) throws IOException {
        final HttpGet get;
        try {
            get = new HttpGet(buildVersionSpecificFingerIdWebapiURI("/trainingstructures.txt").setParameter("predictor", predictorType.toBitsAsString()).build());
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return client.execute(get);
    }

    private static String inchi2inchiKey(String inchi) {
        try {
            if (inchi == null) throw new NullPointerException("Given InChI is null");
            if (inchi.isEmpty()) throw new IllegalArgumentException("Empty string given as InChI");
            JniInchiOutputKey key = JniInchiWrapper.getInchiKey(inchi);
            if (key.getReturnStatus() == INCHI_KEY.OK) {
                return key.getKey();
            } else {
                throw new RuntimeException("Error while creating InChIKey: " + key.getReturnStatus());
            }
        } catch (JniInchiException e) {
            throw new RuntimeException(e);
        }
    }

    ////////////////////////////////////////


    public <T extends ErrorReport> String reportError(T report, String SOFTWARE_NAME) throws IOException, URISyntaxException {
        final HttpPost request = new HttpPost(buildVersionSpecificFingerIdWebapiURI("/report.json").build());
        final String json = ErrorReport.toJson(report);

        final NameValuePair reportValue = new BasicNameValuePair("report", json);
        final NameValuePair softwareName = new BasicNameValuePair("name", SOFTWARE_NAME);
        final UrlEncodedFormEntity params = new UrlEncodedFormEntity(Arrays.asList(reportValue, softwareName));
        request.setEntity(params);

        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
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
    }

    private boolean isSuccessful(HttpResponse response) {
        return response.getStatusLine().getStatusCode() < 400;
    }

    //#################################################################################################################
    //region StaticPathBuilderMethods

    //Path constants
    private static final String FINGERID_URL = FingerIDProperties.fingeridWebHost();
    private static final String FINGERID_DEBUG_FRONTEND_PATH = "/frontend";
    private static final String FINGERID_WEBAPI_PATH = "/webapi";

    private static final String WEBAPI_VERSION_JSON = "/version.json";
    private static final String WEBAPI_WORKER_JSON = "/workers.json";

    //path builder methods

    private static URIBuilder getFingerIdBaseURI(@Nullable String path, final boolean versionSpecificPath) throws URISyntaxException {
        if (path == null)
            path = "";

        URIBuilder b;
        if (ProxyManager.DEBUG) {
            b = new URIBuilder().setScheme(ProxyManager.HTTP_SCHEME).setHost("localhost");
            b = b.setPort(8080);
            path = FINGERID_DEBUG_FRONTEND_PATH + path;
        } else {
            b = new URIBuilder(FINGERID_URL);
            if (versionSpecificPath)
                path = "/csi-fingerid-" + FingerIDProperties.fingeridVersion() + path;
        }

        if (!path.isEmpty())
            b = b.setPath(path);

        return b;
    }

    // WebAPI paths
    private static StringBuilder getWebAPIBasePath() {
        return new StringBuilder(FINGERID_WEBAPI_PATH);
    }

    private static URIBuilder buildFingerIdWebapiURI(@Nullable final String path, final boolean versionSpecific) throws URISyntaxException {
        StringBuilder pathBuilder = getWebAPIBasePath();

        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/"))
                pathBuilder.append("/");

            pathBuilder.append(path);
        }

        return getFingerIdBaseURI(pathBuilder.toString(), versionSpecific);
    }

    private static URIBuilder buildVersionLessFingerIdWebapiURI(@Nullable String path) throws URISyntaxException {
        return buildFingerIdWebapiURI(path, false);
    }


    private static URIBuilder buildVersionSpecificFingerIdWebapiURI(@Nullable String path) throws URISyntaxException {
        return buildFingerIdWebapiURI(path, true);
    }
    //endregion
    //#################################################################################################################


}
