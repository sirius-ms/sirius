package de.unijena.bioinf.ms.cli;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.webapi.VersionsInfo;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.net.ProxyManager;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.core.SiriusProperties;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.dialogs.NewsDialog;
import de.unijena.bioinf.sirius.gui.dialogs.UpdateDialog;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.net.ConnectionMonitor;
import de.unijena.bioinf.sirius.gui.utils.GuiUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusGUIApplication {

    public static void main(String[] args) {
        boolean isGui = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--gui") || arg.equals("-u"))
                isGui = true;
        }
        if (isGui) {
            //shut down hook to clean up when sirius is shutting down
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ApplicationCore.DEFAULT_LOGGER.info("GUI shut down hook: SIRIUS is cleaning up threads and shuts down...");
                MainFrame.CONECTION_MONITOR.close();
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
            final int cpuThreads = Integer.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.cores", "1"));
            SiriusJobs.setGlobalJobManager(new SwingJobManager(PropertyManager.getNumberOfThreads(), Math.min(cpuThreads, 3)));
            ApplicationCore.DEFAULT_LOGGER.info("Swing Job MANAGER initialized! " + SiriusJobs.getGlobalJobManager().getCPUThreads() + " : " + SiriusJobs.getGlobalJobManager().getIOThreads());

            if (ProxyManager.getProxyStrategy() == null) {
                SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty("de.unijena.bioinf.sirius.proxy", ProxyManager.DEFAULT_STRATEGY.name());
            }


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
                ConnectionMonitor.ConnetionCheck cc = MainFrame.CONECTION_MONITOR.checkConnection();
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
        } else {
            //this os save because the only difference between cli and gui parameters is the --gui param
            SiriusCLIApplication.main(args);
        }

    }
}
