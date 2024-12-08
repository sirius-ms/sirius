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

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.projectspace.GuiProjectManager;
import io.sirius.ms.sdk.SiriusClient;
import lombok.Getter;
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

    @Getter
    private final GuiProperties properties;
    @Getter
    private final ConnectionMonitor connectionMonitor;
    @Getter
    private final MainFrame mainFrame;
    @Getter
    private GuiProjectManager projectManager;

    private SiriusClient siriusClient;

    public SiriusClient getSiriusClient() {
        if (siriusClient == null)
            throw new IllegalStateException("Gui instance that is using this Client has already been closed!");
        return siriusClient;
    }

    public SiriusGui(@NotNull String projectId, @NotNull SiriusClient siriusClient, @NotNull ConnectionMonitor connectionMonitor) {
        this.connectionMonitor = connectionMonitor;
        this.siriusClient = siriusClient;
        properties = new GuiProperties();
        projectManager = new GuiProjectManager(projectId, this.siriusClient, properties, this);
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

    public void acceptSiriusClient(BiConsumer<SiriusClient, String> doWithProject) {
        doWithProject.accept(getSiriusClient(), projectManager.getProjectId());
    }

    public <R> R applySiriusClient(BiFunction<SiriusClient, String, R> doWithProject) {
        return doWithProject.apply(getSiriusClient(), projectManager.getProjectId());
    }
}
