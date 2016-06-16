package de.unijena.bioinf.sirius.cli;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import gurobi.GRBEnv;
import gurobi.GRBException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusApplication {

    public static void main(String[] args) throws GRBException {
        GurobiSolver s = new GurobiSolver();
        GRBEnv e = s.getEnv();
        e.resetParams();
        if (args == null || args.length == 0) {
           new MainFrame();
        }else {
            final CLI<SiriusGUIOptions> cli = new CLI<>();
            cli.parseArgs(args, SiriusGUIOptions.class);
            if (cli.options.isGUI()) {
                new MainFrame();
            } else {
                cli.setup();
                cli.validate();
                cli.compute();
            }
        }
    }
}
