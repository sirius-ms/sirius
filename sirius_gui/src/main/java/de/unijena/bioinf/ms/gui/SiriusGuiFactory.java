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

import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import org.jetbrains.annotations.NotNull;

/**
 * This is usually a singleton instance class that holds all active gui instances and there shared infrastructure
 */
public final class SiriusGuiFactory {
    private volatile NightSkyClient nightSkyClient;
    private volatile ConnectionMonitor connectionMonitor;


    public SiriusGuiFactory() {
        this(null, null);
    }
    public SiriusGuiFactory(NightSkyClient nightSkyClient, ConnectionMonitor connectionMonitor) {
        this.nightSkyClient = nightSkyClient;
        this.connectionMonitor = connectionMonitor;
    }

    public SiriusGui newGui(@NotNull String projectId){
        init();
        return new SiriusGui(projectId, nightSkyClient, connectionMonitor);
    }

    @Deprecated
    public SiriusGui newGui(@NotNull String projectId, @NotNull GuiProjectSpaceManager project){
        init();
        return new SiriusGui(projectId, project, nightSkyClient, connectionMonitor);
    }

    private void init(){
        if (nightSkyClient == null){
            synchronized (this){
                if (nightSkyClient == null)
                    nightSkyClient = new NightSkyClient();
            }
        }

        if (connectionMonitor == null){
            synchronized (this){
                if (connectionMonitor == null)
                    connectionMonitor = new ConnectionMonitor(nightSkyClient);
            }
        }
    }

    //todo nightsky create new GUI instance on project
    /*public void openNewProjectSpace(Path selFile) {
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
*/
    public void shutdowm(){
        connectionMonitor.close();
    }
}
