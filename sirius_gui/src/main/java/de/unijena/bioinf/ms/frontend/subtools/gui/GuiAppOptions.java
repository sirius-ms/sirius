package de.unijena.bioinf.ms.frontend.subtools.gui;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.SingletonTool;
import de.unijena.bioinf.ms.frontend.workflow.ServiceWorkflow;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

@CommandLine.Command(name = "gui", aliases = {"GUI"}, description = "Starts the graphical user interface of SIRIUS", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class GuiAppOptions implements SingletonTool {
    @Override
    public Workflow makeSingletonWorkflow(PreprocessingJob preproJob, ProjectSpaceManager projectSpace, ParameterConfig config) {
        return new Flow(preproJob, projectSpace, config);
    }

    class Flow implements ServiceWorkflow {
        private final PreprocessingJob preproJob;
        private final ParameterConfig config;
        private final ProjectSpaceManager projectSpace;


        public Flow(PreprocessingJob preproJob, ProjectSpaceManager projectSpace, ParameterConfig config) {
            this.preproJob = preproJob;
            this.projectSpace = projectSpace;
            this.config = config;
        }

        @Override
        public void run() {
//todo minor: cancellation handling

            // run prepro job. this jobs imports all existing data into the projectspace we use for the GUI session
            SiriusJobs.getGlobalJobManager().submitJob(preproJob).takeResult();


            // NOTE: we do not want to run ConfigJob here because we want to set
            // final config for experient if something will be computed and that is not the case here
            //todo maybe invalidate cache here!
//            final List<AddConfigsJob> configsJobs = new ArrayList<>(projectSpace.size());
//            ps.forEach(inst -> configsJobs.add(SiriusJobs.getGlobalJobManager().submitJob(new AddConfigsJob(config))));
//            configsJobs.forEach(JJob::takeResult);
//            configsJobs.clear();

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
                        projectSpace.updateSummaries(ProjectSpaceManager.defaultSummarizer());
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
            MainFrame.MF.decoradeMainFrameInstance(projectSpace);

            ApplicationCore.DEFAULT_LOGGER.info("Checking client version and webservice connection...");

            System.out.println("DEBUG and reimplement Connection Check");
            //todo reenable
            /*Jobs.runInBackround(() -> {
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
            });*/
        }
    }
}
