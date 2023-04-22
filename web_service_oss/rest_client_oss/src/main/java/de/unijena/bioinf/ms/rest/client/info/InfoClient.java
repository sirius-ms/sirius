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
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.rest.client.AbstractCsiClient;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.info.News;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionConsumables;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class InfoClient extends AbstractCsiClient {
    private static final String WEBAPI_VERSION_JSON = "/version.json";
    private static final String WEBAPI_WORKER_JSON = "/workers.json";

    @SafeVarargs
    public InfoClient(@Nullable URI serverUrl, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators) {
        super(serverUrl, requestDecorators);
    }

    @NotNull
    public VersionsInfo getVersionInfo(final OkHttpClient client, boolean includeUpdateInfo) throws IOException {
        return execute(client,
                () -> new Request.Builder()
                        .url(buildVersionSpecificWebapiURI(WEBAPI_VERSION_JSON)
                                .addQueryParameter("fingeridVersion", FingerIDProperties.fingeridFullVersion())
                                .addQueryParameter("siriusguiVersion", FingerIDProperties.sirius_guiVersion())
                                .addQueryParameter("updateInfo", String.valueOf(includeUpdateInfo))
                                .build())
                        .get(),
                this::parseVersionInfo
        );
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
                VersionsInfo v = new VersionsInfo(version, database, expired, accept, finish, newsList);

                if (gui.containsKey("latestVersion") && gui.getString("latestVersion") != null)
                    v.setLatestSiriusVersion(new DefaultArtifactVersion(gui.getString("latestVersion")));

                if (gui.containsKey("latestVersionUrl"))
                    v.setLatestSiriusLink(gui.getString("latestVersionUrl"));

                return v;
            }
        }
        return null;
    }

    @Nullable
    public WorkerList getWorkerInfo(@NotNull OkHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> new Request.Builder().url(buildVersionSpecificWebapiURI(WEBAPI_WORKER_JSON).build()).get(),
                new TypeReference<>() {}
        );
    }

    public SubscriptionConsumables getConsumables(@NotNull Date monthAndYear, boolean byMonth, @NotNull OkHttpClient client) throws IOException {
        return getConsumables(monthAndYear, null, byMonth, client);
    }

    public SubscriptionConsumables getConsumables(@NotNull Date monthAndYear, @Nullable JobTable jobType, boolean byMonth, @NotNull OkHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> {
                    HttpUrl.Builder builder = buildVersionSpecificWebapiURI("/consumed-resources")
                            .addQueryParameter("date", Long.toString(monthAndYear.getTime()))
                            .addQueryParameter("byMonth", Boolean.toString(byMonth));
                    if (jobType != null)
                        builder.addQueryParameter("jobType", new ObjectMapper().writeValueAsString(jobType));

                    return new Request.Builder().url(builder.build()).get();
                },
                new TypeReference<>() {
                }
        );
    }
}
