package de.unijena.bioinf.sirius.net;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 21.02.17.
 */

import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

import java.io.IOException;
import java.net.*;
import java.util.Iterator;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Proxies {
    public static final String HTTPS_SCHEME = "https";
    public static final String HTTP_SCHEME = "http";

    public static HttpClientBuilder getSystemSystemPropertiesClientBuilder() {
        return HttpClientBuilder.create().useSystemProperties();
    }


    public static HttpClientBuilder getJavaDefaultProxyClientBuilder() {
        return getJavaDefaultProxyClientBuilder(HTTP_SCHEME);
    }

    // this method inits the proxy configuration at program starts
    public static void initProxySettings(){}

    //0 everything is fine
    //1 no push to csi fingerid possible
    //2 no connection to fingerid web site
    //3 no connection to bioinf web site
    //4 no connection to uni jena
    //5 no connection to internet (google/microft/ubuntu????)
    public static void initCheckInternetConnection(){}


    //todo finish
//    public static int getProxyPort(){}
//    public static String getProxyHost(){}
//    public static String getProxyScheme{}

    public static HttpClientBuilder getJavaDefaultProxyClientBuilder(final String scheme) {
        final String hostName = System.getProperty("http.proxyHost");
        final int port = Integer.valueOf(System.getProperty("http.proxyPort"));
        final String name = System.getProperty("http.proxyUser");
        final String pw = System.getProperty("http.proxyPassword");


        return getClientBuilderWithProxySettings(
                hostName,
                port,
                scheme,
                name,
                pw
        );
    }

    public static HttpClientBuilder getSiriusProxyClientBuilder() {
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
            );
        } else {
            return getClientBuilderWithProxySettings(
                    hostName,
                    port,
                    scheme
            );
        }

    }

    public static HttpClientBuilder getClientBuilderWithProxySettings(final String hostname, final int port, final String scheme) {
        return getClientBuilderWithProxySettings(hostname, port, scheme, null, null);

    }

    public static HttpClientBuilder getClientBuilderWithProxySettings(final String hostname, final int port, final String scheme, final String username, final String password) {
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


    public void checkApacheHttpConnection(HttpClient client){

    }

    public void checkJavaNetHttpConnection(HttpClient client){

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
            URI proxyURL = new URI(WebAPI.FINGERID_WEBSITE);

            List<Proxy> l = ProxySelector.getDefault().select(
                    proxyURL );

            for (Iterator<Proxy> iter = l.iterator(); iter.hasNext();) {
                Proxy proxy = iter.next();
                System.out.println("proxy Type : " + proxy.type());
                InetSocketAddress addr = (InetSocketAddress) proxy.address();

                if (addr == null) {
                    System.out.println("No Proxy");
                }
                checkGoogle(proxy);
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

            try {
                System.out.println("manual Proxy check: ");
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, Integer.valueOf(port)));
                checkGoogle(proxy);
                System.out.println();
        } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        try {
            System.out.println("NO Proxy check: ");
            checkGoogle(null);
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static void checkGoogle(Proxy proxy) throws IOException {
        URL u = new  URL("http://www.google.de");
        HttpURLConnection con;
        if (proxy != null) {
            con = (HttpURLConnection)u.openConnection(proxy);
            System.out.println("proxy hostname : " + proxy.address());
            System.out.println("proxy type : " + proxy.type());

        } else {
            con = (HttpURLConnection)u.openConnection();
        }
        con.setConnectTimeout(2048);
        con.connect();
        System.out.println(con.getResponseCode() + " : " + con.getResponseMessage());
        System.out.println(con.getResponseCode() == HttpURLConnection.HTTP_OK);
        System.out.println("Uses Proxy: " + con.usingProxy());

    }


}
