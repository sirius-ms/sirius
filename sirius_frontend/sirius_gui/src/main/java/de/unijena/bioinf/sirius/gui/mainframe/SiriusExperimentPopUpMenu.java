package de.unijena.bioinf.sirius.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import de.unijena.bioinf.sirius.gui.actions.SiriusActionManager;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.Icons;

import javax.swing.*;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusExperimentPopUpMenu extends JPopupMenu {
    private JMenuItem newExpMI, batchMI, editMI, closeMI, computeMI, cancelMI;

    public SiriusExperimentPopUpMenu() {
        initActions();
        newExpMI = new JMenuItem(SiriusActions.IMPORT_EXP.getInstance());
        batchMI = new JMenuItem(SiriusActions.IMPORT_EXP_BATCH.getInstance());
        editMI = new JMenuItem(SiriusActions.EDIT_EXP.getInstance());
        closeMI = new JMenuItem(SiriusActions.DELETE_EXP.getInstance());
        computeMI = new JMenuItem(SiriusActions.COMPUTE.getInstance());
        cancelMI = new JMenuItem(SiriusActions.CANCEL_COMPUTE.getInstance());

        add(computeMI);
        add(cancelMI);
        addSeparator();
        add(newExpMI);
        add(batchMI);
//		addSeparator();
        add(editMI);
        add(closeMI);
//		addSeparator();


    }

    private ActionMap initActions(){
        ActionMap am = getActionMap();
        am.setParent(SiriusActionManager.ROOT_MANAGER);
        return am;
    }
}
