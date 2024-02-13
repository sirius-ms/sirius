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
import de.unijena.bioinf.sse.DataEventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;
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

    public SiriusGui(@NotNull String projectId, @Nullable NightSkyClient nightSkyClient, @NotNull ConnectionMonitor connectionMonitor) { //todo nighsky: change to nightsky api and project ID.
//        this.projectId = projectId;
        this.connectionMonitor = connectionMonitor;
        siriusClient = nightSkyClient != null ? nightSkyClient : new NightSkyClient();
        siriusClient.enableEventListening(EnumSet.allOf(DataEventType.class));
        projectManager = new GuiProjectManager(projectId, siriusClient);
        mainFrame = new MainFrame(this);
        mainFrame.decoradeMainFrame();
        //todo nightsky: connect SSE connection to retrieve gui change states ???
        //todo nightsky: GUI standablone mode with external SIRIUS Service
    }

    public void shutdown() {
        System.out.println("SHUTDOWN SIRIUS GUI");
        try {
            siriusClient.close();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error when closing NighSky client!", e);
        }
        mainFrame.dispose();
    }

    public void acceptSiriusClient(BiConsumer<NightSkyClient, String> doWithProject) {
        doWithProject.accept(getSiriusClient(), projectManager.getProjectId());
    }

    public <R> R applySiriusClient(BiFunction<NightSkyClient, String, R> doWithProject) {
        return doWithProject.apply(getSiriusClient(), projectManager.getProjectId());
    }


    //todo what is still needed from here
    /*
    * //todo nightsky create new GUI instance on project
    public void openNewProjectSpace(Path selFile) {
        changeProject(() -> new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(selFile));
    }

    public void createNewProjectSpace(Path selFile) {
        changeProject(() -> new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createNewProjectSpace(selFile));
    }

    protected void changeProject(IOFunctions.IOSupplier<SiriusProjectSpace> makeSpace) {
        final BasicEventList<InstanceBean> psList = compoundBaseList;
        final AtomicBoolean compatible = new AtomicBoolean(true);
        backgroundRuns.setProject(Jobs.runInBackgroundAndLoad(this, "Opening new Project...", () -> {
            GuiProjectSpaceManager old = ps();
            try {
                final SiriusProjectSpace ps = makeSpace.get();
                compatible.set(InstanceImporter.checkDataCompatibility(ps, NetUtils.checkThreadInterrupt(Thread.currentThread())) == null);
                backgroundRuns.cancelAllRuns();
                final GuiProjectSpaceManager gps = new GuiProjectSpaceManager(ps, psList, PropertyManager.getInteger(GuiAppOptions.COMPOUND_BUFFER_KEY, 10));
                Jobs.runEDTAndWaitLazy(() -> setTitlePath(gps.projectSpace().getLocation().toString()));

                gps.projectSpace().addProjectSpaceListener(event -> {
                    if (event.equals(ProjectSpaceEvent.LOCATION_CHANGED))
                        Jobs.runEDTAndWaitLazy(() -> setTitlePath(gps.projectSpace().getLocation().toString()));
                });
                return gps;
            } finally {
                old.close();
            }
        }).getResult());

        if (ps() == null) {
            try {
                LoggerFactory.getLogger(getClass()).warn("Error when changing project-space. Falling back to tmp project-space");
                createNewProjectSpace(ProjectSpaceIO.createTmpProjectSpaceLocation());
            } catch (IOException e) {
                new StacktraceDialog(this, "Cannot recreate a valid project-space due to: " + e.getMessage() + "'.  SIRIUS will not work properly without valid project-space. Please restart SIRIUS.", e);
            }
        }
        if (!compatible.get())
            if (new QuestionDialog(this, "<html><body>" +
                    "The opened project-space contains results based on an outdated fingerprint version.<br><br>" +
                    "You can either convert the project to the new fingerprint version and <b>lose all fingerprint related results</b> (e.g. CSI:FingerID an CANOPUS),<br>" +
                    "or you stay with the old fingerprint version but without being able to execute any fingerprint related computations (e.g. for data visualization).<br><br>" +
                    "Do you wish to convert and lose all fingerprint related results?" +
                    "</body></html>").isSuccess())
                ps().updateFingerprintData(this);
    }
    * */

    /*protected void copy(Path newlocation, boolean switchLocation, Frame popupOwner) {
        final String header = switchLocation ? "Saving Project to" : "Saving a Copy to";
        final IOException ex = Jobs.runInBackgroundAndLoad(popupOwner, header + " '" + newlocation.toString() + "'...", () -> {
            synchronized (GuiProjectSpaceManager.this) {
                try {
                    siriusClient.projects().
                            ProjectSpaceIO.copyProject(projectSpace(), newlocation, switchLocation);
                    return null;
                } catch (IOException e) {
                    return e;
                }
            }
        }).getResult();

        if (ex != null)
            new ExceptionDialog(popupOwner, ex.getMessage());
    }



    *//**
     * Saves (copy) the project to a new location. New location is active
     *
     * @param newlocation The path where the project will be saved
     * @throws IOException Thrown if writing of archive fails
     *//*
    public void saveAs(Path newlocation, Frame popupOwner) {
        copy(newlocation, true, popupOwner);
    }

    *//**
     * Saves a copy of the project to a new location. Original location will be the active project.
     *
     * @param copyLocation The path where the project will be saved
     *//*
    public void saveCopy(Path copyLocation, Frame popupOwner) {
        copy(copyLocation, false, popupOwner);
    }*/

}
