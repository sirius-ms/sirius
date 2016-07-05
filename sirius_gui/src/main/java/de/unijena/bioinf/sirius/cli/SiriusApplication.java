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
        final CLI<SiriusGUIOptions> cli = new CLI<>();
        cli.parseArgs(args, SiriusGUIOptions.class);
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
