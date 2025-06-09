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

package de.unijena.bioinf.ms.gui.webView.jcefbrowser;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.webView.BrowserPanel;
import de.unijena.bioinf.ms.gui.webView.JCEFLinuxFixer;
import de.unijena.bioinf.ms.gui.webView.LinkInterception;
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

import java.awt.*;
import java.net.URI;

/**
 * A reusable JPanel component that embeds a JCEF browser with proper lifecycle management.
 * This panel takes care of proper resource cleanup when it's no longer necessary.
 */
@Slf4j
public class JCefBrowserPanel extends BrowserPanel {
    private CefClient client;

    @Getter
    private CefBrowser browser;

    JCefBrowserPanel(String url, CefClient client, LinkInterception linkInterception) {
        this.client = client;
        this.linkInterception = linkInterception;
        initialize(url);
    }

    protected void initialize(String url) {
        boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
        ProxyManager.enforceGlobalProxySetting();
        setLayout(new BorderLayout());

        setupLinkInterception();
        setupLoadingHandling();
        // OFFSCREEN rendering is mandatory since otherwise focussing is buggy
        browser = client.createBrowser(url, isLinux ? CefRendering.DEFAULT : CefRendering.OFFSCREEN, false);
        // very important to ensure that the JCEF process can be closed correctly without creating a memory leak
        browser.setCloseAllowed();
        // we create the browser instance synchronously because this is the only way to ensure the browser is fully
        // loaded before we do JS call to update the data ids to be shown
        browser.createImmediately();
        Component browserUI = browser.getUIComponent();
        // prevent drop handling for this panel since it is unnecessary and seems to crash the browser sometimes.
        browserUI.setDropTarget(null);

        // Apply the Linux scroll fix
        // This should be done before adding the component to the panel
        if (isLinux) {
            JCEFLinuxFixer.fixMousewheelScrolling(browserUI);
            JCEFLinuxFixer.setupPopupErrorListener(client, JCefBrowserPanel.this);
        }

        add(browserUI, BorderLayout.CENTER);
        setDropTarget(null);
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

    // we use replaceable calls to ensure that during fast selection changes,
    // we do not stack data loading tasks in the browser process.
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

    @Override
    public void submitDataUpdate(String javascript) {
        submitReplaceableDataUpdate(javascript);
    }

    /**
     * Cleans up resources used by this panel.
     * This should be called when the panel is no longer needed.
     */
    @Override
    public void cleanupResources() {
        if (client == null) return;
        client.dispose();
        browser = null;
        client = null;
    }
}