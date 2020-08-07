/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.gui;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.SiriusCLIApplication;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
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
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import picocli.CommandLine;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@CommandLine.Command(name = "gui", aliases = {"GUI"}, description = "Starts the graphical user interface of SIRIUS", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class GuiAppOptions implements StandaloneTool<GuiAppOptions.Flow> {
    public static final String DONT_ASK_RECOMPUTE_KEY = "de.unijena.bioinf.sirius.computeDialog.writeSummaries.dontAskAgain";
    public static final String COMPOUND_BUFFER_KEY = "de.unijena.bioinf.sirius.gui.instanceBuffer";

    @CommandLine.Option(names = {"--compound-buffer"}, description = "Number of compounds that will be cached in Memory by the GUI. A larger buffer may improve loading times of views that display many results. A smaller buffer reduces the memory maximal consumption of the GUI.", defaultValue = "10")
    public void setInitialInstanceBuffer(int instanceBuffer) {
        PropertyManager.setProperty(COMPOUND_BUFFER_KEY, String.valueOf(instanceBuffer));
    }

    @Override
    public Flow makeWorkflow(RootOptions<?, ?, ?> rootOptions, ParameterConfig config) {
        return new Flow(rootOptions, config);

    }

    public static class Flow implements Workflow {
        private final PreprocessingJob<ProjectSpaceManager> preproJob;
        private final ParameterConfig config;


        private Flow(RootOptions<?, ?, ?> rootOptions, ParameterConfig config) {
            this.preproJob = (PreprocessingJob<ProjectSpaceManager>) rootOptions.makeDefaultPreprocessingJob();
            this.config = config;
        }

        @Override
        public void run() {
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
                    try {
                        ApplicationCore.DEFAULT_LOGGER.info("Saving properties file before termination.");
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().store();
                        Jobs.runInBackgroundAndLoad(MainFrame.MF, "Cancelling running jobs...", Jobs::cancelALL);
                        if (new QuestionDialog(MainFrame.MF,
                                "<html>Do you want to write summary files to the project-space before closing this project?<br>This may take some time for large projects. </html>").isSuccess()) {
                            ApplicationCore.DEFAULT_LOGGER.info("Writing Summaries to Project-Space before termination.");
                            Jobs.runInBackgroundAndLoad(MainFrame.MF, "Writing Summaries to Project-Space", true, new TinyBackgroundJJob<Boolean>() {
                                @Override //todo summary job with real loading screen
                                protected Boolean compute() throws Exception {
                                    MainFrame.MF.ps().updateSummaries(ProjectSpaceManager.defaultSummarizer());
                                    return true;
                                }
                            });
                        }
                        ApplicationCore.DEFAULT_LOGGER.info("Closing Project-Space");
                        Jobs.runInBackgroundAndLoad(MainFrame.MF, "Closing Project-Space", true, new TinyBackgroundJJob<Boolean>() {
                            @Override
                            protected Boolean compute() throws Exception {
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
            });

            Jobs.runInBackgroundAndLoad(MainFrame.MF, "Firing up SIRIUS...", new TinyBackgroundJJob<Boolean>() {
                @Override
                protected Boolean compute() throws Exception {
                    try {
                        int progress = 0;
                        int max = 7;
                        updateProgress(0, max, progress++);
                        ApplicationCore.DEFAULT_LOGGER.info("Configuring CDK InChIGeneratorFactory...");
                        updateProgress(0, max, progress++, "Configuring CDK InChIGeneratorFactory...");
                        InChIGeneratorFactory.getInstance();
                        ApplicationCore.DEFAULT_LOGGER.info("Initializing available DBs...");
                        updateProgress(0, max, progress++, "Initializing available DBs");
                        SearchableDatabases.getAvailableDatabases();
                        ApplicationCore.DEFAULT_LOGGER.info("Initializing Startup Project-Space...");
                        updateProgress(0, max, progress++, "Initializing Project-Space...");
                        // run prepro job. this jobs imports all existing data into the projectspace we use for the GUI session
                        final ProjectSpaceManager projectSpace = SiriusJobs.getGlobalJobManager().submitJob(preproJob).takeResult();
                        ApplicationCore.DEFAULT_LOGGER.info("GUI initialized, showing GUI..");
                        updateProgress(0, max, progress++, "Painting GUI...");
                        MainFrame.MF.decoradeMainFrameInstance((GuiProjectSpaceManager) projectSpace);

                        ApplicationCore.DEFAULT_LOGGER.info("Checking client version and webservice connection...");
                        updateProgress(0,max,progress++,"Checking Webservice connection...");
                        ConnectionMonitor.ConnetionCheck cc = MainFrame.MF.CONNECTION_MONITOR().checkConnection();
                        if (cc.isConnected()) {
                            @Nullable VersionsInfo versionsNumber = ApplicationCore.WEB_API.getVersionInfo();
                            ApplicationCore.DEFAULT_LOGGER.debug("FingerID response " + (versionsNumber != null ? String.valueOf(versionsNumber.toString()) : "NULL"));
                            if (versionsNumber != null) {
                                if (versionsNumber.expired()) {
                                    new UpdateDialog(MainFrame.MF, versionsNumber);
                                }
                                if (versionsNumber.hasNews()) {
                                    new NewsDialog(MainFrame.MF, versionsNumber.getNews());
                                }
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        new StacktraceDialog(MainFrame.MF, "Unexpected error!", e);
                        throw e;
                    }
                }
            });
            if (!PropertyManager.getBoolean(AboutDialog.PROPERTY_KEY, false))
                new AboutDialog(MainFrame.MF, true);
        }
    }
}
