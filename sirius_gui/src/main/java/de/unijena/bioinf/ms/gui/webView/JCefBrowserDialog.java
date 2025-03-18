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

import de.unijena.bioinf.rest.ProxyManager;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

/**
 * A dialog that embeds a JCEF browser component.
 * Resource cleanup is handled automatically through JCefBrowserPanel's removeNotify method.
 */
public class JCefBrowserDialog extends JDialog {
    private final JCefBrowserPanel browserPanel;

    /**
     * Creates a browser dialog with the given title and URL.
     *
     * @param owner The owner frame
     * @param title The dialog title
     * @param url The URL to load in the browser
     * @param client The CefClient to use
     */
    public JCefBrowserDialog(Frame owner, String title, URI url, CefClient client) {
        super(owner, title, true);
        ProxyManager.enforceGlobalProxySetting();

        // Setup dialog
        setLayout(new BorderLayout());
        setResizable(false);
        setMinimumSize(new Dimension(600, 800));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Create the browser panel and make it own its client (will dispose the client when closed)
        browserPanel = new JCefBrowserPanel(url, client, true);
        add(browserPanel, BorderLayout.CENTER);

        // Show dialog
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    /**
     * Creates a browser dialog with the given title and URL.
     *
     * @param owner The owner dialog
     * @param title The dialog title
     * @param url The URL to load in the browser
     * @param client The CefClient to use
     */
    public JCefBrowserDialog(Dialog owner, String title, URI url, CefClient client) {
        super(owner, title, true);
        ProxyManager.enforceGlobalProxySetting();

        // Setup dialog
        setLayout(new BorderLayout());
        setResizable(false);
        setMinimumSize(new Dimension(600, 800));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Create the browser panel and make it own its client (will dispose the client when closed)
        browserPanel = new JCefBrowserPanel(url, client, true);
        add(browserPanel, BorderLayout.CENTER);

        // Show dialog
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    /**
     * Gets the underlying browser panel.
     *
     * @return The JCefBrowserPanel instance
     */
    public JCefBrowserPanel getBrowserPanel() {
        return browserPanel;
    }

    /**
     * Gets the underlying CefBrowser instance.
     *
     * @return The CefBrowser instance
     */
    public CefBrowser getBrowser() {
        return browserPanel.getBrowser();
    }

    /**
     * Loads a new URL in the browser.
     *
     * @param url The URL to load
     */
    public void loadURL(String url) {
        browserPanel.loadURL(url);
    }

    /**
     * Loads a new URL in the browser.
     *
     * @param url The URL to load
     */
    public void loadURL(URI url) {
        browserPanel.loadURL(url);
    }

    /**
     * Executes JavaScript in the browser.
     *
     * @param javascript The JavaScript to execute
     */
    public void executeJavaScript(String javascript) {
        browserPanel.executeJavaScript(javascript);
    }

    // No need for custom dispose() or window listeners!
    // The cleanup is automatically handled by the browserPanel's removeNotify() method
    // when the dialog is disposed or closed
}