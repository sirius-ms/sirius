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
import org.gnu.glpk.GLPK;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusApplication {

    public static void main(String[] args) throws GRBException {
        //todo remove debugging
        System.out.println("check gurobi");
        GurobiSolver s = new GurobiSolver();
        GRBEnv e = s.getEnv();
        System.out.println("check GLPK");
        System.out.println(GLPK.glp_version());
        e.resetParams();


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
