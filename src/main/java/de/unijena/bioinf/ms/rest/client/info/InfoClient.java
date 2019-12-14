package de.unijena.bioinf.ms.rest.client.info;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import de.unijena.bioinf.ms.rest.model.info.News;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
import java.net.URI;
import java.net.URISyntaxException;
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
            v = execute(client,
                    () -> {
                        HttpGet get = new HttpGet(buildVersionLessWebapiURI(WEBAPI_VERSION_JSON).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build());
                        get.setConfig(RequestConfig.custom().setConnectTimeout(8000).setSocketTimeout(8000).build());
                        return get;
                    },
                    this::parseVersionInfo
            );
        } catch (IOException e) {
            LOG.warn("Could not reach fingerid root url for version verification. Try to reach version specific url. Cause: " + e.getMessage());
            try {
                v = execute(client,
                        () -> {
                            HttpGet get = new HttpGet(buildVersionSpecificWebapiURI(WEBAPI_VERSION_JSON).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build());
                            get.setConfig(RequestConfig.custom().setConnectTimeout(8000).setSocketTimeout(8000).build());
                            return get;
                        },
                        this::parseVersionInfo
                );
            } catch (IOException ex) {
                LOG.warn("Could not reach fingerid verssion specific URL for version verification. Cause:" + e.getMessage());
            }
        }

        return v;
    }

    //todo change to Jackson
    private VersionsInfo parseVersionInfo(BufferedReader reader) {
        try (final JsonReader r = Json.createReader(reader)) {
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
    }

    @Nullable
    public WorkerList getWorkerInfo(@NotNull CloseableHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> {
                    HttpGet get = new HttpGet(buildVersionSpecificWebapiURI(WEBAPI_WORKER_JSON).build());
                    final int timeoutInSeconds = 8000;
                    get.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
                    return get;
                }, new TypeReference<>() {}
        );
    }

    public <T extends ErrorReport> String reportError(T report, String SOFTWARE_NAME, @NotNull CloseableHttpClient client) throws IOException, URISyntaxException {
        return execute(client,
                () -> {
                    final HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/report.json").build());
                    final String json = ErrorReport.toJson(report);

                    final NameValuePair reportValue = new BasicNameValuePair("report", json);
                    final NameValuePair softwareName = new BasicNameValuePair("name", SOFTWARE_NAME);
                    final UrlEncodedFormEntity params = new UrlEncodedFormEntity(Arrays.asList(reportValue, softwareName));
                    post.setEntity(params);
                    return post;
                },
                reader -> {
                    JsonNode jTree = new ObjectMapper().readTree(reader);
                    boolean suc = jTree.get("success").asBoolean();
                    String m = jTree.get("message").asText();
                    if (suc)
                        LOG.info(m);
                    else
                        LOG.error(m);
                    return m;
                }
        );
    }
}
