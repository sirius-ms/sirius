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
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.client.utils.HTTPSupplier;
import de.unijena.bioinf.ms.rest.model.SecurityService;
import de.unijena.bioinf.rest.HttpErrorResponseException;
import de.unijena.bioinf.rest.NetUtils;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractClient {
    public static final boolean DEBUG_CONNECTION = PropertyManager.getBoolean("de.unijena.bioinf.webapi.DEBUG_CONNECTION", false);
    protected static final String CID = SecurityService.generateSecurityToken();

    public static final MediaType APPLICATION_JSON =  MediaType.parse(com.google.common.net.MediaType.JSON_UTF_8.type());

    static {
        if (NetUtils.DEBUG)
            PropertyManager.setProperty("de.unijena.bioinf.fingerid.web.host", "http://localhost:8080");
    }

    @NotNull
    private Supplier<URI> serverUrl;
    @NotNull
    protected final List<IOFunctions.IOConsumer<Request.Builder>> requestDecorators;

    @SafeVarargs
    protected AbstractClient(@Nullable URI serverUrl, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators) {
        this(serverUrl, List.of(requestDecorators));
    }

    protected AbstractClient(@NotNull Supplier<URI> serverUrl, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators) {
        this(serverUrl, List.of(requestDecorators));
    }


    protected AbstractClient(@Nullable URI serverUrl, @NotNull List<IOFunctions.IOConsumer<Request.Builder>> requestDecorators) {
        this(() -> serverUrl, requestDecorators);
    }

    protected AbstractClient(@NotNull Supplier<URI> serverUrl, @NotNull List<IOFunctions.IOConsumer<Request.Builder>> requestDecorators) {
        this.serverUrl = serverUrl;
        this.requestDecorators = requestDecorators;
    }

    public void setServerUrl(@NotNull Supplier<URI> serverUrl) {
        if (NetUtils.DEBUG) {
            this.serverUrl = () -> URI.create("http://localhost:8080");
        } else {
            this.serverUrl = serverUrl;
        }
    }

    public URI getServerUrl() {
        return serverUrl.get();
    }

    protected void isSuccessful(Response response, Request sourceRequest) throws IOException {
        if (response.code() >= 400) {
            final String content = response.body() != null ? IOUtils.toString(getIn(response, sourceRequest)) : "No Content";
            throw new HttpErrorResponseException(response.code(), response.message(),
                    Optional.ofNullable(response.header("WWW-Authenticate")).orElse("NULL"), content);
        }
    }


    //region http request execution API
    public <T> T executeWithResponse(@NotNull OkHttpClient client, @NotNull final HTTPSupplier makeRequest, IOFunctions.IOFunction<Response, T> respHandling) throws IOException {
        try {
            return executeWithResponse(client, makeRequest.get(), respHandling);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T executeWithResponse(@NotNull OkHttpClient client, @NotNull final Request.Builder request, IOFunctions.IOFunction<Response, T> respHandling) throws IOException {
            return respHandling.apply(client.newCall(request.build()).execute());
    }

    public <T> T execute(@NotNull OkHttpClient client, @NotNull final Request.Builder request, IOFunctions.IOFunction<BufferedReader, T> respHandling) throws IOException {
        for (IOFunctions.IOConsumer<Request.Builder> requestDecorator : requestDecorators)
            requestDecorator.accept(request);
        return executeWithResponse(client, request, response -> {
            isSuccessful(response, response.request());
            if (response.body() != null) {
                try (final BufferedReader reader = new BufferedReader(getIn(response, response.request()))) {
                    return respHandling.apply(reader);
                }
            }
            if (DEBUG_CONNECTION) {
                LoggerFactory.getLogger(getClass()).warn("Entity return value was NULL: ");
                getIn(response, response.request()).close();
            }
            return null;
        });
    }

    public <T> T execute(@NotNull OkHttpClient client, @NotNull final HTTPSupplier makeRequest, IOFunctions.IOFunction<BufferedReader, T> respHandling) throws IOException {
        try {
            return execute(client, makeRequest.get(), respHandling);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(@NotNull OkHttpClient client, @NotNull final HTTPSupplier makeRequest) throws IOException {
        execute(client, makeRequest, (br) -> true);
    }

    public void execute(@NotNull OkHttpClient client, @NotNull final Request.Builder request) throws IOException {
        execute(client, () -> request);
    }

    public <T, R extends TypeReference<T>> T executeFromJson(@NotNull OkHttpClient client, @NotNull final Request.Builder request, R tr) throws IOException {
        return execute(client, request, r -> r != null ? new ObjectMapper().readValue(r, tr) : null);
    }

    public <T, R extends TypeReference<T>> T executeFromJson(@NotNull OkHttpClient client, @NotNull final HTTPSupplier makeRequest, R tr) throws IOException {
        return execute(client, makeRequest, r -> r != null ? new ObjectMapper().readValue(r, tr) : null);
    }

    public <T> T executeFromStream(@NotNull OkHttpClient client, @NotNull final HTTPSupplier makeRequest, IOFunctions.IOFunction<InputStream, T> respHandling) throws IOException {
        try {
            return executeFromStream(client, makeRequest.get(), respHandling);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T executeFromStream(@NotNull OkHttpClient client, @NotNull final Request.Builder request, IOFunctions.IOFunction<InputStream, T> respHandling) throws IOException {
        for (IOFunctions.IOConsumer<Request.Builder> requestDecorator : requestDecorators)
            requestDecorator.accept(request);

        Response response = client.newCall(request.build()).execute();
        isSuccessful(response, response.request());
        if (response.body() != null) {
            try (final InputStream stream = response.body().byteStream()) {
                return respHandling.apply(stream);
            }
        }
        return null;
    }

    public <T> T executeFromByteBuffer(@NotNull OkHttpClient client, @NotNull final HTTPSupplier makeRequest, IOFunctions.IOFunction<ByteBuffer, T> respHandling, int bufferSize) throws IOException {
        try {
            return executeFromByteBuffer(client, makeRequest.get(), respHandling, bufferSize);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T executeFromByteBuffer(@NotNull OkHttpClient client, @NotNull final Request.Builder request, IOFunctions.IOFunction<ByteBuffer, T> respHandling, int bufferSize) throws IOException {
        return executeFromStream(client, request, inputStream -> {
            final ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
            byteBuffer.put(inputStream.readAllBytes());
            byteBuffer.flip();
            return respHandling.apply(byteBuffer);
        });
    }

    @NotNull
    protected Reader getIn(Response response, Request sourceRequest) throws IOException {
        ResponseBody entity = response.body();
//        String charset = entity.getContentEncoding();
//        charset = charset == null ? StandardCharsets.UTF_8.name() : charset;
        if (!DEBUG_CONNECTION) {
            if (entity == null)
                throw new NullPointerException("Cannot extract content from NULL entity");
            return entity.charStream();
        } else {
            System.out.println("##### Request DEBUG information #####S");
            System.out.println("----- Source Request");
            System.out.println("Request URL: '" + sourceRequest.url() + "'");
            sourceRequest.headers().forEach(header -> System.out.println("Request Header: '" + header.getFirst() + "':'" + header.getSecond() + "'"));

            System.out.println("----- Response");
//            System.out.println("Content encoding: '" + charset + "'");
//            System.out.println("Used Content encoding: '" + (entity == null ? "<ENTITY NULL>" : entity.getContentEncoding()) + "'");
            System.out.println("Content Type: '" + (entity == null ? "<ENTITY NULL>" : entity.contentType()) + "'");
            System.out.println("Response Return Code: '" + response.code() + "'");
            System.out.println("Response Reason Phrase: '" + response.message() + "'");
//            System.out.println("Response Protocol Version: '" + response.getStatusLine().getProtocolVersion().toString() + "'");
//            System.out.println("Response Locale: '" + response.getLocale().toString() + "'");
            response.headers().forEach(header -> System.out.println("Request Header: '" + header.getFirst() + "':'" + header.getSecond() + "'"));
            System.out.println("----- Content");

            final String content = (entity == null || entity.contentLength() <= 0) ? null : IOUtils.toString(entity.charStream());
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
    public HttpUrl.Builder getBaseURI(@Nullable String path) {

        if (path == null)
            path = "";

        HttpUrl.Builder b;
        if (NetUtils.DEBUG) {
            b = new HttpUrl.Builder().scheme("http").host("localhost").port(8080);
        } else {
            URI serverUrl = getServerUrl();
            if (serverUrl == null)
                throw new NullPointerException("Server URL is null!");
            b = new HttpUrl.Builder()
                    .scheme(serverUrl.getScheme())
                    .host(serverUrl.getHost())
                    .port(serverUrl.getPort())
                    .addEncodedPathSegment(serverUrl.getRawPath());
            path = makeVersionContext() + path;
        }

        if (!path.isEmpty())
            b = b.addPathSegments(path);

        return b;
    }

    protected abstract String makeVersionContext();

    //endregion
    //#################################################################################################################
}
