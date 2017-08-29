package de.unijena.bioinf.sirius.cli;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.utils.SwingUtils;
import de.unijena.bioinf.sirius.net.ProxyManager;

import java.util.Arrays;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusApplication extends ApplicationCore {

    public static void main(String[] args) {
        final ZodiacCLI cli = new ZodiacCLI();
        cli.parseArgs(args, FingerIdOptions.class);

        if (cli.options.isZodiac()){
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
            if (ProxyManager.getProxyStrategy() == null) {
                ApplicationCore.changeDefaultProptertyPersistent("de.unijena.bioinf.sirius.proxy", ProxyManager.DEFAULT_STRATEGY.name());
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
