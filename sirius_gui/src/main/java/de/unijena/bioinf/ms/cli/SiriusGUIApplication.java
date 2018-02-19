package de.unijena.bioinf.ms.cli;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.utils.GuiUtils;
import de.unijena.bioinf.sirius.net.ProxyManager;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusGUIApplication {

    public static void main(String[] args) {
        final ZodiacCLI<SiriusGUIOptions> cli = new ZodiacCLI<>();
        cli.parseArgs(args, SiriusGUIOptions.class);

        if (cli.options.isGUI()) {
            final int cpuThreads = Integer.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.cores", "1"));
            SiriusJobs.setGlobalJobManager(new SwingJobManager(PropertyManager.getNumberOfThreads(), Math.min(cpuThreads, 3)));
            FingeridCLI.DEFAULT_LOGGER.info("Swing Job MANAGER initialized! " + SiriusJobs.getGlobalJobManager().getCPUThreads() + " : " + SiriusJobs.getGlobalJobManager().getIOThreads());

            if (ProxyManager.getProxyStrategy() == null) {
                ApplicationCore.SIRIUS_PROPERTIES_FILE.setAndStoreProperty("de.unijena.bioinf.sirius.proxy", ProxyManager.DEFAULT_STRATEGY.name());
            }

            GuiUtils.initUI();
            FingeridCLI.DEFAULT_LOGGER.info("Swing parameters for GUI initialized");
            MainFrame.MF.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    try {
                        FingeridCLI.DEFAULT_LOGGER.info("Saving properties file before termination.");
                        ApplicationCore.SIRIUS_PROPERTIES_FILE.store();
                    } finally {
                        try {
                            Jobs.cancelALL();//cancel all instances to quit
                            JobManager.shutDownAllInstances();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            System.exit(0);
                        }
                    }

                }
            });
            MainFrame.MF.setLocationRelativeTo(null);//init mainframe
            FingeridCLI.DEFAULT_LOGGER.info("GUI initialized, showing GUI..");
            MainFrame.MF.decoradeMainFrameInstance();
        } else {
            //this os save because the only difference between cli and gui parameters is the --gui param
            SiriusCLIApplication.main(args);
        }
    }
}
