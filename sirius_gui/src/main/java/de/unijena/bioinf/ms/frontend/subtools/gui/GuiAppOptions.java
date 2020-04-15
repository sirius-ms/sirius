package de.unijena.bioinf.ms.frontend.subtools.gui;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.ServiceWorkflow;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.NewsDialog;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.UpdateDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

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

    public class Flow implements ServiceWorkflow {
        private final PreprocessingJob<ProjectSpaceManager> preproJob;
        private final ParameterConfig config;


        private Flow(RootOptions<?,?,?> rootOptions, ParameterConfig config) {
            this.preproJob = (PreprocessingJob<ProjectSpaceManager>) rootOptions.makeDefaultPreprocessingJob();
            this.config = config;
        }

        @Override
        public void run() {
            //todo minor: cancellation handling

            // run prepro job. this jobs imports all existing data into the projectspace we use for the GUI session
            final ProjectSpaceManager projectSpace = SiriusJobs.getGlobalJobManager().submitJob(preproJob).takeResult();


            // NOTE: we do not want to run ConfigJob here because we want to set
            // final config for experient if something will be computed and that is not the case here
            //todo maybe invalidate cache here!
//            final List<AddConfigsJob> configsJobs = new ArrayList<>(projectSpace.size());
//            ps.forEach(inst -> configsJobs.add(SiriusJobs.getGlobalJobManager().submitJob(new AddConfigsJob(config))));
//            configsJobs.forEach(JJob::takeResult);
//            configsJobs.clear();
            ApplicationCore.DEFAULT_LOGGER.info("Initializing available DBs");
            SearchableDatabases.getAvailableDatabases();
            //todo 3: init GUI with given project space.
            GuiUtils.initUI();
            ApplicationCore.DEFAULT_LOGGER.info("Swing parameters for GUI initialized");
            MainFrame.MF.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    try {
                        ApplicationCore.DEFAULT_LOGGER.info("Saving properties file before termination.");
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().store();
                        ApplicationCore.DEFAULT_LOGGER.info("Writing Summaries to Project-Space before termination.");
                        if (new QuestionDialog(MainFrame.MF,
                                "<html>Do you want to write summary files to the project-space before closing this project?<br>This may take some time for large projects. </html>").isSuccess()) {
                            Jobs.runInBackgroundAndLoad(MainFrame.MF, "Writing Summaries to Project-Space", true, new TinyBackgroundJJob<Boolean>() {
                                @Override //todo summary job with real loading screen
                                protected Boolean compute() throws Exception {
                                    projectSpace.updateSummaries(ProjectSpaceManager.defaultSummarizer());
                                    return true;
                                }
                            });
                        }
                        projectSpace.close();
                    } catch (IOException e) {
                        ApplicationCore.DEFAULT_LOGGER.error("Could not write summaries", e);
                    } finally {
                        MainFrame.CONNECTION_MONITOR.close();
                        System.exit(0);
                    }
                }
            });
            MainFrame.MF.setLocationRelativeTo(null); //init mainframe
            ApplicationCore.DEFAULT_LOGGER.info("GUI initialized, showing GUI..");
            MainFrame.MF.decoradeMainFrameInstance((GuiProjectSpaceManager) projectSpace);

            ApplicationCore.DEFAULT_LOGGER.info("Checking client version and webservice connection...");

            Jobs.runInBackground(() -> {
                ConnectionMonitor.ConnetionCheck cc = MainFrame.CONNECTION_MONITOR.checkConnection();
                if (cc.isConnected()) {
                    @Nullable VersionsInfo versionsNumber = ApplicationCore.WEB_API.getVersionInfo();
                    ApplicationCore.DEFAULT_LOGGER.debug("FingerID response " + (versionsNumber != null ? String.valueOf(versionsNumber.toString()) : "NULL"));
                    if (versionsNumber != null) {
                        if (versionsNumber.expired()) {
                            new UpdateDialog(MainFrame.MF, versionsNumber);
                        }
                        if (!versionsNumber.outdated()) {
                            MainFrame.MF.setFingerIDEnabled(true);
                        }
                        if (versionsNumber.hasNews()) {
                            new NewsDialog(MainFrame.MF, versionsNumber.getNews());
                        }
                    }
                }
            });
        }
    }
}
