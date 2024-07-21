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

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.rest.ProxyManager;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.net.URI;


public class WebViewBrowserDialog extends JDialog {
    WebView webview;
    public WebViewBrowserDialog(Frame owner, String title, URI url) {
        super(owner, title,true);
        build(url);
    }

    public WebViewBrowserDialog(Dialog owner, String title, URI url) {
        super(owner, title,true);
        build(url);
    }

    private void build(URI url) {
        ProxyManager.enforceGlobalProxySetting();
        setLayout(new BorderLayout());
        JFXPanel jfxP = new JFXPanel();
        add(jfxP, BorderLayout.CENTER);

        setResizable(false);
        setMinimumSize(new Dimension(600, 800));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());

        Jobs.runJFXLater(() -> {
            webview = new WebView();
            webview.getEngine().load(url.toString());
            jfxP.setScene(new Scene(webview));
        });

        setVisible(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        MainFrame.cookieGuard.getCookieStore().removeAll();
    }
}