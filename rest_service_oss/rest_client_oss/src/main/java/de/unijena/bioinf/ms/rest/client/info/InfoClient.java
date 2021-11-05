/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.client.info;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import de.unijena.bioinf.ms.rest.model.info.News;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
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
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

public class InfoClient extends AbstractClient {
    private static final Logger LOG = LoggerFactory.getLogger(InfoClient.class);
    private static final String WEBAPI_VERSION_JSON = "/version.json";
    private static final String WEBAPI_WORKER_JSON = "/workers.json";
    private static final String WEBAPI_TERMS_JSON = "/terms.json";

    public InfoClient(@NotNull URI serverUrl) {
        this(serverUrl, (it) -> {});
    }

    public InfoClient(@Nullable URI serverUrl, @NotNull IOFunctions.IOConsumer<HttpUriRequest> requestDecorator) {
        super(serverUrl, requestDecorator);
    }

    @Nullable
    public VersionsInfo getVersionInfo(final CloseableHttpClient client) {
        VersionsInfo v = null;
        try {
            v = execute(client,
                    () -> {
                        HttpGet get = new HttpGet(buildVersionSpecificWebapiURI(WEBAPI_VERSION_JSON).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build());
                        get.setConfig(RequestConfig.custom().setConnectTimeout(8000).setSocketTimeout(8000).build());
                        return get;
                    },
                    this::parseVersionInfo
            );

        } catch (IOException e) {
            LOG.warn("Could not reach fingerid version specific URL for version verification. Try to reach root url. Cause:" + e.getMessage());

            try {
                v = execute(client,
                        () -> {
                            HttpGet get = new HttpGet(buildVersionLessWebapiURI(WEBAPI_VERSION_JSON).setParameter("fingeridVersion", FingerIDProperties.fingeridVersion()).setParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion()).build());
                            get.setConfig(RequestConfig.custom().setConnectTimeout(8000).setSocketTimeout(8000).build());
                            return get;
                        },
                        this::parseVersionInfo
                );
            } catch (IOException ex) {
                LOG.warn("Could not reach fingerid root url for version verification.  Cause: " + e.getMessage());
            }
        }

        return v;
    }

    //todo change to Jackson
    @Nullable
    private VersionsInfo parseVersionInfo(BufferedReader reader) {
        if (reader != null) {
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
        return null;
    }

    @Nullable
    public List<Term> getTerms(@NotNull CloseableHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> {
                    HttpGet get = new HttpGet(buildVersionSpecificWebapiURI(WEBAPI_TERMS_JSON).build());
                    final int timeoutInSeconds = 8000;
                    get.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
                    return get;
                }, new TypeReference<>() {}
        );
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


    public <T extends ErrorReport> String reportError(ErrorReport report, String SOFTWARE_NAME, @NotNull CloseableHttpClient client) throws IOException {
        return execute(client,
                () -> {
                    final HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/report.json")
                            .addParameter("name", SOFTWARE_NAME).build());
                    final String json = new ObjectMapper().writeValueAsString(report);
                    post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
                    post.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
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
