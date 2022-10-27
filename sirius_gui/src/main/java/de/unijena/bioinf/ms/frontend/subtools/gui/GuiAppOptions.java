/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.frontend.subtools.gui;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.SiriusCLIApplication;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.splash.Splash;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastSubToolJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.*;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateFingerprintsTopK;
import de.unijena.bioinf.projectspace.fingerid.FBCandidatesTopK;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.webapi.rest.RestAPI;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import picocli.CommandLine;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

@CommandLine.Command(name = "gui", aliases = {"GUI"}, description = "Starts the graphical user interface of SIRIUS", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class GuiAppOptions implements StandaloneTool<GuiAppOptions.Flow> {
    public static final String DONT_ASK_CLOSE_KEY = "de.unijena.bioinf.sirius.mainframe.close.dontAskAgain";
    public static final String COMPOUND_BUFFER_KEY = "de.unijena.bioinf.sirius.gui.instanceBuffer";
    private final Splash splash;

    public GuiAppOptions(@Nullable Splash splash) {
        this.splash = splash;
    }

    @CommandLine.Option(names = {"--compound-buffer"}, description = "Number of compounds that will be cached in Memory by the GUI. A larger buffer may improve loading times of views that display many results. A smaller buffer reduces the memory maximal consumption of the GUI.", defaultValue = "10")
    public void setInitialInstanceBuffer(int instanceBuffer) {
        PropertyManager.setProperty(COMPOUND_BUFFER_KEY, String.valueOf(instanceBuffer));
    }

    @Override
    public Flow makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return new Flow(rootOptions, config);

    }

    public class Flow implements Workflow {
        private final PreprocessingJob<GuiProjectSpaceManager> preproJob;
        private final ParameterConfig config;


        private Flow(RootOptions<?,?, ?, ?> rootOptions, ParameterConfig config) {
            this.preproJob = (PreprocessingJob<GuiProjectSpaceManager>) rootOptions.makeDefaultPreprocessingJob();
            this.config = config;
        }

        @Override
        public void run() {
            // modify fingerid subtool so that it works with reduced GUI candidate list.
            FingerblastSubToolJob.formulaResultComponentsToClear.add(FBCandidatesTopK.class);
            FingerblastSubToolJob.formulaResultComponentsToClear.add(FBCandidateFingerprintsTopK.class);
            //todo minor: cancellation handling

            // NOTE: we do not want to run ConfigJob here because we want to set
            // final configs for experient if something will be computed and that is not the case here
            //todo maybe invalidate cache here!
            //todo 3: init GUI with given project space.
            GuiUtils.initUI();
            ApplicationCore.DEFAULT_LOGGER.info("Swing parameters for GUI initialized");
            MainFrame.MF.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    if (!Jobs.MANAGER().hasActiveJobs() || new QuestionDialog(MainFrame.MF,
                            "<html>Do you really want close SIRIUS?" +
                                    "<br> <b>There are still some Jobs running.</b> Running Jobs will be canceled when closing SIRIUS.</html>", DONT_ASK_CLOSE_KEY).isSuccess()) {
                        try {
                            ApplicationCore.DEFAULT_LOGGER.info("Saving properties file before termination.");
                            SiriusProperties.SIRIUS_PROPERTIES_FILE().store();
                            Jobs.runInBackgroundAndLoad(MainFrame.MF, "Cancelling running jobs...", Jobs::cancelAllRuns);

                            ApplicationCore.DEFAULT_LOGGER.info("Closing Project-Space");
                            Jobs.runInBackgroundAndLoad(MainFrame.MF, "Closing Project-Space", true, new TinyBackgroundJJob<Boolean>() {
                                @Override
                                protected Boolean compute() throws Exception {
                                    if (MainFrame.MF.ps() != null)
                                        MainFrame.MF.ps().close();
                                    return true;
                                }
                            });
                            Jobs.runInBackgroundAndLoad(MainFrame.MF, "Disconnecting from webservice...", SiriusCLIApplication::shutdownWebservice);
                        } finally {
                            MainFrame.MF.CONNECTION_MONITOR().close();
                            System.exit(0);
                        }
                    }
                }
            });

            // run prepro job. this jobs imports all existing data into the projectspace we use for the GUI session
            TinyBackgroundJJob<Boolean> j = new TinyBackgroundJJob<>() {
                @Override
                protected Boolean compute() throws Exception {
                    try {
                        int progress = 0;
                        int max = 8;
                        updateProgress(0, max, progress++, "Configuring CDK InChIGeneratorFactory...");
                        InChIGeneratorFactory.getInstance();
                        updateProgress(0, max, progress++, "Initializing available DBs");
                        SearchableDatabases.getAvailableDatabases();
                        updateProgress(0, max, progress++, "Initializing Project-Space...");
                        // run prepro job. this jobs imports all existing data into the projectspace we use for the GUI session
                        final ProjectSpaceManager<?> projectSpace = SiriusJobs.getGlobalJobManager().submitJob(preproJob).takeResult();
                        updateProgress(0, max, progress++, "Painting GUI...");
                        MainFrame.MF.decoradeMainFrameInstance((GuiProjectSpaceManager) projectSpace);
                        updateProgress(0, max, progress++, "Checking Webservice connection...");
                        ConnectionMonitor.ConnectionCheck cc = MainFrame.MF.CONNECTION_MONITOR().checkConnection();

                        if (cc.isConnected() || cc.hasOnlyWarning()) {
                            try {
                                ApplicationCore.DEFAULT_LOGGER.info("Checking for Update... ");
                                @Nullable VersionsInfo versionInfo = ApplicationCore.WEB_API.getVersionInfo(true);
                                if (versionInfo != null){
                                    ApplicationCore.DEFAULT_LOGGER.info("Latest Release: " + versionInfo.getLatestSiriusVersion() + " (Installed: " + ApplicationCore.VERSION() + ")");

                                    if ((!UpdateDialog.isDontAskMe()  && ApplicationCore.VERSION_OBJ().compareTo(versionInfo.getLatestSiriusVersion()) < 0) || versionInfo.expired()) {
                                        new UpdateDialog(MainFrame.MF, versionInfo);
                                    }
                                    if (versionInfo.hasNews()) {
                                        new NewsDialog(MainFrame.MF, versionInfo.getNews());
                                    }
                                }
                            } catch (IOException e) {
                                ApplicationCore.DEFAULT_LOGGER.error("Error when fetching update/release information form GitHub.", e);
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        new StacktraceDialog(MainFrame.MF, "Unexpected error!", e);
                        throw e;
                    }
                }
            };

            if (splash != null)
                j.addPropertyChangeListener(splash);

            Jobs.runInBackground(j).takeResult();

            if (splash != null) {
                splash.setVisible(false);
                splash.dispose();
            }

            if (!PropertyManager.getBoolean(AboutDialog.PROPERTY_KEY, false))
                new AboutDialog(MainFrame.MF, true);
        }
    }
}
