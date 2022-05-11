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

package de.unijena.bioinf.webapi.rest;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProxyManager {
    private static final RequestConfig DEFAULT_CONFIG = RequestConfig.custom()
            .setConnectTimeout(15000)
//                .setConnectionRequestTimeout(30000)
            .setSocketTimeout(15000)
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .build();

    public enum ProxyStrategy {SIRIUS, NONE}

    private ProxyManager() {
    } //prevent instantiation

    public static ProxyStrategy getStrategyByName(String value) {
        try {
            if ("SYSTEM".equals(value)) //legacy
                value = "NONE";
            return ProxyStrategy.valueOf(value);
        } catch (IllegalArgumentException e) {
            LoggerFactory.getLogger(ProxyStrategy.class).debug("Invalid Proxy Strategy state!", e);
            return null;
        }
    }

    public static ProxyStrategy getProxyStrategy() {
        return getStrategyByName(PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy", null, ProxyStrategy.NONE.name()));
    }

    public static boolean useSiriusProxyConfig() {
        return getProxyStrategy() == ProxyStrategy.SIRIUS;
    }

    public static boolean useNoProxyConfig() {
        return getProxyStrategy() == ProxyStrategy.NONE;
    }

    // this method inits the proxy configuration at program start
    public static CloseableHttpClient getSirirusHttpClient() {
        return getSirirusHttpClient(getProxyStrategy());
    }

    public static CloseableHttpClient getSirirusHttpClient(ProxyStrategy strategy) {
        return (CloseableHttpClient) getSirirusHttpClient(strategy, false);
    }

    public static CloseableHttpAsyncClient getSirirusHttpAsyncClient() {
        return getSirirusHttpAsyncClient(getProxyStrategy());
    }

    public static CloseableHttpAsyncClient getSirirusHttpAsyncClient(ProxyStrategy strategy) {
        return (CloseableHttpAsyncClient) getSirirusHttpClient(strategy, true);
    }

    private static Object getSirirusHttpClient(ProxyStrategy strategy, boolean async) {
        final Object client;
        switch (strategy) {
            case SIRIUS:
                client = getSiriusProxyClient(async);
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using Proxy Type " + ProxyStrategy.SIRIUS);
                break;
            case NONE:
                client = getNoProxyClient(async);
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using Proxy Type " + ProxyStrategy.NONE);
                break;
            default:
                client = getNoProxyClient(async);
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using FALLBACK Proxy Type " + ProxyStrategy.NONE);
        }
        return client;
    }


    //0 everything is fine
    //1 no connection to Internet
    //2 no connection to Auth0 Server
    //3 no connection to BG License Server
    public static Optional<List<ConnectionError>> checkInternetConnection(final HttpClient client) {
        List<ConnectionError> failedChecks = new ArrayList<>();
        checkLicenseServer(client).ifPresent(failedChecks::add);
        checkAuthServer(client).ifPresent(failedChecks::add);
        checkExternal(client).ifPresent(failedChecks::add);
        return failedChecks.isEmpty() ? Optional.empty() : Optional.of(failedChecks);
    }

    public static Optional<List<ConnectionError>> checkInternetConnection() {
        try (CloseableHttpClient client = getSirirusHttpClient()) {
            return checkInternetConnection(client);
        } catch (IOException e) {
            String m = "Could not create Http client during Internet connection check.";
            LoggerFactory.getLogger(ProxyManager.class).error(m, e);
            return Optional.of(List.of(new ConnectionError(98, m)));
        }
    }

    private static <B> B handleSSLValidation(@NotNull final B builder) {
        if (isSSLValidationDisabled()) {
            try {
                SSLContext context = new SSLContextBuilder().loadTrustMaterial(null, (TrustStrategy) (arg0, arg1) -> true).build();
                if (builder instanceof HttpAsyncClientBuilder)
                    ((HttpAsyncClientBuilder) builder).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setSSLContext(context);
                else if (builder instanceof HttpClientBuilder)
                    ((HttpClientBuilder) builder).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setSSLContext(context);
                else
                    throw new IllegalArgumentException("Only HttpAsyncClientBuilder and  HttpClientBuilder are supported");
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                LoggerFactory.getLogger(ProxyManager.class).warn("Could not create Noop SSL context. SSL Validation will NOT be disabled!");
            }
        }
        return builder;
    }

    private static Object getNoProxyClient(boolean async) {
        return handleSSLValidation(async
                ? HttpAsyncClientBuilder.create().setDefaultRequestConfig(DEFAULT_CONFIG).build()
                : HttpClientBuilder.create().setDefaultRequestConfig(DEFAULT_CONFIG).build());
    }

    private static Object getSiriusProxyClient(boolean async) {
        final String hostName = PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.hostname");
        final int port = Integer.parseInt(PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.port"));
        final String scheme = PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.scheme");

        final Object builder;


        if (Boolean.getBoolean(PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials"))) {
            builder = getClientBuilderWithProxySettings(
                    hostName,
                    port,
                    scheme,
                    PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials.user"),
                    PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials.pw"),
                    async
            );
        } else {
            builder = getClientBuilderWithProxySettings(hostName, port, scheme, null, null, async);
        }

        handleSSLValidation(builder);

        return async ? ((HttpAsyncClientBuilder) builder).build() : ((HttpClientBuilder) builder).build();

    }

    private static Object getClientBuilderWithProxySettings(final String hostname, final int port, final String scheme, final String username, final String password, boolean async) {
        return decorateClientBuilderWithProxySettings(
                async
                        ? HttpAsyncClients.custom().setDefaultRequestConfig(DEFAULT_CONFIG)
                        : HttpClients.custom().setDefaultRequestConfig(DEFAULT_CONFIG),
                hostname, port, scheme, username, password);
    }

    private static <B> B decorateClientBuilderWithProxySettings(final B builder, final String hostname, final int port, final String scheme, final String username, final String password) {
        BasicCredentialsProvider clientCredentials = new BasicCredentialsProvider();

        HttpHost proxy = new HttpHost(
                hostname,
                port,
                scheme
        );

        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

        if (username != null && password != null)
            clientCredentials.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(username, password));

        if (builder instanceof HttpAsyncClientBuilder) {
            final HttpAsyncClientBuilder clientBuilder = (HttpAsyncClientBuilder) builder;
            clientBuilder.setDefaultCredentialsProvider(clientCredentials);
            clientBuilder.setRoutePlanner(routePlanner);
        } else if (builder instanceof HttpClientBuilder) {
            final HttpClientBuilder clientBuilder = (HttpClientBuilder) builder;
            clientBuilder.setDefaultCredentialsProvider(clientCredentials);
            clientBuilder.setRoutePlanner(routePlanner);
        } else {
            throw new IllegalArgumentException("Only HttpAsyncClientBuilder and  HttpClientBuilder are supported");
        }

        return builder;
    }

    public static void enforceGlobalProxySetting() {
        enforceGlobalProxySetting(getProxyStrategy());
    }

    public static void enforceGlobalProxySetting(ProxyStrategy strategy) {
        if (strategy == ProxyStrategy.SIRIUS) {
            final String hostName = PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.hostname");
            final String port = PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.port");
            System.setProperty("http.proxyHost", hostName);
            System.setProperty("http.proxyPort", port);
            System.setProperty("https.proxyHost", hostName);
            System.setProperty("https.proxyPort", port);

            if (Boolean.getBoolean(PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials"))) {
                String user = PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials.user");
                String pw = PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials.pw");
                System.setProperty("http.proxyUser", user);
                System.setProperty("http.proxyPassword", pw);
                System.setProperty("https.proxyUser", user);
                System.setProperty("https.proxyPassword", pw);
            } else {
                System.getProperties().remove("http.proxyUser");
                System.getProperties().remove("http.proxyPassword");
                System.getProperties().remove("https.proxyUser");
                System.getProperties().remove("https.proxyPassword");
            }
        } else {
            System.getProperties().remove("http.proxyHost");
            System.getProperties().remove("http.proxyPort");
            System.getProperties().remove("http.proxyUser");
            System.getProperties().remove("http.proxyPassword");

            System.getProperties().remove("https.proxyHost");
            System.getProperties().remove("https.proxyPort");
            System.getProperties().remove("https.proxyUser");
            System.getProperties().remove("https.proxyPassword");
        }
    }



    public static Optional<ConnectionError> checkExternal(HttpClient proxy) {
        String url = PropertyManager.getProperty("de.unijena.bioinf.sirius.web.external", null, "https://www.google.de/");
        return checkConnectionToUrl(proxy, url).map(e -> e.withNewMessage(1, "Could not connect to the Internet: " + url));
//                ? Optional.of(new ConnectionError(2, "Could not connect to the Internet: " + url)) : Optional.empty();
    }

    public static Optional<ConnectionError> checkAuthServer(HttpClient proxy) {
        String auth0HealthCheck = "https://status.auth0.com/feed?domain=dev-4yibfvd4.auth0.com";
//        String auth0HealthCheck = "status.auth0.com/feed?domain=auth0.brigh-giant.com";
        //        String url =  PropertyManager.getProperty("de.unijena.bioinf.sirius.security.authServer",null,"https://auth0.brigh-giant.com/");
        return checkConnectionToUrl(proxy, auth0HealthCheck).map(e -> e.withNewMessage(2, "Could not connect to the Authentication Server: " + auth0HealthCheck));
//                ? Optional.of(new ConnectionError()) : Optional.empty();
    }

    public static Optional<ConnectionError> checkLicenseServer(HttpClient proxy) {
        String url = PropertyManager.getProperty("de.unijena.bioinf.sirius.web.licenseServer", null, "https://gate.bright-giant.com/") + "v0.1/actuator/health";
        return checkConnectionToUrl(proxy, url).map(e -> e.withNewMessage(3, "Could not connect to the License Server: " + url));
    }

    public static Optional<ConnectionError> checkConnectionToUrl(final HttpClient proxy, String url) {
        try {
            HttpResponse response = proxy.execute(new HttpGet(url));
            int code = response.getStatusLine().getStatusCode();
            LoggerFactory.getLogger(ProxyManager.class).debug("Testing internet connection");
            LoggerFactory.getLogger(ProxyManager.class).debug("Try to connect to: " + url);

            LoggerFactory.getLogger(ProxyManager.class).debug("Response Code: " + code);

            LoggerFactory.getLogger(ProxyManager.class).debug("Response Message: " + response.getStatusLine().getReasonPhrase());
            LoggerFactory.getLogger(ProxyManager.class).debug("Protocol Version: " + response.getStatusLine().getProtocolVersion());
            if (code != HttpURLConnection.HTTP_OK) {
                LoggerFactory.getLogger(ProxyManager.class).warn("Error Response code: " + response.getStatusLine().getReasonPhrase() + " " + code);
                return Optional.of(new ConnectionError(96,"Error when connecting to: " + url + "Bad Response!", null, ConnectionError.Type.ERROR, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()));
            }
            return Optional.empty();
        } catch (Exception e) {
            LoggerFactory.getLogger(ProxyManager.class).warn("Connection error", e);
            return Optional.of(new ConnectionError(97,"Error when connecting to: " + url, e));
        }
    }


    public static boolean isSSLValidationDisabled() {
        return !PropertyManager.getBoolean("de.unijena.bioinf.sirius.security.sslValidation", true);
    }

    //region HTTPClientManagement
    public static void disconnect() {
        closingContainers.add(clientContainer);
        Iterator<GentlyHttpClientCloser> iterator = closingContainers.iterator();
        while (iterator.hasNext()) {
            try {
                LoggerFactory.getLogger(ProxyManager.class).info("Closing open http connection before shutdown");
                iterator.next().client.close();
            } catch (IOException e) {
                LoggerFactory.getLogger(PropertyManager.class).error("Error when closing HTTP connection", e);
            }
        }
    }

    public static void reconnect() {
        final GentlyHttpClientCloser old = clientContainer;
        synchronized (old.clientUsers) {
            clientContainer = new GentlyHttpClientCloser(getSirirusHttpClient());
            old.closeMe.set(true);
            closingContainers.add(old);
        }
    }

    static LockedClosableHttpClient client() {
        return new LockedClosableHttpClient(clientContainer);
    }

    public static void consumeClient(IOFunctions.IOConsumer<LockedClosableHttpClient> doWithClient) throws IOException {
        try (LockedClosableHttpClient client = ProxyManager.client()) {
            doWithClient.accept(client);
        }
    }

    public static <T> T doWithClient(Function<LockedClosableHttpClient, T> doWithClient) {
        try (LockedClosableHttpClient client = ProxyManager.client()) {
            return doWithClient.apply(client);
        }
    }

    public static <T> T applyClient(IOFunctions.IOFunction<LockedClosableHttpClient, T> doWithClient) throws IOException {
        try (LockedClosableHttpClient client = ProxyManager.client()) {
            return doWithClient.apply(client);
        }
    }


    private static final Set<GentlyHttpClientCloser> closingContainers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static GentlyHttpClientCloser clientContainer = new GentlyHttpClientCloser(getSirirusHttpClient());

    private static class GentlyHttpClientCloser {
        private final AtomicBoolean closeMe = new AtomicBoolean(false);
        private final AtomicInteger clientUsers = new AtomicInteger(0);
        private final CloseableHttpClient client;

        private GentlyHttpClientCloser(final CloseableHttpClient client) {
            this.client = client;
        }


        private void close() throws IOException {
            synchronized (clientUsers) {
                if (closeMe.get() && clientUsers.get() == 0) {
                    client.close();
                    closingContainers.remove(this);
                }
            }
        }
    }

    public static class LockedClosableHttpClient extends CloseableHttpClient implements AutoCloseable {
        private final GentlyHttpClientCloser clientContainer;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private LockedClosableHttpClient(final GentlyHttpClientCloser clientCon) {
            this.clientContainer = clientCon;
            synchronized (clientContainer.clientUsers) {
                if (clientContainer.closeMe.get()) {
                    throw new RuntimeException("Client has been already stated for closing");
                }
                clientContainer.clientUsers.incrementAndGet();
            }
        }

        @Override
        protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) {
            throw new RuntimeException("Method not Implemented because of delegation from wrapped http client");
        }

        @Override
        public CloseableHttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
            return clientContainer.client.execute(target, request, context);
        }

        @Override
        public CloseableHttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
            return clientContainer.client.execute(request, context);
        }

        @Override
        public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
            return clientContainer.client.execute(request);
        }

        @Override
        public CloseableHttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
            return clientContainer.client.execute(target, request);
        }

        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
            return clientContainer.client.execute(request, responseHandler);
        }

        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
            return clientContainer.client.execute(request, responseHandler, context);
        }

        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
            return clientContainer.client.execute(target, request, responseHandler);
        }

        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
            return clientContainer.client.execute(target, request, responseHandler, context);
        }

        @Override
        @Deprecated
        public HttpParams getParams() {
            return clientContainer.client.getParams();
        }

        @Override
        @Deprecated
        public ClientConnectionManager getConnectionManager() {
            return clientContainer.client.getConnectionManager();
        }

        @Override
        public void close() {
            if (!closed.getAndSet(true)) {
                synchronized (clientContainer.clientUsers) {
                    clientContainer.clientUsers.decrementAndGet();
                    try {
                        clientContainer.close();
                    } catch (IOException e) {
                        LoggerFactory.getLogger(PropertyManager.class).error("Error while HTTP container closing attempt!", e);
                    }
                }
            }
        }
    }
    //endregion
}
