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


package de.unijena.bioinf.ms.gui;

import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.webView.BrowserPanelProvider;
import de.unijena.bioinf.ms.gui.webView.jxbrowser.JxBrowserPanelProvider;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sse.DataEventType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.EnumSet;

/**
 * This is usually a singleton instance class that holds all active gui instances and there shared infrastructure
 */
public final class SiriusGuiFactory {
    private volatile SiriusClient siriusClient;
    private volatile ConnectionMonitor connectionMonitor;
    private volatile BrowserPanelProvider<?> browserPanelProvider;

    public SiriusGuiFactory(int port) {
        this(new SiriusClient(port), null, null);
    }

    public SiriusGuiFactory(SiriusClient siriusClient, ConnectionMonitor connectionMonitor, BrowserPanelProvider<?> browserPanelProvider) {
        this.siriusClient = siriusClient;
        this.connectionMonitor = connectionMonitor;
        this.browserPanelProvider = browserPanelProvider;
    }

    public SiriusGui newGui(@NotNull String projectId) {
        init();
        return new SiriusGui(projectId, siriusClient, connectionMonitor, browserPanelProvider);
    }

    private void init() {
        if (siriusClient == null) {
            synchronized (this) {
                if (siriusClient == null)
                    siriusClient = new SiriusClient(8080); //try 8080 as default
            }
        }
        siriusClient.enableEventListening(EnumSet.allOf(DataEventType.class));


        if (connectionMonitor == null) {
            synchronized (this) {
                if (connectionMonitor == null)
                    connectionMonitor = new ConnectionMonitor(siriusClient);
            }
        }

        if (browserPanelProvider == null) {
            synchronized (this) {
                if (browserPanelProvider == null)
                    browserPanelProvider = new JxBrowserPanelProvider(URI.create(siriusClient.getApiClient().getBasePath()));
            }
        }
    }

    public void shutdowm() {
        try {
            if (connectionMonitor != null)
                connectionMonitor.close();
            if (siriusClient != null)
                siriusClient.close();
            if (browserPanelProvider != null)
                browserPanelProvider.destroy();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error when closing NighSky client!", e);
        }
    }
}
