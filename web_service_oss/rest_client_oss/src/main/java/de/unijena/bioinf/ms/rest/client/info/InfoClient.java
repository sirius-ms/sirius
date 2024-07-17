/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.rest.client.AbstractCsiClient;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionConsumables;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Date;

public class InfoClient extends AbstractCsiClient {
    private static final String WEBAPI_VERSION_JSON = "/version.json";
    private static final String WEBAPI_WORKER_JSON = "/workers.json";

    @SafeVarargs
    public InfoClient(@Nullable URI serverUrl, @Nullable String contextPath, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators) {
        super(serverUrl, contextPath, requestDecorators);
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

    @Nullable
    private VersionsInfo parseVersionInfo(BufferedReader reader) {
        if (reader != null) {
            ObjectMapper mapper = new ObjectMapper();
            try (final JsonParser parser = mapper.createParser(reader)){
                final JsonNode o = mapper.readTree(parser);

                JsonNode gui = o.get("SIRIUS GUI");

                final String version = gui.get("version").asText();
                String database = o.get("database").get("version").asText();

                boolean expired = true;
                Timestamp accept = null;
                Timestamp finish = null;

                if (o.has("expiry dates")) {
                    JsonNode expiryInfo = o.get("expiry dates");
                    expired = expiryInfo.get("isExpired").asBoolean();
                    if (expiryInfo.get("isAvailable").asBoolean()) {
                        accept = Timestamp.valueOf(expiryInfo.get("acceptJobs").asText());
                        finish = Timestamp.valueOf(expiryInfo.get("finishJobs").asText());
                    }
                }

                VersionsInfo v = new VersionsInfo(version, database, expired, accept, finish);

                if (gui.has("latestVersion") && gui.get("latestVersion") != null)
                    v.setLatestSiriusVersion(new DefaultArtifactVersion(gui.get("latestVersion").asText()));

                if (gui.has("latestVersionUrl"))
                    v.setLatestSiriusLink(gui.get("latestVersionUrl").asText());

                return v;
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).warn("Error when deserializing version information.", e);
            }
        }
        return null;
    }


    public SubscriptionConsumables getConsumables(@NotNull Date monthAndYear, boolean byMonth, @NotNull OkHttpClient client) throws IOException {
        return getConsumables(monthAndYear, null, byMonth, client);
    }

    public SubscriptionConsumables getConsumables(@NotNull Date monthAndYear, @Nullable JobTable jobType, boolean byMonth, @NotNull OkHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> {
                    HttpUrl.Builder builder = buildVersionSpecificWebapiURI("/consumed-resources")
                            .addQueryParameter("date", Long.toString(monthAndYear.getTime()))
                            .addQueryParameter("byMonth", Boolean.toString(byMonth))
                            .addQueryParameter("includePendingJobs", Boolean.toString(true));
                    if (jobType != null)
                        builder.addQueryParameter("jobType", new ObjectMapper().writeValueAsString(jobType));

                    return new Request.Builder().url(builder.build()).get();
                },
                new TypeReference<>() {
                }
        );
    }
}
