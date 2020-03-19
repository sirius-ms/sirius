package de.unijena.bioinf.webapi;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProxyManager {
//    public final static boolean DEBUG = PropertyManager.getBoolean("de.unijena.bioinf.ms.rest.DEBUG",false);
//    public static final int OK_STATE = 0;
//    public static final ProxyStrategy DEFAULT_STRATEGY = ProxyStrategy.SYSTEM;

    private static RequestConfig DEFAULT_CONFIG = RequestConfig.custom()
            .setConnectTimeout(15000)
//                .setConnectionRequestTimeout(30000)
            .setSocketTimeout(15000).build();

    public enum ProxyStrategy {SYSTEM, SIRIUS, NONE}

    private ProxyManager() {
    } //prevent instantiation

    public static ProxyStrategy getStrategyByName(String vlaue) {
        try {
            return ProxyStrategy.valueOf(vlaue);
        } catch (IllegalArgumentException e) {
            LoggerFactory.getLogger(ProxyStrategy.class).debug("Invalid Proxy Strategy state!", e);
            return null;
        }
    }

    public static ProxyStrategy getProxyStrategy() {
        return getStrategyByName(PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy", null, ProxyStrategy.SYSTEM.name()));
    }

    public static boolean useSystemProxyConfig() {
        return getProxyStrategy() == ProxyStrategy.SYSTEM;
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
        final CloseableHttpClient client;
        switch (strategy) {
            case SYSTEM:
                client = getJavaDefaultProxyClient();
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using Proxy Type " + ProxyStrategy.SYSTEM);
                break;
            case SIRIUS:
                client = getSiriusProxyClient();
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using Proxy Type " + ProxyStrategy.SIRIUS);
                break;
            case NONE:
                client = getNoProxyClient();
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using Proxy Type " + ProxyStrategy.NONE);
                break;
            default:
                client = getJavaDefaultProxyClient();
                LoggerFactory.getLogger(ProxyStrategy.class).debug("Using FALLBACK Proxy Type " + ProxyStrategy.SYSTEM);
        }

        return client;
    }


    //0 everything is fine
    //1 no connection to bioinf web site
    //2 no connection to uni jena
    //3 no connection to internet (google/microft/ubuntu????)
    public static int checkInternetConnection(final HttpClient client) {
        if (!checkBioinf(client)) {
            if (!checkJena(client)) {
                if (!checkExternal(client)) {
                    return 1;
                }
                return 2;
            }
            return 3;
        }
        return 0;
    }

    public static int checkInternetConnection() {
        try (CloseableHttpClient client = getSirirusHttpClient()) {
            return checkInternetConnection(client);
        } catch (IOException e) {
            LoggerFactory.getLogger(ProxyManager.class).error("Cant not create Http client", e);
        }
        return 3;
    }

    private static CloseableHttpClient getJavaDefaultProxyClient() {
        return HttpClientBuilder.create().useSystemProperties().setDefaultRequestConfig(DEFAULT_CONFIG).build();
    }

    private static CloseableHttpClient getNoProxyClient() {
        return HttpClientBuilder.create().setDefaultRequestConfig(DEFAULT_CONFIG).build();
    }

    private static CloseableHttpClient getSiriusProxyClient() {
        final String hostName = PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.hostname");
        final int port = Integer.valueOf(PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.port"));
        final String scheme = PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.scheme");

        if (Boolean.getBoolean(PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials"))) {
            return getClientBuilderWithProxySettings(
                    hostName,
                    port,
                    scheme,
                    PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials.user"),
                    PropertyManager.getProperty("de.unijena.bioinf.sirius.proxy.credentials.pw")
            ).build();
        } else {
            return getClientBuilderWithProxySettings(
                    hostName,
                    port,
                    scheme
            ).build();
        }
    }


    private static HttpClientBuilder getClientBuilderWithProxySettings(final String hostname, final int port, final String scheme) {
        return getClientBuilderWithProxySettings(hostname, port, scheme, null, null);

    }

    private static HttpClientBuilder getClientBuilderWithProxySettings(final String hostname, final int port, final String scheme, final String username, final String password) {
        HttpClientBuilder clientBuilder = HttpClients.custom().setDefaultRequestConfig(DEFAULT_CONFIG);
        BasicCredentialsProvider clientCredentials = new BasicCredentialsProvider();
        clientBuilder.setDefaultCredentialsProvider(clientCredentials);

        HttpHost proxy = new HttpHost(
                hostname,
                port,
                scheme
        );

        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        clientBuilder.setRoutePlanner(routePlanner);

        if (username != null && password != null) {
            clientCredentials.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(username, password));
        }
        return clientBuilder;
    }

    public static boolean checkExternal(HttpClient proxy) {
        return checkConnectionToUrl(proxy, "http://www.google.de");
    }

    public static boolean checkJena(HttpClient proxy) {
        return checkConnectionToUrl(proxy, "http://www.uni-jena.de");
    }

    public static boolean checkBioinf(HttpClient proxy) {
        return checkConnectionToUrl(proxy, "https://bio.informatik.uni-jena.de");
    }

    public static boolean checkConnectionToUrl(final HttpClient proxy, String url) {
        try {
            HttpResponse response = proxy.execute(new HttpHead(url));
            int code = response.getStatusLine().getStatusCode();
            LoggerFactory.getLogger(ProxyManager.class).debug("Testing internet connection");
            LoggerFactory.getLogger(ProxyManager.class).debug("Try to connect to: " + url);

            LoggerFactory.getLogger(ProxyManager.class).debug("Response Code: " + code);

            LoggerFactory.getLogger(ProxyManager.class).debug("Response Message: " + response.getStatusLine().getReasonPhrase());
            LoggerFactory.getLogger(ProxyManager.class).debug("Protocol Version: " + response.getStatusLine().getProtocolVersion());
            if (code != HttpURLConnection.HTTP_OK) {
                LoggerFactory.getLogger(ProxyManager.class).warn("Error Response code: " + response.getStatusLine().getReasonPhrase() + " " + code);
                return false;
            }
            return true;
        } catch (Exception e) {
            LoggerFactory.getLogger(ProxyManager.class).warn("Connection error", e);
        }
        return false;
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


    private static final Set<GentlyHttpClientCloser> closingContainers = Collections.newSetFromMap(new ConcurrentHashMap());
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
