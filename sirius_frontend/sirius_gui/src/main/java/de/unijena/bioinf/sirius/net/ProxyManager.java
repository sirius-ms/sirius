package de.unijena.bioinf.sirius.net;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 21.02.17.
 */

import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.RESTDatabase;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
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
    public static final String HTTPS_SCHEME = "https";
    public static final String HTTP_SCHEME = "http";
    public static final int MAX_STATE = 4;
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
        return getStrategyByName(System.getProperty("de.unijena.bioinf.sirius.proxy"));
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

    private static int errorState;


    // this method inits the proxy configuration at program start
    public static CloseableHttpClient getSirirusHttpClient() {
        return getSirirusHttpClient(getProxyStrategy());
    }

    public static CloseableHttpClient getSirirusHttpClient(ProxyStrategy strategy) {
        switch (strategy) {
            case SYSTEM:
                return getJavaDefaultProxyClientBuilder();
            case SIRIUS:
                return getSiriusProxyClientBuilder();
            case NONE:
                return getNoProxyClientBuilder();
            default:
                return getJavaDefaultProxyClientBuilder();
        }
        //endregion

    }

    public static CloseableHttpClient getTestedSirirusHttpClient() {
        return getTestedSirirusHttpClient(true);
    }


    public static CloseableHttpClient getTestedSirirusHttpClient(final boolean failover) {
        CloseableHttpClient client = getSirirusHttpClient();
        if (hasInternetConnection(client)) {
            return client;
        } else if (failover) {
            LoggerFactory.getLogger(ProxyManager.class).warn("No connection with selected setting. Searching for Failover Settings!");
            for (ProxyStrategy strategy : ProxyStrategy.values()) {
                CloseableHttpClient failoverClient = getSirirusHttpClient(strategy);
                if (hasInternetConnection(client)) return failoverClient;
            }
        }
        return client;
    }

    //0 everything is fine
    //1 no push to csi fingerid possible
    //2 no connection to fingerid web site
    //3 no connection to bioinf web site
    //4 no connection to uni jena
    //5 no connection to internet (google/microft/ubuntu????)
    public static int checkInternetConnection() {
        return checkInternetConnection(getSirirusHttpClient());
    }

    public static boolean hasInternetConnection(final CloseableHttpClient client) {
        return checkInternetConnection(client) == OK_STATE;
    }

    public static boolean hasInternetConnection() {
        return checkInternetConnection() == OK_STATE;
    }

    public static int checkInternetConnection(final CloseableHttpClient client) {

        if (!checkFingerID(client)) {
            if (!checkBioinf(client)) {
                if (!checkJena(client)) {
                    if (!checkExternal(client)) {
                        errorState = 4;
                    } else {
                        errorState = 3;
                    }
                } else {
                    errorState = 2;
                }
            } else {
                errorState = 1;
            }
        } else {
            errorState = 0;
        }

        return errorState;

    }


    //todo finish
//    public static int getProxyPort(){}
//    public static String getProxyHost(){}
//    public static String getProxyScheme{}

    private static CloseableHttpClient getJavaDefaultProxyClientBuilder() {
        /*final String hostName = System.getProperty("http.proxyHost");
        final int port = Integer.valueOf(System.getProperty("http.proxyPort"));
        final String name = System.getProperty("http.proxyUser");
        final String pw = System.getProperty("http.proxyPassword");


        return getClientBuilderWithProxySettings(
                hostName,
                port,
                scheme,
                name,
                pw
        );*/
        return HttpClients.createSystem();

    }

    private static CloseableHttpClient getNoProxyClientBuilder() {
        return HttpClients.createDefault();
    }

    private static CloseableHttpClient getSiriusProxyClientBuilder() {
        String versionString = ApplicationCore.VERSION_STRING;
        final String hostName = System.getProperty("de.unijena.bioinf.sirius.proxy.hostname");
        final int port = Integer.valueOf(System.getProperty("de.unijena.bioinf.sirius.proxy.port"));
        final String scheme = System.getProperty("de.unijena.bioinf.sirius.proxy.scheme");

        if (Boolean.getBoolean(System.getProperty("de.unijena.bioinf.sirius.proxy.credentials"))) {
            return getClientBuilderWithProxySettings(
                    hostName,
                    port,
                    scheme,
                    System.getProperty("de.unijena.bioinf.sirius.proxy.credentials.user"),
                    System.getProperty("de.unijena.bioinf.sirius.proxy.credentials.pw")
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


    public static void main(String[] args) {
        System.out.println("System settings");
        System.out.println("use system proxy? " + System.getProperty("java.net.useSystemProxies"));
        String port = System.getProperty("http.proxyPort");
        System.out.println("http port: " + port);
        String host = System.getProperty("http.proxyHost");
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
    }

    public static boolean checkExternal(CloseableHttpClient proxy) {
        return checkConnectionToUrl(proxy, "http://www.google.de");
    }

    public static boolean checkJena(CloseableHttpClient proxy) {
        return checkConnectionToUrl(proxy, "http://www.uni-jena.de");
    }

    public static boolean checkBioinf(CloseableHttpClient proxy) {
        return checkConnectionToUrl(proxy, "https://bio.informatik.uni-jena.de");
    }


    public static boolean checkFingerID(CloseableHttpClient proxy) {
        return new RESTDatabase(null, BioFilter.ALL, null, proxy).testConnection();
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
