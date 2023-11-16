/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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

package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final NightSkyClient sirius;

    public NightSkyClient getSirius() {
        return sirius;
    }

    private final MainFrame mainFrame;

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public SiriusGui(@NotNull GuiProjectSpaceManager project) { //todo nighsky: change to nightsky api and project ID.
        this(project, null, null);
    }
    public SiriusGui(@NotNull GuiProjectSpaceManager project, @Nullable NightSkyClient nightSkyClient, @Nullable ConnectionMonitor connectionMonitor) { //todo nighsky: change to nightsky api and project ID.
        sirius = nightSkyClient != null ? nightSkyClient : new NightSkyClient();
        mainFrame = new MainFrame(sirius, connectionMonitor != null ? connectionMonitor : new ConnectionMonitor(sirius));
        mainFrame.decoradeMainFrame(project);
        //todo nighsky: check why JFX webview is only working for first instance...
    }

    public void shutdown(boolean closeProject){
        mainFrame.setCloseProjectOnDispose(closeProject);
        mainFrame.dispose();
    }
    public void shutdown(){
        shutdown(true);
    }
}
