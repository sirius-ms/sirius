package de.unijena.bioinf.sirius.gui.mainframe.experiments;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import de.unijena.bioinf.sirius.gui.actions.SiriusActions;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusExperimentPopUpMenu extends JPopupMenu {
    private JMenuItem newExpMI, batchMI, editMI, closeMI, computeMI/*, cancelMI*/;

    public SiriusExperimentPopUpMenu() {
        newExpMI = new JMenuItem(SiriusActions.IMPORT_EXP.getInstance());
        batchMI = new JMenuItem(SiriusActions.IMPORT_EXP_BATCH.getInstance());
        editMI = new JMenuItem(SiriusActions.EDIT_EXP.getInstance());
        closeMI = new JMenuItem(SiriusActions.DELETE_EXP.getInstance());
        computeMI = new JMenuItem(SiriusActions.COMPUTE.getInstance());
//        cancelMI = new JMenuItem(SiriusActions.CANCEL_COMPUTE.getInstance());

        add(computeMI);
//        add(cancelMI);
        addSeparator();
        add(newExpMI);
        add(batchMI);
//		addSeparator();
        add(editMI);
        add(closeMI);
        addSeparator();
        add(new JMenuItem(SiriusActions.ORDER_BY_MASS.getInstance()));
        add(new JMenuItem(SiriusActions.ORDER_BY_NAME.getInstance()));
    }
}
