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
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.sse.DataEventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Represents an instance of the SIRIUS GUI and its context.
 * - GUI MainFrame
 * - Persistence layer
 * - Background Computations
 */
public class SiriusGui {

    static {
        GuiUtils.initUI();
    }
    private final String projectId;

    public String getProjectId() {
        return projectId;
    }

    private final NightSkyClient siriusClient;


    public NightSkyClient getSiriusClient() {
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

    public SiriusGui(@NotNull String projectId, @Nullable NightSkyClient nightSkyClient, @NotNull ConnectionMonitor connectionMonitor) {
        this(projectId, null, nightSkyClient, connectionMonitor);

    }

    public SiriusGui(@NotNull String projectId, @NotNull GuiProjectSpaceManager project, @Nullable NightSkyClient nightSkyClient, @NotNull ConnectionMonitor connectionMonitor) { //todo nighsky: change to nightsky api and project ID.
        this.projectId = projectId;
        this.connectionMonitor = connectionMonitor;
        siriusClient = nightSkyClient != null ? nightSkyClient : new NightSkyClient();
        siriusClient.enableEventListening(EnumSet.allOf(DataEventType.class));
        mainFrame = new MainFrame();
        mainFrame.decoradeMainFrame(this, project);
        //todo nighsky: check why JFX webview is only working for first instance...
        //todo nighsky: connect SSE connection to retrieve gui change states ???
    }

    public void shutdown(boolean closeProject){
        System.out.println("SHUTDOWN SIRIUS GUI");
        try {
            siriusClient.close();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error when closing NighSky client!", e);
        }
        mainFrame.setCloseProjectOnDispose(closeProject);
        mainFrame.dispose();
    }
    public void shutdown(){
        shutdown(true);
    }
}
