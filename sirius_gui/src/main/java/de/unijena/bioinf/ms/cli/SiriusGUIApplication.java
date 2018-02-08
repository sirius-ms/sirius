package de.unijena.bioinf.ms.cli;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
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
import java.util.Arrays;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusGUIApplication {

    public static void main(String[] args) {
        final ZodiacCLI<SiriusGUIOptions> cli = new ZodiacCLI<>();
        cli.parseArgs(args, SiriusGUIOptions.class);

        if (cli.options.isZodiac()) {
            ZodiacOptions options = null;
            try {
                String[] newArgs = removeFromArrayIgnoreCase(args, "--zodiac");
                options = CliFactory.createCli(ZodiacOptions.class).parseArguments(newArgs);
            } catch (HelpRequestedException e) {
                cli.println(e.getMessage());
                cli.println("");
                System.exit(0);
            }

            cli.setup();
            cli.validate();
            Zodiac zodiac = new Zodiac(options);
            zodiac.run();
        } else if (cli.options.isGUI()) {
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
        } else {
            try {
                cli.setup();
                cli.validate();
                cli.compute();
            } finally {
                try {
                    JobManager.shutDownAllInstances();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }

    }

    private static String[] removeFromArrayIgnoreCase(String[] args, String param) {
        int idx = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(param)) {
                idx = i;
                break;
            }

        }
        if (idx < 0) return args.clone();
        String[] argsNew = Arrays.copyOf(args, args.length - 1);
        for (int i = idx + 1; i < args.length; i++) {
            argsNew[i - 1] = args[i];
        }
        return argsNew;
    }
}
