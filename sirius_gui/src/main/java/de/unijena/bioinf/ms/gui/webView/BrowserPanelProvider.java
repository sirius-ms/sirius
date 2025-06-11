package de.unijena.bioinf.ms.gui.webView;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.gui.configs.Colors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
    private final URI baseUrl;

    protected BrowserPanelProvider(@NotNull URI baseUrl) {
        this.baseUrl = baseUrl;
    }


    public BP makeReactPanel(@NotNull String appPath, @NotNull String projectId){
        return makeReactPanel(appPath, projectId, null, null, null, null);
    }

    public BP makeReactPanel(@NotNull String appPath,
                                      @NotNull String projectId,
                                      @Nullable String alignedFeatureId,
                                      @Nullable String formulaId,
                                      @Nullable String inchiKey,
                                      @Nullable String matchId){
        String url = baseUrl.resolve(appPath) +  makeParameters(projectId, alignedFeatureId, formulaId, inchiKey, matchId);
        return newBrowserPanel(url);
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
        String dataUrl = WebViewUtils.textToDataURL(htmlContent);

        // Load the data URL
        return newBrowserPanel(dataUrl, LinkInterception.ALL);
    }

    public abstract BP newBrowserPanel(@NotNull String fullUrlWithParameters, @NotNull LinkInterception linkInterception);

    public BP newBrowserPanel(@NotNull String fullUrlWithParameters){
        return newBrowserPanel(fullUrlWithParameters, LinkInterception.NONE);
    }

    public JDialog newBrowserPopUp(Frame owner, String title, URI url) {
        return new BrowserDialog(owner, title, newBrowserPanel(url.toString()));
    }

    public JDialog newBrowserPopUp(Dialog owner, String title, URI url) {
        return new BrowserDialog(owner, title, newBrowserPanel(url.toString()));
    }

    public abstract void destroy();


    protected static String makeParameters(@NotNull String projectId) {
        return THEME_REST_PARA + "&pid=" + projectId;
    }

    protected static String makeParameters(@NotNull String projectId, @Nullable String alignedFeatureId,
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
