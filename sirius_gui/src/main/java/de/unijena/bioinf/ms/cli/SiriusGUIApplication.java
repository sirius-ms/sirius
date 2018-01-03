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
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.utils.SwingUtils;
import de.unijena.bioinf.sirius.net.ProxyManager;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusGUIApplication {

    public static void main(String[] args) {
        final FingeridCLI<SiriusGUIOptions> cli = new FingeridCLI<>();
        cli.parseArgs(args, SiriusGUIOptions.class);


        if (cli.options.isGUI()) {
            SiriusJobs.setGlobalJobManager(
                    new SwingJobManager(Integer.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.cores", "1")))
            );
            FingeridCLI.DEFAULT_LOGGER.info("Swing Job manager initialized!");


            if (ProxyManager.getProxyStrategy() == null) {
                ApplicationCore.SIRIUS_PROPERTIES_FILE.setAndStoreProperty("de.unijena.bioinf.sirius.proxy", ProxyManager.DEFAULT_STRATEGY.name());
            }

            SwingUtils.initUI();
            MainFrame.MF.setLocationRelativeTo(null);//init mainframe
        } else {
            cli.setup();
            cli.validate();
            cli.compute();
        }

        /*try {
            JobManager.shutDownAllInstances();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(0);
        }*/
    }
}
