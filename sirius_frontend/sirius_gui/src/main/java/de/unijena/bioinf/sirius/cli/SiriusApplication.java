package de.unijena.bioinf.sirius.cli;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.actions.SiriusActionManager;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.utils.SwingUtils;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusApplication extends ApplicationCore {
    private SiriusApplication(){};

    public static void main(String[] args) {
        final FingeridApplication cli = new FingeridApplication();
        cli.parseArgs(args, FingerIdOptions.class);

        if (cli.options.isGUI()) {
            SwingUtils.initUI();
            MainFrame.MF.setLocationRelativeTo(null);//init mainframe


        } else {
            cli.setup();
            cli.validate();
            cli.compute();
        }
    }
}
