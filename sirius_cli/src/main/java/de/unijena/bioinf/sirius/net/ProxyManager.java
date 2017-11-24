package de.unijena.bioinf.sirius.net;

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProxyManager {
    public final static boolean DEBUG = false;
    public static final String HTTPS_SCHEME = "https";
    public static final String HTTP_SCHEME = "http";
    public static final int OK_STATE = 0;
    public static final ProxyStrategy DEFAULT_STRATEGY = ProxyStrategy.SYSTEM;

    public enum ProxyStrategy {SYSTEM, SIRIUS, NONE}

    public static ProxyStrategy getStrategyByName(String vlaue) {
        try {
            return ProxyStrategy.valueOf(vlaue);
        } catch (IllegalArgumentException e) {
            LoggerFactory.getLogger(ProxyStrategy.class).debug("Invalid Proxy Strategy state!", e);
            return null;
        }
    }

    public static ProxyStrategy getProxyStrategy() {
        return getStrategyByName(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.proxy", ProxyStrategy.SYSTEM.name()));
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

    public static CloseableHttpClient getTestedHttpClient() {
        return getTestedHttpClient(true);
    }


    public static CloseableHttpClient getTestedHttpClient(final boolean failover) {
        CloseableHttpClient client = getSirirusHttpClient();
        if (hasInternetConnection(client)) {
            return client;
        } else if (failover) {
            LoggerFactory.getLogger(ProxyManager.class).warn("No connection with selected setting. Searching for Failover Settings!");
            for (ProxyStrategy strategy : ProxyStrategy.values()) {
                CloseableHttpClient failoverClient = getSirirusHttpClient(strategy);
                if (hasInternetConnection(client)) {
                    client = failoverClient;
                    break;
                }
            }
        }
        return client;
    }

    //0 everything is fine
    //1 no connection to bioinf web site
    //2 no connection to uni jena
    //3 no connection to internet (google/microft/ubuntu????)
    public static int checkInternetConnection(final CloseableHttpClient client) {
        if (!checkBioinf(client)) {
            if (!checkJena(client)) {
                if (!checkExternal(client)) {
                    return 1;
                } else {
                    return 2;
                }
            } else {
                return 3;
            }
        } else {
            return 0;
        }
    }

    public static int checkInternetConnection() {
        CloseableHttpClient client = getSirirusHttpClient();
        int val = checkInternetConnection(client);
        return val;
    }

    public static boolean hasInternetConnection(final CloseableHttpClient client) {
        return checkInternetConnection(client) == OK_STATE;
    }

    public static boolean hasInternetConnection() {
        return checkInternetConnection() == OK_STATE;
    }


    private static CloseableHttpClient getJavaDefaultProxyClient() {
        return HttpClients.createSystem();
    }

    private static CloseableHttpClient getNoProxyClient() {
        return HttpClients.createDefault();
    }

    private static CloseableHttpClient getSiriusProxyClient() {
        final String hostName = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.proxy.hostname");
        final int port = Integer.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.proxy.port"));
        final String scheme = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.proxy.scheme");

        if (Boolean.getBoolean(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.proxy.credentials"))) {
            return getClientBuilderWithProxySettings(
                    hostName,
                    port,
                    scheme,
                    PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.proxy.credentials.user"),
                    PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.proxy.credentials.pw")
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
        HttpClientBuilder clientBuilder = HttpClients.custom();
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


    /*public static void main(String[] args) {
        String versionString = ApplicationCore.VERSION_STRING;
        System.out.println("System settings");
        System.out.println("use system proxy? " + PropertyManager.PROPERTIES.getProperty("java.net.useSystemProxies"));
        String port = PropertyManager.PROPERTIES.getProperty("http.proxyPort");
        System.out.println("http port: " + port);
        String host = PropertyManager.PROPERTIES.getProperty("http.proxyHost");
        System.out.println("http host: " + host);
        System.out.println();

        try {
            for (ProxyStrategy strategy : ProxyStrategy.values()) {
                System.out.println("checking strategy: " + strategy);
                CloseableHttpClient client = getSirirusHttpClient(strategy);
                int status = checkInternetConnection(client);
                System.out.println("Sirius connection state: " + status);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public static boolean checkExternal(CloseableHttpClient proxy) {
        return checkConnectionToUrl(proxy, "http://www.google.de");
    }

    public static boolean checkJena(CloseableHttpClient proxy) {
        return checkConnectionToUrl(proxy, "http://www.uni-jena.de");
    }

    public static boolean checkBioinf(CloseableHttpClient proxy) {
        return checkConnectionToUrl(proxy, "https://bio.informatik.uni-jena.de");
    }

    public static boolean checkConnectionToUrl(final CloseableHttpClient proxy, String url) {
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

}
