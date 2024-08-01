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
import de.unijena.bioinf.ms.rest.client.utils.HTTPSupplier;
import de.unijena.bioinf.ms.rest.model.SecurityService;
import de.unijena.bioinf.rest.HttpErrorResponseException;
import de.unijena.bioinf.rest.NetUtils;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractClient {
    protected static final String CID = SecurityService.generateSecurityToken();

    public static final MediaType APPLICATION_JSON = MediaType.parse("application/json;charset=UTF-8");

    @NotNull
    private Supplier<URI> serverUrl;

    /**
     * Allows to add a context path to a client that is added to the serverUrl before adding
     * additional path segments af the respective request.
     */
    @NotNull
    private Supplier<String> contextPath;
    @NotNull
    protected final List<IOFunctions.IOConsumer<Request.Builder>> requestDecorators;

    @SafeVarargs
    protected AbstractClient(@Nullable URI serverUrl, @Nullable String contextPath, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators) {
        this(serverUrl, contextPath, List.of(requestDecorators));
    }

    protected AbstractClient(@NotNull Supplier<URI> serverUrl, @NotNull Supplier<String> contextPath, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators) {
        this(serverUrl, contextPath, List.of(requestDecorators));
    }


    protected AbstractClient(@Nullable URI serverUrl, @Nullable String contextPath, @NotNull List<IOFunctions.IOConsumer<Request.Builder>> requestDecorators) {
        this(() -> serverUrl, () -> contextPath, requestDecorators);
    }

    protected AbstractClient(@NotNull Supplier<URI> serverUrl, @NotNull Supplier<String> contextPath, @NotNull List<IOFunctions.IOConsumer<Request.Builder>> requestDecorators) {
        this.serverUrl = serverUrl;
        this.contextPath = contextPath;
        this.requestDecorators = requestDecorators;
    }

    public void setServerUrl(@NotNull Supplier<URI> serverUrl) {
        if (NetUtils.DEBUG) {
            this.serverUrl = () -> URI.create(FingerIDProperties.siriusFallbackWebHost());
        } else {
            this.serverUrl = serverUrl;
        }
    }

    /**
     * @return Unmodifiable list of requestDecorators
     */
    public List<IOFunctions.IOConsumer<Request.Builder>> getRequestDecorators() {
        return Collections.unmodifiableList(requestDecorators);
    }

    public URI getServerUrl() {
        return serverUrl.get();
    }

    protected void isSuccessful(Response response, Request sourceRequest) throws IOException {
        if (response.code() >= 400) {
            throw new HttpErrorResponseException(response.code(), response.message(),
                    Optional.ofNullable(response.header("WWW-Authenticate")).orElse("NULL"),
                    response.request().method() + ": " + response.request().url(), "No Content");
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
        try (Response resp = client.newCall(request.build()).execute()) {
            return respHandling.apply(resp);
        }
    }

    public <T> T execute(@NotNull OkHttpClient client, @NotNull final Request.Builder request, IOFunctions.IOFunction<BufferedReader, T> respHandling) throws IOException {
        for (IOFunctions.IOConsumer<Request.Builder> requestDecorator : requestDecorators)
            requestDecorator.accept(request);
        return executeWithResponse(client, request, response -> {
            isSuccessful(response, response.request());
            try (ResponseBody body = response.body()) {
                if (body != null) {
                    try (final BufferedReader reader = new BufferedReader(body.charStream())) {
                        return respHandling.apply(reader);
                    }
                }
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

        try (Response response = client.newCall(request.build()).execute()) {
            isSuccessful(response, response.request());
            try (ResponseBody body = response.body()) {
                if (body != null) {
                    return respHandling.apply(body.byteStream());
                }
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

    //endregion


    //#################################################################################################################
    //region PathBuilderMethods
    public HttpUrl.Builder getBaseURI(@Nullable String path) {

        HttpUrl serverUrl = HttpUrl.get(getServerUrl());
        if (serverUrl == null)
            throw new NullPointerException("Server URL is null!");

        HttpUrl.Builder b = serverUrl.newBuilder();

        {
            String v = contextPath.get();
            if (v != null) {
                v = StringUtils.strip(v, "/");
                if (!v.isBlank())
                    b.addPathSegments(v);
            }
        }

        if (path != null && !path.isBlank())
            b.addPathSegments(StringUtils.strip(path, "/"));

        return b;
    }

    //endregion
    //#################################################################################################################
}
