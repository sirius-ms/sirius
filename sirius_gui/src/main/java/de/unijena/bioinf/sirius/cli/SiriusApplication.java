package de.unijena.bioinf.sirius.cli;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.fingerid.db.CustomDatabase;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.utils.SwingUtils;
import de.unijena.bioinf.sirius.net.ProxyManager;

import java.util.Arrays;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusApplication extends ApplicationCore {

    public static void main(String[] args) {
        //todo this is copy paste from CLI class -> make nice
        SiriusJobs.setGlobalJobManager(Integer.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.cores", "1"))+1);
        DEFAULT_LOGGER.info("Job manager initialized!");

        CustomDatabase.customDatabases(true);
        DEFAULT_LOGGER.info("Custom DBs initialized!");

        final ZodiacCLI cli = new ZodiacCLI();
        cli.parseArgs(args, FingerIdOptions.class);

        if (cli.options.isZodiac()) {
            ZodiacOptions options = null;
            try {
                options = CliFactory.createCli(ZodiacOptions.class).parseArguments(Arrays.copyOfRange(args, 1, args.length));
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
            SiriusJobs.setGlobalJobManager(
                    new SwingJobManager(Integer.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.cores", "1")))
            );
            DEFAULT_LOGGER.info("Swing Job manager initialized!");


            if (ProxyManager.getProxyStrategy() == null) {
                ApplicationCore.SIRIUS_PROPERTIES_FILE.changePropertyPersistent("de.unijena.bioinf.sirius.proxy", ProxyManager.DEFAULT_STRATEGY.name());
            }

            SwingUtils.initUI();
            MainFrame.MF.setLocationRelativeTo(null);//init mainframe


        } else {
            cli.setup();
            cli.validate();
            cli.compute();
        }
    }
}
