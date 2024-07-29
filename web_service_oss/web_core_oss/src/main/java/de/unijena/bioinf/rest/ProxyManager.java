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
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProxyManager {


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

    private static OkHttpClient getSirirusHttpClient(boolean pooled) {
        if (pooled)
            return decorateWithPoolSettings(getSirirusHttpClientBuilder(getProxyStrategy())).build();
        return getSirirusHttpClient(2, 3, 1);
    }

    private static OkHttpClient getSirirusHttpClient(int maxPerRoute, int maxTotal, int keeAlive) {
        return getSirirusHttpClient(getProxyStrategy(), maxPerRoute, maxTotal, keeAlive);
    }

    private static OkHttpClient getSirirusHttpClient(ProxyStrategy strategy, int maxPerRoute, int maxTotal, int keeAlive) {
        return decorateWithPoolSettings(maxPerRoute, maxTotal, keeAlive, getSirirusHttpClientBuilder(strategy)).build();
//        b.setConnectionReuseStrategy((request, response, context) -> response.getCode() < 400);
//        b.setConnectionManagerShared();
//        b.disableAutomaticRetries();
//        b.disableConnectionState()
    }

    private static OkHttpClient.Builder getSirirusHttpClientBuilder(ProxyStrategy strategy) {
        final OkHttpClient.Builder clientBuilder = decorateWithSSLValidationSettings(
                decorateWithDefaultSettings(new OkHttpClient.Builder())
        );

        if (strategy.equals(ProxyStrategy.SIRIUS))
            decorateWithProxySettings(clientBuilder);

        LoggerFactory.getLogger(ProxyStrategy.class).debug("Using Proxy Type {}", strategy);

        return clientBuilder;
    }


    //0 everything is fine
    //1 no connection to Internet
    //2 no connection to Auth0 Server
    //3 no connection to BG License Server
    public static Optional<List<ConnectionError>> checkInternetConnection(final OkHttpClient client) {
        List<ConnectionError> failedChecks = new ArrayList<>();
        checkLicenseServer(client).ifPresent(failedChecks::add);
        checkAuthServer(client).ifPresent(failedChecks::add);
        checkExternal(client).ifPresent(failedChecks::add);
        return failedChecks.isEmpty() ? Optional.empty() : Optional.of(failedChecks);
    }

    public static Optional<List<ConnectionError>> checkInternetConnection() {
        return checkInternetConnection(getSirirusHttpClient(false));
    }

    private static OkHttpClient.Builder decorateWithPoolSettings(final OkHttpClient.Builder builder) {
        int maxPerRoute = Math.min(Math.max(1, SiriusJobs.getCPUThreads()), PropertyManager.getInteger("de.unijena.bioinf.sirius.http.maxRoute", 2));
        return decorateWithPoolSettings(maxPerRoute,
                PropertyManager.getInteger("de.unijena.bioinf.sirius.http.maxTotal", 5),
                PropertyManager.getInteger("de.unijena.bioinf.sirius.http.maxIdle", 3),
                builder);
    }

    private static OkHttpClient.Builder decorateWithPoolSettings(int maxPerRoute, int maxTotal, int keeAlive, final OkHttpClient.Builder builder) {

        LoggerFactory.getLogger(ProxyManager.class).info("Starting http Client with MaxPerRoute=" + maxPerRoute + " / maxTotal=" + maxTotal + " (CPU-Threads=" + SiriusJobs.getCPUThreads() + ").");
        ConnectionPool pool = new ConnectionPool(keeAlive,
                PropertyManager.getInteger("de.unijena.bioinf.sirius.http.keepAlive", 180000),
                TimeUnit.MILLISECONDS);

        Dispatcher dispatcher = new Dispatcher(SiriusJobs.getGlobalJobManager().getDefaultCacheThreadPool());
        dispatcher.setMaxRequests(maxTotal);
        dispatcher.setMaxRequestsPerHost(maxPerRoute);

        return builder.connectionPool(pool).dispatcher(dispatcher);
    }

    private static OkHttpClient.Builder decorateWithDefaultSettings(OkHttpClient.Builder builder) {
        //todo check timeouts
        return builder.connectTimeout(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.connectTimeout", 15000), TimeUnit.MILLISECONDS)
                .readTimeout(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.readTimeout", 15000), TimeUnit.MILLISECONDS)
                .writeTimeout(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.writeTimeout", 15000), TimeUnit.MILLISECONDS)
                .callTimeout(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.callTimeout", 0), TimeUnit.MILLISECONDS)
                .cookieJar(CookieJar.NO_COOKIES);
    }

    @NotNull
    private static OkHttpClient.Builder decorateWithSSLValidationSettings(@NotNull OkHttpClient.Builder builder) {
        if (isSSLValidationDisabled()) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
                builder.hostnameVerifier((hostname, session) -> true);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                LoggerFactory.getLogger(ProxyManager.class).warn("Could not create Noop SSL context. SSL Validation will NOT be disabled!");
            }
        }
        return builder;
    }


    private static OkHttpClient.Builder decorateWithProxySettings(final OkHttpClient.Builder builder) {
        final String hostName = PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.hostname");
        final int port = Integer.parseInt(PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.port"));

        if (Boolean.getBoolean(PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials"))) {
            return decorateWithProxySettings(builder,
                    hostName,
                    port,
                    PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials.user"),
                    PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials.pw")
            );
        }

        return decorateWithProxySettings(builder, hostName, port, null, null);
    }

    private static OkHttpClient.Builder decorateWithProxySettings(
            final OkHttpClient.Builder builder,
            final String hostname, final int port,
            final String username, final String password) {

        final Authenticator proxyAuthenticator = (route, response) -> {
            String credential = Credentials.basic(username, password);
            return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
        };
        //todo allow socks proxy?
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, port));

        builder.proxy(proxy);
        if (username != null && !username.isBlank() && password != null && !password.isBlank())
            builder.proxyAuthenticator(proxyAuthenticator);

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


    public static Optional<ConnectionError> checkExternal(OkHttpClient proxy) {
        String url = PropertyManager.getProperty("de.unijena.bioinf.sirius.web.external", null, "https://www.google.de/");
        return checkConnectionToUrl(proxy, url)
                .map(e -> e.withNewMessage(1, "Could not connect to the Internet: " + url,
                        ConnectionError.Klass.INTERNET));
    }

    public static Optional<ConnectionError> checkAuthServer(OkHttpClient proxy) {
        String auth0HealthCheck = PropertyManager.getProperty("de.unijena.bioinf.sirius.security.authServer", null, "https://auth0.bright-giant.com") + "/pem";
        return checkConnectionToUrl(proxy, auth0HealthCheck)
                .map(e -> e.withNewMessage(2, "Could not connect to the Authentication Server: " + auth0HealthCheck,
                        ConnectionError.Klass.LOGIN_SERVER));
    }

    public static Optional<ConnectionError> checkLicenseServer(OkHttpClient proxy) {
        String url = PropertyManager.getProperty("de.unijena.bioinf.sirius.web.licenseServer", null, "https://gate.bright-giant.com/") +
                PropertyManager.getProperty("de.unijena.bioinf.sirius.web.licenseServer.version", null, "v3/")
                + "/actuator/health";
        return checkConnectionToUrl(proxy, url)
                .map(e -> e.withNewMessage(3, "Could not connect to the License Server: " + url,
                        ConnectionError.Klass.LICENSE_SERVER));
    }

    public static Optional<ConnectionError> checkConnectionToUrl(final OkHttpClient proxy, String url) {
        try (Response response = proxy.newCall(new Request.Builder().head().url(url).build()).execute()) {
            int code = response.code();
            LoggerFactory.getLogger(ProxyManager.class).debug("Testing internet connection");
            LoggerFactory.getLogger(ProxyManager.class).debug("Try to connect to: " + url);

            LoggerFactory.getLogger(ProxyManager.class).debug("Response Code: " + code);

            LoggerFactory.getLogger(ProxyManager.class).debug("Response Message: " + response.message());
            if (code != HttpURLConnection.HTTP_OK) {
                LoggerFactory.getLogger(ProxyManager.class).warn("Error Response code: " + response.message() + " " + code);
                return Optional.of(new ConnectionError(103,
                        "Error when connecting to: " + url + "Bad Response!",
                        ConnectionError.Klass.UNKNOWN, null, ConnectionError.Type.ERROR,
                        response.code(), response.message()));
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
            close(clients);
            clients = null; // prevents reconnection
        } finally {
            if (locked)
                reconnectLock.writeLock().unlock();
        }
    }

    private static void close(final ConcurrentHashMap<String, OkHttpClient> clients) {
        if (clients == null)
            return;
        clients.forEach((k, c) -> {
            c.connectionPool().evictAll();
            c.dispatcher().cancelAll();
            c.connectionPool().evictAll();
            LoggerFactory.getLogger(ProxyManager.class).info("Close clients: '" + k + "' Successfully closed!");
        });
    }

    private static final ReadWriteLock reconnectLock = new ReentrantReadWriteLock();

    public static void reconnect() {
        final ConcurrentHashMap<String, OkHttpClient> old;
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
        SiriusJobs.runInBackground(() -> close(old));
    }

    public static void closeAllStaleConnections() {
        reconnectLock.readLock().lock();
        try {
            clients.forEach((k, v) -> closeStaleConnections(k));
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    //
    private static void closeStaleConnections() {
        closeStaleConnections(POOL_CLIENT_ID);
    }

    private static void closeStaleConnections(@NotNull final String clientID) {
        reconnectLock.readLock().lock();
        try {
            if (clients.containsKey(clientID)) {
//                LoggerFactory.getLogger(ProxyManager.class).warn("Start closing Stale connections for Pool '"+ clientID + "'...");
                clients.get(clientID).connectionPool().evictAll();
//                LoggerFactory.getLogger(ProxyManager.class).warn("Closing Stale connections DONE for Pool '"+ clientID + "'!");
            }
        } finally {
            reconnectLock.readLock().unlock();
        }
    }


    static OkHttpClient client() {
        return client(POOL_CLIENT_ID);
    }

    static OkHttpClient client(String clientID) {
        if (clients == null)
            throw new IllegalStateException("ProxyManager has already been closed! Use reconnect to re-enable!");
        reconnectLock.readLock().lock();
        try {
            return clients.computeIfAbsent(clientID, k -> getSirirusHttpClient(POOL_CLIENT_ID.equals(k)));
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    public static void initClient(String clientID, int maxPerRoute, int maxTotal, int keeAlive) {
        if (clients == null)
            throw new IllegalStateException("ProxyManager has already been closed! Use reconnect to re-enable!");
        reconnectLock.readLock().lock();
        try {
            clients.computeIfAbsent(clientID, k -> getSirirusHttpClient(maxPerRoute, maxTotal, keeAlive));
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    public static void consumeClient(IOFunctions.IOConsumer<OkHttpClient> doWithClient) throws IOException {
        consumeClient(doWithClient, POOL_CLIENT_ID);
    }

    public static void consumeClient(IOFunctions.IOConsumer<OkHttpClient> doWithClient, String clientID) throws IOException {
        checkTimeAndWait(clientID);
        reconnectLock.readLock().lock();
        try {
            doWithClient.accept(client(clientID));
        } catch (IOException e) {
            e.printStackTrace();
            throw new SiriusHttpException(clientID, e);
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    public static <T> T doWithClient(Function<OkHttpClient, T> doWithClient) {
        return doWithClient(doWithClient, POOL_CLIENT_ID);
    }

    public static <T> T doWithClient(Function<OkHttpClient, T> doWithClient, String clientID) {
        checkTimeAndWait(clientID);
        reconnectLock.readLock().lock();
        try {
            return doWithClient.apply(ProxyManager.client(clientID));
        } finally {
            reconnectLock.readLock().unlock();
        }
    }

    public static <T> T applyClient(IOFunctions.IOFunction<OkHttpClient, T> doWithClient) throws IOException {
        return applyClient(doWithClient, POOL_CLIENT_ID);
    }

    public static <T> T applyClient(IOFunctions.IOFunction<OkHttpClient, T> doWithClient, String clientID) throws IOException {
        checkTimeAndWait(clientID);
        reconnectLock.readLock().lock();
        try {
            return doWithClient.apply(ProxyManager.client(clientID));
        } catch (IOException e) {
            e.printStackTrace();
            throw new SiriusHttpException(clientID, e);
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
    private static ConcurrentHashMap<String, OkHttpClient> clients = new ConcurrentHashMap<>();

    private static final int requestPauseMillis = PropertyManager.getInteger("de.unijena.bioinf.sirius.http.requestPause", 125);
    private static final AtomicLong lastCall = new AtomicLong(System.currentTimeMillis());

    private static void checkTimeAndWait(String clientID) {
        if (requestPauseMillis <= 0)
            return;
        if (!POOL_CLIENT_ID.equals(clientID))
            return;

        while (true) {
            long wait;
            synchronized (lastCall) {
                wait = (lastCall.get() + requestPauseMillis) - System.currentTimeMillis();
                if (wait <= 0) {
                    lastCall.set(System.currentTimeMillis());
                    break;
                }
            }
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ignored) {
            }
        }

    }
}
