package de.unijena.bioinf.ms.rest.client;

import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.SecurityService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class AbstractClient {
    public final static boolean DEBUG = PropertyManager.getBoolean("de.unijena.bioinf.ms.rest.DEBUG",false);
    protected static final String API_ROOT = "/api";
    protected static final String CID = SecurityService.generateSecurityToken();


    @NotNull
    protected URI serverUrl;

    protected AbstractClient(@Nullable URI serverUrl) {
        this.serverUrl = Objects.requireNonNullElseGet(serverUrl, () -> URI.create(FingerIDProperties.fingeridWebHost()));
    }

    public void setServerUrl(@NotNull URI serverUrl) {
        this.serverUrl = serverUrl;
    }

    @NotNull
    protected InputStreamReader getIn(HttpEntity entity) throws IOException {
        final Charset charset = ContentType.getOrDefault(entity).getCharset();
        return new InputStreamReader(entity.getContent(), charset == null ? StandardCharsets.UTF_8 : charset);
    }

    public boolean testConnection() {
        try {
            URIBuilder builder = getBaseURI("/actuator/health", true);
            HttpURLConnection urlConn = (HttpURLConnection) builder.build().toURL().openConnection();
            urlConn.connect();

            return HttpURLConnection.HTTP_OK == urlConn.getResponseCode();
        } catch (IOException | URISyntaxException e) {
            return false;
        }
    }

    protected void isSuccessful(HttpResponse response) throws IOException {
        final StatusLine status = response.getStatusLine();
        if (status.getStatusCode() >= 400)
            throw new IOException("Error when querying REST service. Bad Response Code: "
                    + status.getStatusCode() + " | Message: " + status.getReasonPhrase());
    }

    //#################################################################################################################
    //region PathBuilderMethods
    protected URIBuilder getBaseURI(@Nullable String path, final boolean versionSpecificPath) throws URISyntaxException {
        if (path == null)
            path = "";

        URIBuilder b;
        if (DEBUG) {
            b = new URIBuilder().setScheme("http").setHost("localhost");
            b = b.setPort(8080);
//            path = FINGERID_DEBUG_FRONTEND_PATH + path;
        } else {
            b = new URIBuilder(serverUrl);
            if (versionSpecificPath)
                path = "/v" + FingerIDProperties.fingeridVersion() + path; //todo check if this works
        }

        if (!path.isEmpty())
            b = b.setPath(path);

        return b;
    }

    // WebAPI paths
    protected StringBuilder getWebAPIBasePath() {
        return new StringBuilder(API_ROOT);
    }

    protected URIBuilder buildWebapiURI(@Nullable final String path, final boolean versionSpecific) throws URISyntaxException {
        StringBuilder pathBuilder = getWebAPIBasePath();

        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/"))
                pathBuilder.append("/");

            pathBuilder.append(path);
        }

        return getBaseURI(pathBuilder.toString(), versionSpecific);
    }

    protected URIBuilder buildVersionLessWebapiURI(@Nullable String path) throws URISyntaxException {
        return buildWebapiURI(path, false);
    }


    protected URIBuilder buildVersionSpecificWebapiURI(@Nullable String path) throws URISyntaxException {
        return buildWebapiURI(path, true);
    }

    //endregion
    //#################################################################################################################
}
