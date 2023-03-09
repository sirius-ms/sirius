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

package de.unijena.bioinf.rest;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.ExFunctions;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.apache.commons.math3.util.Pair;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProxyManager {
    private static RequestConfig.Builder DEFAULT_CONFIG() {
        return RequestConfig.custom()
                //socket timeout is set on connection level
                .setConnectTimeout(Timeout.of(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.connectTimeout", 15000), TimeUnit.MILLISECONDS))
                .setResponseTimeout(Timeout.of(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.responseTimeout", 15000), TimeUnit.MILLISECONDS))
                .setConnectionRequestTimeout(Timeout.of(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.connectRequestTimeout", 15000), TimeUnit.MILLISECONDS))
                .setDefaultKeepAlive(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.keepAlive", 180000), TimeUnit.MILLISECONDS)
                .setCookieSpec(StandardCookieSpec.IGNORE);
    }

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
    public static CloseableHttpClient getSirirusHttpClient(boolean pooled) {
        return getSirirusHttpClient(getProxyStrategy(), pooled);
    }

    public static CloseableHttpClient getSirirusHttpClient(ProxyStrategy strategy, boolean pooled) {
        HttpClientBuilder b = getSirirusHttpSyncClientBuilder(strategy);
        b.setConnectionManager(pooled ? connectionPoolManager() : connectionPoolManager(1, 1));
        b.setConnectionReuseStrategy((request, response, context) -> response.getCode() < 400);
//        b.setConnectionManagerShared();
//        b.disableAutomaticRetries();
//        b.disableConnectionState()
        return b.build();
    }

    public static HttpClientBuilder getSirirusHttpSyncClientBuilder(ProxyStrategy strategy) {
        return (HttpClientBuilder) getSirirusHttpClientBuilder(strategy, false);
    }

    public static CloseableHttpAsyncClient getSirirusHttpAsyncClient() {
        return getSirirusHttpAsyncClient(getProxyStrategy());
    }

    public static CloseableHttpAsyncClient getSirirusHttpAsyncClient(ProxyStrategy strategy) {
        return getSirirusHttpAsyncClientBuilder(strategy).build();
    }

    public static HttpAsyncClientBuilder getSirirusHttpAsyncClientBuilder(ProxyStrategy strategy) {
        return (HttpAsyncClientBuilder) getSirirusHttpClientBuilder(strategy, true);
    }

    private static Object getSirirusHttpClientBuilder(ProxyStrategy strategy, boolean async) {
        final Object clientBuilder;
        switch (strategy) {
            case SIRIUS:
                clientBuilder = getSiriusProxyClientBuilder(async);
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using Proxy Type " + ProxyStrategy.SIRIUS);
                break;
            case NONE:
                clientBuilder = getNoProxyClientBuilder(async);
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using Proxy Type " + ProxyStrategy.NONE);
                break;
            default:
                clientBuilder = getNoProxyClientBuilder(async);
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using FALLBACK Proxy Type " + ProxyStrategy.NONE);
        }
        return clientBuilder;
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
        try (CloseableHttpClient client = getSirirusHttpClient(false)) {
            return checkInternetConnection(client);
        } catch (IOException e) {
            String m = "Could not create Http client during Internet connection check.";
            LoggerFactory.getLogger(ProxyManager.class).error(m, e);
            return Optional.of(List.of(new ConnectionError(98, m, ConnectionError.Klass.UNKNOWN)));
        }
    }

    private static PoolingHttpClientConnectionManager connectionPoolManager() {
        // minus 2 because we have to dedicated connection for job submission and watching
        return connectionPoolManager(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.maxTotal", 5) - 2);
    }

    private static PoolingHttpClientConnectionManager connectionPoolManager(int maxTotal) {
        int maxPerRoute = Math.min(Math.max(1, SiriusJobs.getCPUThreads()), PropertyManager.getInteger("de.unijena.bioinf.sirius.http.maxRoute", 2));
        return connectionPoolManager(maxPerRoute, maxTotal);
    }

    private static PoolingHttpClientConnectionManager connectionPoolManager(int maxPerRoute, int maxTotal) {
//        System.out.println("Starting http Client with MaxPerRout=" + maxPerRoute + " / maxTotal=" + maxTotal + "(Threads=" + SiriusJobs.getCPUThreads() + ").");
        LoggerFactory.getLogger(ProxyManager.class).info("Starting http Client with MaxPerRout=" + maxPerRoute + " / maxTotal=" + maxTotal + "(Threads=" + SiriusJobs.getCPUThreads() + ").");
        PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create();

        builder.setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.socketTimeout", 15000), TimeUnit.MILLISECONDS)
                .build());

        builder.setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX)
                .setConnPoolPolicy(PoolReusePolicy.FIFO) //todo maybe lifo recovers better
                .setConnectionTimeToLive(TimeValue.ofMinutes(3L));

        PoolingHttpClientConnectionManager poolingConnManager = builder.build();
        poolingConnManager.setDefaultMaxPerRoute(maxPerRoute);
        poolingConnManager.setMaxTotal(maxTotal);
        return poolingConnManager;
    }


    private static <B> B handleSSLValidation(@NotNull final B builder) {
        if (isSSLValidationDisabled()) {
            try {
                SSLContext context = new SSLContextBuilder().loadTrustMaterial(null, (arg0, arg1) -> true).build();
//todo do we need this?
                //                if (builder instanceof HttpAsyncClientBuilder)
//                    ((HttpAsyncClientBuilder) builder).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setSSLContext(context);
//                else if (builder instanceof HttpClientBuilder)
//                    ((HttpClientBuilder) builder).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setSSLContext(context);
//                else
//                    throw new IllegalArgumentException("Only HttpAsyncClientBuilder and  HttpClientBuilder are supported but found: " + builder.getClass().getName());
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                LoggerFactory.getLogger(ProxyManager.class).warn("Could not create Noop SSL context. SSL Validation will NOT be disabled!");
            }
        }
        return builder;
    }

    private static Object getNoProxyClientBuilder(boolean async) {
        Object builder = handleSSLValidation(async
                ? HttpAsyncClientBuilder.create().setDefaultRequestConfig(DEFAULT_CONFIG().build())
                : HttpClientBuilder.create().setDefaultRequestConfig(DEFAULT_CONFIG().build())
        );
        return async
                ? ((HttpAsyncClientBuilder) builder).setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1)
                : ((HttpClientBuilder) builder);
    }

    private static Object getSiriusProxyClientBuilder(boolean async) {
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

        return async
                ? ((HttpAsyncClientBuilder) builder)
                .setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1)
                : ((HttpClientBuilder) builder);
    }

    private static Object getClientBuilderWithProxySettings(final String hostname, final int port, final String scheme, final String username, final String password, boolean async) {
        return decorateClientBuilderWithProxySettings(async
                        ? HttpAsyncClientBuilder.create().setDefaultRequestConfig(DEFAULT_CONFIG().build())
                        : HttpClientBuilder.create().setDefaultRequestConfig(DEFAULT_CONFIG().build()),
                hostname, port, scheme, username, password);
    }

    private static <B> B decorateClientBuilderWithProxySettings(final B builder, final String hostname, final int port, final String scheme, final String username, final String password) {
        BasicCredentialsProvider clientCredentials = new BasicCredentialsProvider();

        HttpHost proxy = new HttpHost(
                scheme,
                hostname,
                port
        );

        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

        if (username != null && password != null)
            clientCredentials.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(username, password.toCharArray()));

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
        return checkConnectionToUrl(proxy, url)
                .map(e -> e.withNewMessage(1, "Could not connect to the Internet: " + url,
                        ConnectionError.Klass.INTERNET));
    }

    public static Optional<ConnectionError> checkAuthServer(HttpClient proxy) {
        String auth0HealthCheck = PropertyManager.getProperty("de.unijena.bioinf.sirius.security.authServer", null, "https://auth0.bright-giant.com") + "/pem";
        return checkConnectionToUrl(proxy, auth0HealthCheck)
                .map(e -> e.withNewMessage(2, "Could not connect to the Authentication Server: " + auth0HealthCheck,
                        ConnectionError.Klass.LOGIN_SERVER));
    }

    public static Optional<ConnectionError> checkLicenseServer(HttpClient proxy) {
        String url = PropertyManager.getProperty("de.unijena.bioinf.sirius.web.licenseServer", null, "https://gate.bright-giant.com/") +
                PropertyManager.getProperty("de.unijena.bioinf.sirius.web.licenseServer.version", null, "v1/")
                + "/actuator/health";
        return checkConnectionToUrl(proxy, url)
                .map(e -> e.withNewMessage(3, "Could not connect to the License Server: " + url,
                        ConnectionError.Klass.LICENSE_SERVER));
    }

    public static Optional<ConnectionError> checkConnectionToUrl(final HttpClient proxy, String url) {
        try {
            HttpResponse response = proxy.execute(new HttpHead(url));
            int code = response.getCode();
            LoggerFactory.getLogger(ProxyManager.class).debug("Testing internet connection");
            LoggerFactory.getLogger(ProxyManager.class).debug("Try to connect to: " + url);

            LoggerFactory.getLogger(ProxyManager.class).debug("Response Code: " + code);

            LoggerFactory.getLogger(ProxyManager.class).debug("Response Message: " + response.getReasonPhrase());
//            LoggerFactory.getLogger(ProxyManager.class).debug("Protocol Version: " + response.getProtocolVersion());
            if (code != HttpURLConnection.HTTP_OK) {
                LoggerFactory.getLogger(ProxyManager.class).warn("Error Response code: " + response.getReasonPhrase() + " " + code);
                return Optional.of(new ConnectionError(103,
                        "Error when connecting to: " + url + "Bad Response!",
                        ConnectionError.Klass.UNKNOWN, null, ConnectionError.Type.ERROR,
                        response.getCode(), response.getReasonPhrase()));
            }
            return Optional.empty();
        } catch (Exception e) {
            LoggerFactory.getLogger(ProxyManager.class).warn("Connection error", e);
            return Optional.of(new ConnectionError(102, "Error when connecting to: " + url,
                    ConnectionError.Klass.UNKNOWN, e));
        }
    }


    public static boolean isSSLValidationDisabled() {
        return !PropertyManager.getBoolean("de.unijena.bioinf.sirius.security.sslValidation", true);
    }

    //region HTTPClientManagement
    public static void disconnect() {
        boolean locked = false;
        try {
            locked = reconnectLock.writeLock().tryLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LoggerFactory.getLogger(ProxyManager.class).warn("Waiting for connection lock was interrupted!");
        }
        try {
            close(clients, CloseMode.IMMEDIATE);
            clients = null; // prevents reconnection
        } finally {
            if (locked)
                reconnectLock.writeLock().unlock();
        }
    }

    private static void close(final Map<String, Pair<CloseableHttpClient, PoolingHttpClientConnectionManager>> clients) {
        close(clients, CloseMode.GRACEFUL);
    }

    private static void close(final Map<String, Pair<CloseableHttpClient, PoolingHttpClientConnectionManager>> clients, @NotNull final CloseMode mode) {
        if (clients == null)
            return;
        clients.forEach((k, c) -> {
            c.getSecond().close(mode);
            c.getFirst().close(mode);
            LoggerFactory.getLogger(ProxyManager.class).info("Close clients: '" + k + "' Successfully closed!");
        });

    }

    private static final ReadWriteLock reconnectLock = new ReentrantReadWriteLock();

    public static void reconnect() {
        final Map<String, Pair<CloseableHttpClient, PoolingHttpClientConnectionManager>> old;
        boolean locked = false;
        try {
            locked = reconnectLock.writeLock().tryLock(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LoggerFactory.getLogger(ProxyManager.class).warn("Unexpected interruption when waiting for reconnect lock!");
        }
        try {
            if (!locked)
                LoggerFactory.getLogger(ProxyManager.class).warn("Could not acquire lock during reconnect. Some connections might be killed during reconnect.");
            old = clients;
            clients = new ConcurrentHashMap<>();
        } finally {
            if (locked)
                reconnectLock.writeLock().unlock();
        }
        SiriusJobs.runInBackground(() -> close(old, CloseMode.IMMEDIATE));
    }

    public static void closeAllStaleConnections() {
        closeAllStaleConnections(5, TimeUnit.MILLISECONDS);
    }

    public static void closeAllStaleConnections(final long duration, final TimeUnit timeUnit) {
        reconnectLock.readLock().lock();
        try {
            clients.forEach((k, v) -> closeStaleConnections(k, duration, timeUnit));
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    //
    private static void closeStaleConnections() {
        closeStaleConnections(POOL_CLIENT_ID);
    }

    private static void closeStaleConnections(@NotNull final String clientID) {
        closeStaleConnections(clientID, 5, TimeUnit.MILLISECONDS);
    }

    private static void closeStaleConnections(@NotNull final String clientID, final long duration, final TimeUnit timeUnit) {
        reconnectLock.readLock().lock();
        try {
            if (clients.containsKey(clientID)) {
//                LoggerFactory.getLogger(ProxyManager.class).warn("Start closing Stale connections for Pool '"+ clientID + "'...");
                PoolingHttpClientConnectionManager cm = clients.get(clientID).getSecond();
                cm.closeExpired();
                cm.closeIdle(TimeValue.of(duration, timeUnit));
//                LoggerFactory.getLogger(ProxyManager.class).warn("Closing Stale connections DONE for Pool '"+ clientID + "'!");
            }
        } finally {
            reconnectLock.readLock().unlock();
        }
    }


    static HttpClient client() {
        return client(POOL_CLIENT_ID);
    }

    static HttpClient client(String clientID) {
        if (clients == null)
            throw new IllegalStateException("ProxyManager has already been closed! Use reconnect to re-enable!");
        reconnectLock.readLock().lock();
        try {
            return clients.computeIfAbsent(clientID, k -> {
                PoolingHttpClientConnectionManager cm = POOL_CLIENT_ID.equals(k) ? connectionPoolManager() : connectionPoolManager(1, 1);
                HttpClientBuilder b = getSirirusHttpSyncClientBuilder(getProxyStrategy());
                b.setConnectionManager(cm);
                return Pair.create(b.build(), cm);
            }).getFirst();
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    public static void consumeClient(IOFunctions.IOConsumer<HttpClient> doWithClient) throws IOException {
        consumeClient(doWithClient, POOL_CLIENT_ID);
    }

    public static void consumeClient(IOFunctions.IOConsumer<HttpClient> doWithClient, String clientID) throws IOException {
        reconnectLock.readLock().lock();
        try {
            doWithClient.accept(client(clientID));
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    public static <T> T doWithClient(Function<HttpClient, T> doWithClient) {
        return doWithClient(doWithClient, POOL_CLIENT_ID);
    }

    public static <T> T doWithClient(Function<HttpClient, T> doWithClient, String clientID) {
        reconnectLock.readLock().lock();
        try {
            return doWithClient.apply(ProxyManager.client(clientID));
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    public static <T> T applyClient(IOFunctions.IOFunction<HttpClient, T> doWithClient) throws IOException {
        return applyClient(doWithClient, POOL_CLIENT_ID);
    }

    public static <T> T applyClient(IOFunctions.IOFunction<HttpClient, T> doWithClient, String clientID) throws IOException {
        reconnectLock.readLock().lock();
        try {
            return doWithClient.apply(ProxyManager.client(clientID));
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    public static void withConnectionLock(Runnable runner) {
        reconnectLock.writeLock().lock();
        try {
            runner.run();
        } finally {
            reconnectLock.writeLock().unlock();
        }
    }

    public static void withConnectionLock(ExFunctions.Runnable runner) throws Exception {
        reconnectLock.writeLock().lock();
        try {
            runner.run();
        } finally {
            reconnectLock.writeLock().unlock();
        }
    }

    private static final String POOL_CLIENT_ID = "POOL_CLIENT";
    private static ConcurrentHashMap<String, Pair<CloseableHttpClient, PoolingHttpClientConnectionManager>> clients = new ConcurrentHashMap<>();
}
