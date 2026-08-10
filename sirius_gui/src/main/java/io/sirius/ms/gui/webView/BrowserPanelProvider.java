package io.sirius.ms.gui.webView;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.gui.configs.Colors;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

public abstract class BrowserPanelProvider<BP extends BrowserPanel> {

    protected static final String CSS_LIGHT_RESOURCE_TEXT = "/sirius/style-light.css";
    protected static final String CSS_DARK_RESOURCE_TEXT = "/sirius/style-dark.css";

    protected static final String CSS_LIGHT_RESOURCE = "/js/styles.css";
    protected static final String CSS_DARK_RESOURCE = "/js/styles-dark.css";

    protected static String THEME_REST_PARA = "?theme=" + (Colors.isDarkTheme() ? "dark" : "light");

    @NotNull
    @Getter
    private final URI baseUrl;

    public URI resolveBaseUrl(URI uri) {
        return baseUrl.resolve(uri);
    }

    public URI resolveBaseUrl(String str) {
        return baseUrl.resolve(str);
    }

    protected BrowserPanelProvider(@NotNull URI baseUrl) {
        this.baseUrl = baseUrl;
    }


    public BrowserPanel makeReactPanel(@NotNull String appPath, @NotNull String paramName, @NotNull String paramValue){
        String url = baseUrl.resolve(appPath) + THEME_REST_PARA + "&" + paramName + "=" + paramValue;
        return newLoadingBrowserPanel(url);
    }

    public BrowserPanel makeReactPanel(@NotNull String appPath, @NotNull String projectId){
        return makeReactPanel(appPath, projectId, null, null, null, null);
    }

    public BrowserPanel makeReactPanel(@NotNull String appPath,
                                      @NotNull String projectId,
                                      @Nullable String alignedFeatureId,
                                      @Nullable String formulaId,
                                      @Nullable String inchiKey,
                                      @Nullable String matchId){
        String url = baseUrl.resolve(appPath) +  makeParameters(projectId, alignedFeatureId, formulaId, inchiKey, matchId);
        return newLoadingBrowserPanel(url);
    }

    public BP makeHTMLTextPanel(String htmlText) {
        return makeHTMLTextPanel(htmlText, Colors.BACKGROUND);
    }

    public BP makeHTMLTextPanel(String htmlText, Color background) {
        final StringBuilder buf = new StringBuilder();
        try (final BufferedReader br = FileUtils.ensureBuffering(new InputStreamReader(getClass().getResourceAsStream("/sirius/text.html")))) {
            String line;
            while ((line = br.readLine()) != null) buf.append(line).append('\n');
            String htmlContent = buf.toString().replace("#BACKGROUND#", "#" + Integer.toHexString(background.getRGB()).substring(2)).replace("#TEXT#", htmlText);
            return makeHTMLPanel(htmlContent, Colors.isDarkTheme() ? CSS_DARK_RESOURCE_TEXT : CSS_LIGHT_RESOURCE_TEXT);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public BP makeHTMLPanel(String htmlContent) {
        return makeHTMLPanel(htmlContent, Colors.isDarkTheme() ? CSS_DARK_RESOURCE : CSS_LIGHT_RESOURCE);
    }

    public BP makeHTMLPanel(String htmlContent, String cssResource) {
        // Load the data URL
        return newBrowserPanel(toHtmlDataUrl(htmlContent, cssResource), LinkInterception.ALL);
    }

    /**
     * Turns the given html into a data url that a browser can load directly, so it does not have to
     * be served from anywhere.
     */
    protected String toHtmlDataUrl(String htmlContent, String cssResource) {
        // Include the CSS in the HTML if provided
        String cssContent = WebViewUtils.loadCSSAndSetColorThemeAndFont(cssResource);
        if (cssContent != null && !cssContent.isEmpty()) {
            String styleTag = "<style>" + cssContent + "</style>";
            // Insert style tag into head if exists, otherwise add it at the beginning
            if (htmlContent.contains("<head>")) {
                htmlContent = htmlContent.replace("<head>", "<head>" + styleTag);
            } else {
                htmlContent = styleTag + htmlContent;
            }
        }

        // Create data URL with base64 encoding
        return WebViewUtils.textToDataURL(htmlContent);
    }

    public abstract BP newBrowserPanel(@NotNull String fullUrlWithParameters, @NotNull LinkInterception linkInterception);

    public BP newBrowserPanel(@NotNull String fullUrlWithParameters){
        return newBrowserPanel(fullUrlWithParameters, LinkInterception.NONE);
    }

    /**
     * Creates a browser panel that shows a loading animation until its web view has been created in
     * the background. To be used for web views that take long enough to start that waiting for them
     * would block the event dispatch thread noticeably, which is the case for the react apps.
     * <p>
     * Not used for the panels showing plain html, since a data url is loaded immediately and a
     * loading animation would be bigger than the content of e.g. a single line of text.
     */
    public BrowserPanel newLoadingBrowserPanel(@NotNull String fullUrlWithParameters){
        return newLoadingBrowserPanel(fullUrlWithParameters, LinkInterception.NONE);
    }

    public BrowserPanel newLoadingBrowserPanel(@NotNull String fullUrlWithParameters, @NotNull LinkInterception linkInterception){
        return new LoadingBrowserPanel(() -> newBrowserPanel(fullUrlWithParameters, linkInterception));
    }

    /**
     * Starts building a window showing one of the react apps. It shows a loading animation until the
     * app is ready, since starting one takes seconds.
     *
     * @see #makeReactPanel(String, String, String)
     */
    public BrowserWindowBuilder newReactWindow(@NotNull String appPath, @NotNull String paramName, @NotNull String paramValue) {
        String url = baseUrl.resolve(appPath) + THEME_REST_PARA + "&" + paramName + "=" + paramValue;
        return new BrowserWindowBuilder(this, url, LinkInterception.NONE, true);
    }

    /**
     * Starts building a window showing one of the react apps. It shows a loading animation until the
     * app is ready, since starting one takes seconds.
     *
     * @see #makeReactPanel(String, String, String, String, String, String)
     */
    public BrowserWindowBuilder newReactWindow(@NotNull String appPath,
                                               @NotNull String projectId,
                                               @Nullable String alignedFeatureId,
                                               @Nullable String formulaId,
                                               @Nullable String inchiKey,
                                               @Nullable String matchId) {
        String url = baseUrl.resolve(appPath) + makeParameters(projectId, alignedFeatureId, formulaId, inchiKey, matchId);
        return new BrowserWindowBuilder(this, url, LinkInterception.NONE, true);
    }

    /**
     * Starts building a window showing the given url. It shows a loading animation until the page is
     * there, since how long loading a page takes is not known beforehand.
     */
    public BrowserWindowBuilder newBrowserWindow(@NotNull URI url) {
        return newBrowserWindow(url.toString());
    }

    public BrowserWindowBuilder newBrowserWindow(@NotNull String url) {
        return new BrowserWindowBuilder(this, url, LinkInterception.NONE, true);
    }

    /**
     * Starts building a window showing the given html.
     * <p>
     * Such a window does not show a loading animation, since a data url is there immediately and the
     * animation would be bigger than the content of e.g. a single line of text.
     */
    public BrowserWindowBuilder newHtmlWindow(@NotNull String htmlContent) {
        return newHtmlWindow(htmlContent, Colors.isDarkTheme() ? CSS_DARK_RESOURCE : CSS_LIGHT_RESOURCE);
    }

    public BrowserWindowBuilder newHtmlWindow(@NotNull String htmlContent, @NotNull String cssResource) {
        return new BrowserWindowBuilder(this, toHtmlDataUrl(htmlContent, cssResource), LinkInterception.ALL, false);
    }

    public abstract void destroy();


    public static String makeParameters(@NotNull String projectId) {
        return THEME_REST_PARA + "&pid=" + projectId;
    }

    public static String makeParameters(@NotNull String projectId, @Nullable String alignedFeatureId,
                                           @Nullable String formulaId, @Nullable String inchiKey, @Nullable String matchId
    ) {
        StringBuilder params = new StringBuilder(makeParameters(projectId));
        if (Utils.notNullOrBlank(alignedFeatureId))
            params.append("&fid=").append(alignedFeatureId);
        if (Utils.notNullOrBlank(formulaId))
            params.append("&formulaId=").append(formulaId);
        if (Utils.notNullOrBlank(inchiKey))
            params.append("&inchikey=").append(inchiKey);
        if (Utils.notNullOrBlank(matchId))
            params.append("&matchid=").append(matchId);

        return params.toString();
    }
}
