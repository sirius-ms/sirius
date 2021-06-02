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

package de.unijena.bioinf.ms.rest.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.client.utils.HTTPSupplier;
import de.unijena.bioinf.ms.rest.model.SecurityService;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class AbstractClient {
    public final static boolean DEBUG = PropertyManager.getBoolean("de.unijena.bioinf.ms.rest.DEBUG",false);
    protected static final String API_ROOT = "/api";
    protected static final String CID = SecurityService.generateSecurityToken();

    static {
        if (DEBUG)
            PropertyManager.setProperty("de.unijena.bioinf.fingerid.web.host", "http://localhost:8080");
    }

    @NotNull
    protected URI serverUrl;
    @NotNull
    protected final IOFunctions.IOConsumer<HttpUriRequest> requestDecorator;

    protected AbstractClient(@Nullable URI serverUrl, @NotNull IOFunctions.IOConsumer<HttpUriRequest> requestDecorator) {
        this.serverUrl = Objects.requireNonNullElseGet(serverUrl, () -> URI.create(FingerIDProperties.fingeridWebHost()));
        this.requestDecorator = requestDecorator;
    }

    public void setServerUrl(@NotNull URI serverUrl) {
        this.serverUrl = serverUrl;
    }


    public boolean testConnection() {
        try {
            URIBuilder builder = getBaseURI("/actuator/health", true);
            HttpURLConnection urlConn = (HttpURLConnection) builder.build().toURL().openConnection();
            urlConn.connect();

            return HttpURLConnection.HTTP_OK == urlConn.getResponseCode();
        } catch (IOException | URISyntaxException e) {
            return false;
        }
    }

    public boolean testSecuredConnection(@NotNull CloseableHttpClient client) {
        try {
            execute(client, () -> {
                HttpGet get = new HttpGet(getBaseURI("/check", true).build());
                final int timeoutInSeconds = 8000;
                get.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
                return get;
            });
            return true;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("Could not reach secured api endpoint: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteAccount(@NotNull CloseableHttpClient client){
        try {
            execute(client, () -> {
                HttpDelete delete = new HttpDelete(getBaseURI("/delete-account", true).build());
                final int timeoutInSeconds = 8000;
                delete.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
                return delete;
            });
            return true;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("Error when deleting user account: " + e.getMessage());
            return false;
        }
    }

    protected void isSuccessful(HttpResponse response) throws IOException {
        final StatusLine status = response.getStatusLine();
        if (status.getStatusCode() >= 400){
            final String content = response.getEntity()!= null ? IOUtils.toString(getIn(response.getEntity())) : "No Content";
            throw new IOException("Error when querying REST service. Bad Response Code: "
                    + status.getStatusCode() + " | Message: " + status.getReasonPhrase() + " | " + response.getFirstHeader("WWW-Authenticate") + " | Content: " + content);
        }
    }


    //region http request execution API
    public <T> T execute(@NotNull CloseableHttpClient client, @NotNull final HttpUriRequest request, IOFunctions.IOFunction<BufferedReader, T> respHandling) throws IOException {
        requestDecorator.accept(request);
        try (CloseableHttpResponse response = client.execute(request)) {
            isSuccessful(response);
            if (response.getEntity() != null) {
                try (final BufferedReader reader = new BufferedReader(getIn(response.getEntity()))) {
                    return respHandling.apply(reader);
                }
            }
            return null;
        }
    }

    public <T> T execute(@NotNull CloseableHttpClient client, @NotNull final HTTPSupplier<?> makeRequest, IOFunctions.IOFunction<BufferedReader, T> respHandling) throws IOException {
        try {
            return execute(client, makeRequest.get(), respHandling);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(@NotNull CloseableHttpClient client, @NotNull final HTTPSupplier<?> makeRequest) throws IOException {
        execute(client, makeRequest, (br) -> true);
    }

    public void execute(@NotNull CloseableHttpClient client, @NotNull final HttpUriRequest request) throws IOException {
        execute(client, () -> request);
    }

    public <T, R extends TypeReference<T>> T executeFromJson(@NotNull CloseableHttpClient client, @NotNull final HttpUriRequest request, R tr) throws IOException {
        return execute(client, request, r -> new ObjectMapper().readValue(r, tr));
    }

    public <T, R extends TypeReference<T>> T executeFromJson(@NotNull CloseableHttpClient client, @NotNull final HTTPSupplier<?> makeRequest,  R tr) throws IOException {
        return execute(client, makeRequest, r -> new ObjectMapper().readValue(r, tr));
    }

    @NotNull
    protected InputStreamReader getIn(HttpEntity entity) throws IOException {
        final Charset charset = ContentType.getOrDefault(entity).getCharset();
        return new InputStreamReader(entity.getContent(), charset == null ? StandardCharsets.UTF_8 : charset);
    }
    //endregion


    //#################################################################################################################
    //region PathBuilderMethods
    public URIBuilder getBaseURI(@Nullable String path, final boolean versionSpecificPath) throws URISyntaxException {
        if (path == null)
            path = "";

        URIBuilder b;
        if (DEBUG) {
            b = new URIBuilder().setScheme("http").setHost("localhost");
            b = b.setPort(8080);
//            path = FINGERID_DEBUG_FRONTEND_PATH + path;
        } else {
            b = new URIBuilder(serverUrl);
            if (versionSpecificPath)
                path = makeVersionContext() + path;
        }

        if (!path.isEmpty())
            b = b.setPath(path);

        return b;
    }

    // WebAPI paths
    protected StringBuilder getWebAPIBasePath() {
        return new StringBuilder(API_ROOT);
    }

    protected URIBuilder buildWebapiURI(@Nullable final String path, final boolean versionSpecific) throws URISyntaxException {
        StringBuilder pathBuilder = versionSpecific || DEBUG ? getWebAPIBasePath() : new StringBuilder("/webapi"); //todo workaround until nu api version is deployed to root

        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/"))
                pathBuilder.append("/");

            pathBuilder.append(path);
        }

        return getBaseURI(pathBuilder.toString(), versionSpecific);
    }

    protected URIBuilder buildVersionLessWebapiURI(@Nullable String path) throws URISyntaxException {
        return buildWebapiURI(path, false);
    }


    protected URIBuilder buildVersionSpecificWebapiURI(@Nullable String path) throws URISyntaxException {
        return buildWebapiURI(path, true);
    }

    //endregion
    //#################################################################################################################

    protected static String makeVersionContext() {
        final String[] versionParts = FingerIDProperties.fingeridVersion().split("[.]");
        if (versionParts.length > 1)
            return "/v" + versionParts[0] + "." + versionParts[1];
        throw new IllegalArgumentException("Illegal Version String");
    }

}
