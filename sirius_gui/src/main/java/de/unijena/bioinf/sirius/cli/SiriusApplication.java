package de.unijena.bioinf.sirius.cli;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusApplication {

    public static void main(String[] args) {
        final FingeridApplication cli = new FingeridApplication();
        cli.parseArgs(args, FingerIdOptions.class);
        if (cli.options.isGUI()) {
            new MainFrame();
        } else {
            cli.setup();
            cli.validate();
            cli.compute();
        }
//        new MainFrame();
    }
}
