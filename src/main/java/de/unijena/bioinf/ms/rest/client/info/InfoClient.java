package de.unijena.bioinf.ms.rest.client.info;

import com.google.gson.Gson;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import de.unijena.bioinf.ms.rest.model.info.News;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InfoClient extends AbstractClient {
    private static final Logger LOG = LoggerFactory.getLogger(InfoClient.class);
    private static final String WEBAPI_VERSION_JSON = "/version.json";
    private static final String WEBAPI_WORKER_JSON = "/workers.json";

    public InfoClient(@NotNull URI serverUrl) {
        super(serverUrl);
    }

    //todo check if the version suff
    @Nullable
    public VersionsInfo getVersionInfo(final CloseableHttpClient client) {
        VersionsInfo v = null;

        try {
            v = getVersionInfo(new HttpGet(buildVersionLessWebapiURI(WEBAPI_VERSION_JSON).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build())
                    , client
            );
            if (v == null) {
                LOG.warn("Could not reach fingerid root url for version verification. Try to reach version specific url");
                v = getVersionInfo(new HttpGet(buildVersionSpecificWebapiURI(WEBAPI_VERSION_JSON).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build())
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
    private VersionsInfo getVersionInfo(final HttpGet get, final CloseableHttpClient client) {
        final int timeoutInSeconds = 8000;
        get.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
        try (CloseableHttpResponse response = client.execute(get)) {
            try (final JsonReader r = Json.createReader(new InputStreamReader(response.getEntity().getContent()))) {
                JsonObject o = r.readObject();
                JsonObject gui = o.getJsonObject("SIRIUS GUI");

                final String version = gui.getString("version");
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
        return null;
    }

    @Nullable
    public WorkerList getWorkerInfo(@NotNull CloseableHttpClient client) {
        try {
            HttpGet get = new HttpGet(buildVersionSpecificWebapiURI(WEBAPI_WORKER_JSON).build());
            final int timeoutInSeconds = 8000;
            get.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
            try (CloseableHttpResponse response = client.execute(get)) {
                return new Gson().fromJson(new InputStreamReader(response.getEntity().getContent()), WorkerList.class);
            } catch (IOException e) {
                LOG.error("Error when executing get request for WORKER information", e);
            }
        } catch (URISyntaxException e) {
            LOG.error("Wrong request Syntax!", e);
        } catch (Exception e) {
            LOG.error("Unknown error when fetching WORKER information from webservice!", e);
        }
        return null;
    }

    public <T extends ErrorReport> String reportError(T report, String SOFTWARE_NAME, @NotNull CloseableHttpClient client) throws IOException, URISyntaxException {
        final HttpPost request = new HttpPost(buildVersionSpecificWebapiURI("/report.json").build());
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
}
