/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.webView;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.rest.ProxyManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefRendering;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.validation.constraints.Null;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * A reusable JPanel component that embeds a JCEF browser with proper lifecycle management.
 * This panel takes care of proper resource cleanup when it's no longer needed.
 */
@Slf4j
public class JCefBrowserPanel extends JPanel {
    private CefClient client;

    @Getter
    private CefBrowser browser;
    private LinkInterception linkInterception = LinkInterception.NONE;

    private static final String CSS_LIGHT_RESOURCE_TEXT = "/sirius/style-light.css";
    private static final String CSS_DARK_RESOURCE_TEXT = "/sirius/style-dark.css";

    private static final String CSS_LIGHT_RESOURCE = "/js/styles.css";
    private static final String CSS_DARK_RESOURCE = "/js/styles-dark.css";

    protected static String THEME_REST_PARA = "?theme=" + (Colors.isDarkTheme() ? "dark" : "light");

    protected static String makeParameters(@NotNull String projectId) {
        return THEME_REST_PARA + "&pid=" + projectId;
    }

    protected static String makeParameters(@NotNull String projectId, @Nullable String alignedFeatureId,
                                           @Nullable String formulaId, @Nullable String inchiKey, @Nullable String matchId,@Nullable String smiles
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
        if (Utils.notNullOrBlank(matchId))
            params.append("&smiles=").append(matchId);

        return params.toString();
    }

    public static JCefBrowserPanel makeHTMLTextPanel(String htmlText, SiriusGui browserProvider) {
        return makeHTMLTextPanel(htmlText, browserProvider, Colors.BACKGROUND);
    }

    public static JCefBrowserPanel makeHTMLTextPanel(String htmlText, SiriusGui browserProvider, Color background) {
        final StringBuilder buf = new StringBuilder();
        try (final BufferedReader br = FileUtils.ensureBuffering(new InputStreamReader(JCefBrowserPanel.class.getResourceAsStream("/sirius/text.html")))) {
            String line;
            while ((line = br.readLine()) != null) buf.append(line).append('\n');
            String htmlContent = buf.toString().replace("#BACKGROUND#", "#" + Integer.toHexString(background.getRGB()).substring(2)).replace("#TEXT#", htmlText);
            return makeHTMLPanel(htmlContent, Colors.isDarkTheme() ? CSS_DARK_RESOURCE_TEXT : CSS_LIGHT_RESOURCE_TEXT, browserProvider);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JCefBrowserPanel makeHTMLPanel(String htmlContent, SiriusGui browserProvider) {
        return makeHTMLPanel(htmlContent, Colors.isDarkTheme() ? CSS_DARK_RESOURCE : CSS_LIGHT_RESOURCE, browserProvider);
    }

    public static JCefBrowserPanel makeHTMLPanel(String htmlContent, String cssResource, SiriusGui browserProvider) {
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
        return new JCefBrowserPanel(dataUrl, browserProvider, LinkInterception.ALL);
    }

    /**
     * Creates a new browser panel using a provided CefClient.
     */
    public JCefBrowserPanel(String urlPath, SiriusGui siriusGui) {
        this(urlPath, siriusGui, LinkInterception.NONE);
    }

    public JCefBrowserPanel(String urlPath, SiriusGui siriusGui, LinkInterception linkInterception) {
        this.client = siriusGui.newClient();
        this.linkInterception = linkInterception;
        initialize(URI.create(siriusGui.getSiriusClient().getApiClient().getBasePath()).resolve(urlPath).toString());
    }


    /**
     * Creates a new browser panel using a provided CefClient.
     *
     * @param url       The URL to load
     * @param client    The CefClient to use
     */
    public JCefBrowserPanel(String url, CefClient client) {
        this.client = client;
        initialize(url);
    }

    /**
     * Creates a new browser panel using a provided CefClient.
     *
     * @param url    The URL to load
     * @param client The CefClient to use
     */
    public JCefBrowserPanel(URI url, CefClient client) {
        this(url.toString(), client);
    }

    protected void initialize(String url) {
        ProxyManager.enforceGlobalProxySetting();
        setLayout(new BorderLayout());

        setupLinkInterception();
        setupLoadingHandling();
        // OFFSCREEN rendering is mandatory since otherwise focussing is buggy
        browser = client.createBrowser(url, CefRendering.OFFSCREEN, false);
        // very important to ensure that the JCEF process can be closed correctly without creating a memory leak
        browser.setCloseAllowed();
        // we create the browser instance synchronously because this is the only way to ensure the browser is fully
        // loaded before we do JS call to update the data ids to be shown
        browser.createImmediately();
        Component browserUI = browser.getUIComponent();

        // Apply the Linux scroll fix
        // This should be done before adding the component to the panel
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            MouseWheelFix.apply(browserUI);
            client.addDialogHandler((dialogBrowser, mode, title, defaultPath, acceptFilters, callback) -> {
                // Native file dialogs don't work on linux, see
                // https://github.com/chromiumembedded/cef/blob/master/libcef/browser/file_dialog_manager.cc#L405
                // https://github.com/JetBrains/jcef/blob/dev/native/context.cpp#L211
                //
                // Implementing a java dialog also doesn't work, results in error "The request is not allowed by the user agent or the platform in the current context"
                // Maybe can be fixed with some chromium flags to allow local system writing
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(JCefBrowserPanel.this, "Native file dialogs from this view are not available on Linux.", null, JOptionPane.ERROR_MESSAGE));
                callback.Cancel();
                return true;
            });
        }

        add(browserUI, BorderLayout.CENTER);
    }

    private void setupLoadingHandling() {
        // Add a load handler to detect when a page is fully loaded
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                if (!isLoading) {
                    if (browser.getFocusedFrame().isMain())
                        executeReplaceableDataUpdate();
                }
            }
        });
    }

    // Add this to your JCefBrowserPanel class
    private void setupLinkInterception() {
        if (linkInterception != LinkInterception.NONE) {
            client.addRequestHandler(new CefRequestHandlerAdapter() {
                @Override
                public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                    // Only intercept user-initiated navigation (clicks)
                    if (user_gesture) {
                        String url = request.getURL();
                        // Ignore about:blank and similar
                        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                            // Open URL in system browser
                            try {
                                URI targetURL = URI.create(url);
                                URI currentURL = browser.getURL() != null ? URI.create(browser.getURL()) : null;
                                if (linkInterception == LinkInterception.ALL || currentURL == null || !currentURL.getHost().equals(targetURL.getHost())) {
                                    GuiUtils.openURL(targetURL, null, true);
                                    // Return true to cancel the navigation in the embedded browser
                                    return true;
                                }
                            } catch (Exception e) {
                                log.error("Error when loading external link!", e);
                            }
                        }
                    }
                    // Let the navigation happen in the embedded browser
                    return false;
                }
            });
        }
    }

    /**
     * Executes JavaScript in the browser.
     *
     * @param javascript The JavaScript to execute
     */
    public void executeJavaScript(String javascript) {
        if (browser != null) {
            browser.executeJavaScript(javascript, browser.getURL(), 0);
        }
    }

    //we use replaceable calls to ensure that during fast selection changes we do not stack data loading tasks
    //browser process.
    private final Object dataUpdateLock = new Object();
    private Runnable replaceableDataUpdate = null;


    public void executeReplaceableDataUpdate() {
        synchronized (dataUpdateLock) {
            if (replaceableDataUpdate != null) {
                replaceableDataUpdate.run();
                replaceableDataUpdate = null;
            }
        }
    }

    public void submitReplaceableDataUpdate(String javascript) {
        synchronized (dataUpdateLock) {
            replaceableDataUpdate = () -> executeJavaScript(javascript);
            if (!browser.isLoading())
                executeReplaceableDataUpdate();
        }
    }

    public void updateSelectedFeature(@Nullable String alignedFeatureId) {
        submitReplaceableDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s)", parseNullable(alignedFeatureId)));
    }

    public void updateSelectedFeatureSketcher(@Nullable String alignedFeatureId, @Nullable String smiles){
        submitReplaceableDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s,undefined,undefined,undefined, smiles=%s)", parseNullable(alignedFeatureId), parseNullable(smiles)));
    }

    public void updateSelectedFormulaCandidate(@Nullable String alignedFeatureId, @Nullable String formulaId) {
        submitReplaceableDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s, formulaID=%s)", parseNullable(alignedFeatureId), parseNullable(formulaId)));
    }

    public void updateSelectedStructureCandidate(@Nullable String alignedFeatureId, @Nullable String formulaId, @Nullable String inchiKey) {
        submitReplaceableDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s, formulaID=%s, inchikey=%s)", parseNullable(alignedFeatureId), parseNullable(formulaId), parseNullable(inchiKey)));
    }

    public void updateSelectedSpectralMatch(@Nullable String alignedFeatureId, @Nullable String matchId) {
        submitReplaceableDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s, undefined, undefined, matchid=%s)", parseNullable(alignedFeatureId), parseNullable(matchId)));
    }

    private static String parseNullable(@Nullable String s) {
        return s == null || s.isBlank() ? "null" : ("'" + s + "'");
    }

    /**
     * Cleans up resources used by this panel.
     * This should be called when the panel is no longer needed.
     */
    public void cleanupResources() {
        if (client == null) return;
        client.dispose();
        browser = null;
        client = null;
    }

    /**
     * Called automatically when the component is being removed from the parent container.
     * This is the proper Swing way to clean up resources when a component is no longer displayed.
     */
    @Override
    public void removeNotify() {
        // Clean up resources before the component is removed
        cleanupResources();
        // Call the superclass implementation to complete normal component removal
        super.removeNotify();
    }

    // Some arbitrary size numbers to fix JSplitPane divider not moving
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(500,300);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(50,20);
    }
}