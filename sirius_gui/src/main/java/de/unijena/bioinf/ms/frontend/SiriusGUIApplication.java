package de.unijena.bioinf.ms.frontend;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.webapi.VersionsInfo;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.NewsDialog;
import de.unijena.bioinf.ms.gui.dialogs.UpdateDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.net.ProxyManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import org.jetbrains.annotations.Nullable;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusGUIApplication {

    public static void main(String[] args) {
        boolean isGui = false;
        for (String arg : args)
            if (arg.equalsIgnoreCase("--gui") || arg.equals("-u"))
                isGui = true;


        if (isGui) {
            //shut down hook to clean up when sirius is shutting down
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ApplicationCore.DEFAULT_LOGGER.info("GUI shut down hook: SIRIUS is cleaning up threads and shuts down...");
                MainFrame.CONNECTION_MONITOR.close();
                Jobs.cancelALL();//cancel all instances to quit
                try {
                    JobManager.shutDownNowAllInstances();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    ProxyManager.disconnect();
                }
            }));


            ApplicationCore.DEFAULT_LOGGER.info("Application Core started");

            final int cpuThreads = Integer.valueOf(PropertyManager.getProperty("de.unijena.bioinf.sirius.cpu.cores", null, "1"));
            SiriusJobs.setGlobalJobManager(new SwingJobManager(PropertyManager.getNumberOfThreads(), Math.min(cpuThreads, 3)));
            ApplicationCore.DEFAULT_LOGGER.info("Swing Job MANAGER initialized! " + SiriusJobs.getGlobalJobManager().getCPUThreads() + " : " + SiriusJobs.getGlobalJobManager().getIOThreads());

            if (ProxyManager.getProxyStrategy() == null) {
                SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty("de.unijena.bioinf.sirius.proxy", ProxyManager.DEFAULT_STRATEGY.name());
            }

            //parse args to set config
            final CLIRun cliRunCore = new CLIRun();
            cliRunCore.parseArgs(args);


            GuiUtils.initUI();
            ApplicationCore.DEFAULT_LOGGER.info("Swing parameters for GUI initialized");
            MainFrame.MF.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    try {
                        ApplicationCore.DEFAULT_LOGGER.info("Saving properties file before termination.");
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().store();
                    } finally {
                        System.exit(0);
                    }

                }
            });
            MainFrame.MF.setLocationRelativeTo(null);//init mainframe
            ApplicationCore.DEFAULT_LOGGER.info("GUI initialized, showing GUI..");
            MainFrame.MF.decoradeMainFrameInstance();

            ApplicationCore.DEFAULT_LOGGER.info("Checking client version and webservice connection...");
            Jobs.runInBackround(() -> {
                ConnectionMonitor.ConnetionCheck cc = MainFrame.CONNECTION_MONITOR.checkConnection();
                if (cc.isConnected()) {
                    @Nullable VersionsInfo versionsNumber = ApplicationCore.WEB_API.getVersionInfo();
                    ApplicationCore.DEFAULT_LOGGER.debug("FingerID response " + (versionsNumber != null ? String.valueOf(versionsNumber.toString()) : "NULL"));
                    if (versionsNumber != null) {
                        if (versionsNumber.expired()) {
                            new UpdateDialog(MainFrame.MF, versionsNumber);
                        }
                        if (!versionsNumber.outdated()) {
                            MainFrame.MF.getCsiFingerId().setEnabled(true);
                        }
                        if (versionsNumber.hasNews()) {
                            new NewsDialog(MainFrame.MF, versionsNumber.getNews());
                        }
                    }
                }
            });

            //todo find a good way to run tasks from the command line in the gui
            cliRunCore.compute();


        } else {
            //this is save because the only difference between cli and gui parameters is the --gui param
            SiriusCLIApplication.main(args);
        }

    }
}
