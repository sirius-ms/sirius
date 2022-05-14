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
import de.unijena.bioinf.ChemistryBase.utils.NetUtils;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.client.utils.HTTPSupplier;
import de.unijena.bioinf.ms.rest.model.SecurityService;
import de.unijena.bioinf.ms.rest.model.info.LicenseInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractClient {
    public static final boolean DEBUG_CONNECTION = PropertyManager.getBoolean("de.unijena.bioinf.webapi.DEBUG_CONNECTION", false);
    protected static final String API_ROOT = "/api";
    protected static final String CID = SecurityService.generateSecurityToken();

    static {
        if (NetUtils.DEBUG)
            PropertyManager.setProperty("de.unijena.bioinf.fingerid.web.host", "http://localhost:8080");
    }

    @NotNull
    private Supplier<URI> serverUrl;
    @NotNull
    protected final List<IOFunctions.IOConsumer<HttpUriRequest>> requestDecorators;

    @SafeVarargs
    protected AbstractClient(@Nullable URI serverUrl, @NotNull IOFunctions.IOConsumer<HttpUriRequest>... requestDecorators) {
        this(serverUrl, List.of(requestDecorators));
    }

    protected AbstractClient(@Nullable URI serverUrl, @NotNull List<IOFunctions.IOConsumer<HttpUriRequest>> requestDecorators) {
        this.serverUrl = () -> serverUrl;
        this.requestDecorators = requestDecorators;
    }

    public void setServerUrl(@NotNull Supplier<URI> serverUrl) {
        if (NetUtils.DEBUG){
            this.serverUrl = () -> URI.create("http://localhost:8080");
        }else {
            this.serverUrl = serverUrl;
        }
    }

    public URI getServerUrl() {
        return serverUrl.get();
    }

    public boolean deleteAccount(@NotNull CloseableHttpClient client) {
        try {
            execute(client, () -> {
                HttpDelete delete = new HttpDelete(getBaseURI("/delete-account").build());
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

    public boolean acceptTerms(@NotNull CloseableHttpClient client) {
        try {
            execute(client, () -> {
                HttpPost post = new HttpPost(getBaseURI("/accept-terms").build());
                final int timeoutInSeconds = 8000;
                post.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
                return post;
            });
            return true;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("Error when accepting terms: " + e.getMessage());
            return false;
        }
    }

    protected void isSuccessful(HttpResponse response, HttpRequest sourceRequest) throws IOException {
        final StatusLine status = response.getStatusLine();

        if (status.getStatusCode() >= 400) {
            final String content = response.getEntity() != null ? IOUtils.toString(getIn(response, sourceRequest)) : "No Content";
            throw new HttpErrorResponseException(status.getStatusCode(), status.getReasonPhrase(),
                    Optional.ofNullable(response.getFirstHeader("WWW-Authenticate")).map(Header::getValue).orElse("NULL"), content);
        }
    }


    //region http request execution API
    public <T> T executeWithResponse(@NotNull CloseableHttpClient client, @NotNull final HTTPSupplier<?> makeRequest, IOFunctions.IOFunction<HttpResponse, T> respHandling) throws IOException {
        try {
            return executeWithResponse(client, makeRequest.get(), respHandling);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T executeWithResponse(@NotNull CloseableHttpClient client, @NotNull final HttpUriRequest request, IOFunctions.IOFunction<HttpResponse, T> respHandling) throws IOException {
        try (CloseableHttpResponse response = client.execute(request)) {
            return respHandling.apply(response);
        }
    }

    public <T> T execute(@NotNull CloseableHttpClient client, @NotNull final HttpUriRequest request, IOFunctions.IOFunction<BufferedReader, T> respHandling) throws IOException {
        for (IOFunctions.IOConsumer<HttpUriRequest> requestDecorator : requestDecorators)
            requestDecorator.accept(request);
        return executeWithResponse(client, request, response -> {
            isSuccessful(response, request);
            if (response.getEntity() != null) {
                try (final BufferedReader reader = new BufferedReader(getIn(response, request))) {
                    return respHandling.apply(reader);
                }
            }
            if (DEBUG_CONNECTION) {
                LoggerFactory.getLogger(getClass()).warn("Entity return value was NULL: ");
                getIn(response, request).close();
            }
            return null;
        });
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
        return execute(client, request, r -> r != null && r.ready() ? new ObjectMapper().readValue(r, tr) : null);
    }

    public <T, R extends TypeReference<T>> T executeFromJson(@NotNull CloseableHttpClient client, @NotNull final HTTPSupplier<?> makeRequest, R tr) throws IOException {
        return execute(client, makeRequest, r -> r != null && r.ready() ? new ObjectMapper().readValue(r, tr) : null);
    }

    public <T> T executeFromStream(@NotNull CloseableHttpClient client, @NotNull final HTTPSupplier<?> makeRequest, IOFunctions.IOFunction<InputStream, T> respHandling) throws IOException {
        try {
            return executeFromStream(client, makeRequest.get(), respHandling);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T executeFromStream(@NotNull CloseableHttpClient client, @NotNull final HttpUriRequest request, IOFunctions.IOFunction<InputStream, T> respHandling) throws IOException {
        for (IOFunctions.IOConsumer<HttpUriRequest> requestDecorator : requestDecorators)
            requestDecorator.accept(request);

        try (CloseableHttpResponse response = client.execute(request)) {
            isSuccessful(response, request);
            if (response.getEntity() != null) {
                try (final InputStream stream = response.getEntity().getContent()) {
                    return respHandling.apply(stream);
                }
            }
            return null;
        }
    }

    public <T> T executeFromByteBuffer(@NotNull CloseableHttpClient client, @NotNull final HTTPSupplier<?> makeRequest, IOFunctions.IOFunction<ByteBuffer, T> respHandling, int bufferSize) throws IOException {
        try {
            return executeFromByteBuffer(client, makeRequest.get(), respHandling, bufferSize);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T executeFromByteBuffer(@NotNull CloseableHttpClient client, @NotNull final HttpUriRequest request, IOFunctions.IOFunction<ByteBuffer, T> respHandling, int bufferSize) throws IOException {
        return executeFromStream(client, request, inputStream -> {
            final ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
            byteBuffer.put(inputStream.readAllBytes());
            byteBuffer.flip();
            return respHandling.apply(byteBuffer);
        });
    }

    @NotNull
    protected Reader getIn(HttpResponse response, HttpRequest sourceRequest) throws IOException {
        final HttpEntity entity = response.getEntity();
        Charset charset = ContentType.getOrDefault(entity).getCharset();
        charset = charset == null ? StandardCharsets.UTF_8 : charset;
        if (!DEBUG_CONNECTION) {
            return new InputStreamReader(entity.getContent(), charset);
        } else {
            final String content = entity.getContent() == null ? null : IOUtils.toString(new InputStreamReader(entity.getContent(), charset));
            System.out.println("##### Request DEBUG information #####S");
            System.out.println("----- Source Request");
            System.out.println("Request URL: '" + sourceRequest.getRequestLine().getUri() + "'");
            for (Header header : sourceRequest.getAllHeaders())
                System.out.println("Request Header: '" + header.getName() + "':'" + header.getValue() + "'");

            System.out.println("----- Response");
            System.out.println("Content encoding: '" + charset + "'");
            System.out.println("Used Content encoding: '" + entity.getContentEncoding() + "'");
            System.out.println("Content Type: '" + entity.getContentType() + "'");
            System.out.println("Response Return Code: '" + response.getStatusLine().getStatusCode() + "'");
            System.out.println("Response Reason Phrase: '" + response.getStatusLine().getReasonPhrase() + "'");
            System.out.println("Response Protocol Version: '" + response.getStatusLine().getProtocolVersion().toString() + "'");
            System.out.println("Response Locale: '" + response.getLocale().toString() + "'");
            for (Header header : response.getAllHeaders())
                System.out.println("Request Header: '" + header.getName() + "':'" + header.getValue() + "'");
            System.out.println("----- Content");
            if (content == null || content.isBlank())
                System.out.println("<NO CONTENT>");
            else
                System.out.println(content);

            System.out.println("#####################################");

            return content == null ? new StringReader("") : new StringReader(content);
        }


    }
    //endregion


    //#################################################################################################################
    //region PathBuilderMethods
    public URIBuilder getBaseURI(@Nullable String path) throws URISyntaxException {
        if (path == null)
            path = "";

        URIBuilder b;
        if (NetUtils.DEBUG) {
            b = new URIBuilder().setScheme("http").setHost("localhost");
            b = b.setPort(8080);
//            path = FINGERID_DEBUG_FRONTEND_PATH + path;
        } else {
            b = new URIBuilder(getServerUrl());
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

    protected URIBuilder buildWebapiURI(@Nullable final String path) throws URISyntaxException {
        StringBuilder pathBuilder = getWebAPIBasePath();

        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/"))
                pathBuilder.append("/");

            pathBuilder.append(path);
        }

        return getBaseURI(pathBuilder.toString());
    }


    protected URIBuilder buildVersionSpecificWebapiURI(@Nullable String path) throws URISyntaxException {
        return buildWebapiURI(path);
    }

    //endregion
    //#################################################################################################################

    protected static String makeVersionContext() {
        return "/v" + FingerIDProperties.fingeridMinorVersion();
    }
}
