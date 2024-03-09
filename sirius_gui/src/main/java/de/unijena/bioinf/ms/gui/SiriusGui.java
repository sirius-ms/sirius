/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import de.unijena.bioinf.projectspace.GuiProjectManager;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Represents an instance of the SIRIUS GUI and its context.
 * - GUI MainFrame
 * - Persistence layer
 * - Background Computations
 */
public class SiriusGui {

    static {
        GuiUtils.initUI();
        //debug console
//        WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId) -> {
//            System.err.println("WEBVIEW: " + message + "[at " + lineNumber + "]");
//        });
    }

    private NightSkyClient siriusClient;

    public NightSkyClient getSiriusClient() {
        if (siriusClient == null)
            throw new IllegalStateException("Gui instance that is using this Client has already been closed!");
        return siriusClient;
    }

    private final ConnectionMonitor connectionMonitor;

    public ConnectionMonitor getConnectionMonitor() {
        return connectionMonitor;
    }

    private final MainFrame mainFrame;

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    private GuiProjectManager projectManager;

    public GuiProjectManager getProjectManager() {
        return projectManager;
    }

    public SiriusGui(@NotNull String projectId, @NotNull NightSkyClient nightSkyClient, @NotNull ConnectionMonitor connectionMonitor) {
        this.connectionMonitor = connectionMonitor;
        siriusClient = nightSkyClient;
        projectManager = new GuiProjectManager(projectId, siriusClient);
        mainFrame = new MainFrame(this);
        mainFrame.decoradeMainFrame();
        //todo nightsky: GUI standablone mode with external SIRIUS Service
    }

    public void close() {
        boolean closed = getSiriusClient().gui().closeGui(getProjectManager().getProjectId(), true);
        if (!closed)
            shutdown();
    }

    public void shutdown() {
        mainFrame.dispose();
    }

    public void acceptSiriusClient(BiConsumer<NightSkyClient, String> doWithProject) {
        doWithProject.accept(getSiriusClient(), projectManager.getProjectId());
    }

    public <R> R applySiriusClient(BiFunction<NightSkyClient, String, R> doWithProject) {
        return doWithProject.apply(getSiriusClient(), projectManager.getProjectId());
    }
}