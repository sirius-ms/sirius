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
import org.cef.handler.CefLifeSpanHandler;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A reusable JPanel component that embeds a JCEF browser with proper lifecycle management.
 * This panel takes care of proper resource cleanup when it's no longer needed.
 */
@Slf4j
public class JCefBrowserPanel extends JPanel {
    private CefClient client;

    @Getter
    private CefBrowser browser;
    private boolean isDisposed = false;
    private final CountDownLatch browserCloseLatch = new CountDownLatch(1);
    private final boolean ownClient;
    private LinkInterception linkInterception = LinkInterception.NONE;

    private static final String CSS_LIGHT_RESOURCE_TEXT = "/sirius/style-light.css";
    private static final String CSS_DARK_RESOURCE_TEXT = "/sirius/style-dark.css";

    private static final String CSS_LIGHT_RESOURCE = "/js/styles.css";
    private static final String CSS_DARK_RESOURCE = "/js/styles-dark.css";

    public static JCefBrowserPanel makeHTMLTextPanel(String htmlText, SiriusGui browserProvider) {
        return makeHTMLTextPanel(htmlText, browserProvider,  Colors.BACKGROUND);
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
        this.ownClient = true;
        this.linkInterception = linkInterception;
        initialize(URI.create(siriusGui.getSiriusClient().getApiClient().getBasePath()).resolve(urlPath).toString());
    }


    /**
     * Creates a new browser panel using a provided CefClient.
     *
     * @param url       The URL to load
     * @param client    The CefClient to use
     * @param ownClient Whether this panel should dispose the client when it's disposed
     */
    public JCefBrowserPanel(String url, CefClient client, boolean ownClient) {
        this.client = client;
        this.ownClient = ownClient;
        initialize(url);
    }

    /**
     * Creates a new browser panel using a provided CefClient.
     *
     * @param url    The URL to load
     * @param client The CefClient to use
     */
    public JCefBrowserPanel(URI url, CefClient client, boolean ownClient) {
        this(url.toString(), client, ownClient);
    }

    /**
     * Creates a new browser panel using a provided CefClient.
     * This panel will not dispose the client when it's disposed.
     *
     * @param url    The URL to load
     * @param client The CefClient to use
     */
    public JCefBrowserPanel(String url, CefClient client) {
        this(url, client, false);
    }

    /**
     * Creates a new browser panel using a provided CefClient.
     * This panel will not dispose the client when it's disposed.
     *
     * @param url    The URL to load
     * @param client The CefClient to use
     */
    public JCefBrowserPanel(URI url, CefClient client) {
        this(url.toString(), client, false);
    }

    protected void initialize(String url) {
        ProxyManager.enforceGlobalProxySetting();
        setLayout(new BorderLayout());

        // Add life span handler to properly track browser closing
        CefLifeSpanHandler lifeSpanHandler = new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                super.onAfterCreated(browser);
            }

            @Override
            public boolean doClose(CefBrowser browser) {
                browser.setCloseAllowed();
                return false;
            }

            @Override
            public void onBeforeClose(CefBrowser browser) {
                browserCloseLatch.countDown();
            }
        };

        client.addLifeSpanHandler(lifeSpanHandler);
        setupLinkInterception();
        // OFFSCREEN rendering is mandatory since otherwise focussing is buggy
        browser = client.createBrowser(url, CefRendering.OFFSCREEN, false);
        Component browserUI = browser.getUIComponent();
        add(browserUI, BorderLayout.CENTER);
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
                                    GuiUtils.openURL(targetURL,null, true);
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

      //todo loading urls after initialize is not working right now.
//    /**
//     * Loads a new URL in the browser.
//     *
//     * @param url The URL to load
//     */
//
//    public void loadURL(String url) {
//        if (browser != null && !isDisposed) {
//            browser.loadURL(url);
//        }
//    }
//
//    /**
//     * Loads a new URL in the browser.
//     *
//     * @param url The URL to load
//     */
//    public void loadURL(URI url) {
//        loadURL(url.toString());
//    }

    /**
     * Executes JavaScript in the browser.
     *
     * @param javascript The JavaScript to execute
     */
    public void executeJavaScript(String javascript) {
        if (browser != null && !isDisposed) {
            browser.executeJavaScript(javascript, browser.getURL(), 0);
        }
    }

    public void updateSelectedFeature(@NotNull String alignedFeatureId){
        executeJavaScript(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID='%s')", alignedFeatureId));
    }

    public void updateSelectedFormulaCandidate(@NotNull String alignedFeatureId, @NotNull String formulaId){
        executeJavaScript(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID='%s', formulaID='%s')", alignedFeatureId, formulaId));
    }

    public void updateSelectedStructureCandidate(@NotNull String alignedFeatureId, @NotNull String formulaId , @NotNull String inchiKey){
        executeJavaScript(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID='%s', formulaID='%s', inchikey='%s')", alignedFeatureId, formulaId, inchiKey));
    }

    /**
     * Cleans up resources used by this panel.
     * This should be called when the panel is no longer needed.
     */
    public void cleanupResources() {
        if (isDisposed) return;

        if (browser != null) {
            try {
                // Stop any media playback
                browser.executeJavaScript(
                        "document.querySelectorAll('video,audio').forEach(m => {m.pause(); m.remove();});",
                        browser.getURL(), 0);

                // Load an empty page
                browser.loadURL("about:blank");

                // Wait briefly for the empty page to load and resources to be released
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }

                // Close the browser
                browser.close(true);

                // Wait for browser to close with timeout
                try {
                    browserCloseLatch.await(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                }

                // Remove the browser from the component hierarchy
                Component uiComponent = browser.getUIComponent();
                if (uiComponent != null && uiComponent.getParent() != null) {
                    uiComponent.getParent().remove(uiComponent);
                }

                browser = null;
            } catch (Exception e) {
                log.error("Error when cleaning up browser resources!", e);
            }
        }

        if (client != null && ownClient) {
            client.dispose();

            // If you're using SiriusGui's client tracking, you should remove the client
            // If this is part of your SiriusGui application, uncomment the following line:
            // SiriusGui.getInstance().removeClient(client);

            client = null;
        }

        isDisposed = true;
    }

    /**
     * Indicates whether this panel has been disposed.
     *
     * @return true if the panel has been disposed, false otherwise
     */
    public boolean isDisposed() {
        return isDisposed;
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
}