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

import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import org.jetbrains.annotations.NotNull;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Represents an instance of the SIRIUS GUI and its context.
 * - GUI MainFrame
 * - Persistence layer
 * - Background Computations
 */
public class SiriusGui {
    public static final String DONT_ASK_CLOSE_KEY = "de.unijena.bioinf.sirius.mainframe.close.dontAskAgain";

    static {
        GuiUtils.initUI();
    }

    private final MainFrame mainFrame;

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public SiriusGui(@NotNull GuiProjectSpaceManager project) { //todo nighsky: change to nightsky api and project ID.
        mainFrame = new MainFrame();
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (!Jobs.MANAGER().hasActiveJobs() || new QuestionDialog(mainFrame,
                        "<html>Do you really want close SIRIUS?" +
                                "<br> <b>There are still some Jobs running.</b> Running Jobs will be canceled when closing SIRIUS.</html>", DONT_ASK_CLOSE_KEY).isSuccess()) {
                    try {
                        ApplicationCore.DEFAULT_LOGGER.info("Saving properties file before termination.");
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().store();
                        Jobs.runInBackgroundAndLoad(mainFrame, "Cancelling running jobs...",  () -> mainFrame.getBackgroundRuns().cancelAllRuns());

                        ApplicationCore.DEFAULT_LOGGER.info("Closing Project-Space"); //todo nighsky: do not close project when closing mainframe
                        Jobs.runInBackgroundAndLoad(mainFrame, "Closing Project-Space", true, new TinyBackgroundJJob<Boolean>() {
                            @Override
                            protected Boolean compute() throws Exception {
                                if (mainFrame.ps() != null)
                                    mainFrame.ps().close();
                                return true;
                            }
                        });
                        Jobs.runInBackgroundAndLoad(mainFrame, "Disconnecting from webservice...", SiriusCLIApplication::shutdownWebservice);
                    } finally {
                        mainFrame.CONNECTION_MONITOR().close();
                        System.exit(0);
                    }
                }
            }
        });
        mainFrame.decoradeMainFrame(project);
    }

    public void shutdown(){
        mainFrame.dispose();
    }
}
