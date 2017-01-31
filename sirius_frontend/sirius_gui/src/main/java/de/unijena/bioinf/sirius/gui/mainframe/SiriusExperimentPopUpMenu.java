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

    public SiriusExperimentPopUpMenu(ExperimentListPanel toObserve) {
        initActions();
        newExpMI = new JMenuItem(SiriusActions.IMPORT_EXP.getInstance());
        batchMI = new JMenuItem(SiriusActions.IMPORT_EXP_BATCH.getInstance());
        editMI = new JMenuItem("Edit Experiment", Icons.EDIT_16);
        closeMI = new JMenuItem("Remove Experiment(s)", Icons.REMOVE_DOC_16);
        computeMI = new JMenuItem("Compute", Icons.RUN_16);
        cancelMI = new JMenuItem("Cancel Computation", Icons.CANCEL_16);

        //TODO finish actions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        editMI.setEnabled(false);
        closeMI.setEnabled(false);
        computeMI.setEnabled(false);
        cancelMI.setEnabled(false);

        add(computeMI);
        add(cancelMI);
        addSeparator();
        add(newExpMI);
        add(batchMI);
//		addSeparator();
        add(editMI);
        add(closeMI);
//		addSeparator();

//
        //filtered Workspace Listener
        toObserve.addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> event, JList<ExperimentContainer> source) {
                if (!event.getSourceList().isEmpty()) {
                    for (ExperimentContainer ec : source.getSelectedValuesList()) {
                        if (ec != null && (ec.isComputing() || ec.isQueued())) {
                            cancelMI.setEnabled(true); //todo move to action
                            return;
                        }
                    }
                }
                cancelMI.setEnabled(false);
            }

            @Override
            public void listSelectionChanged(JList<ExperimentContainer> source) {
                if (source.isSelectionEmpty()){
                    closeMI.setEnabled(false);
                    editMI.setEnabled(false);
                    computeMI.setEnabled(false);
                }else{
                    closeMI.setEnabled(true);
                    editMI.setEnabled(true);
                    computeMI.setEnabled(true);
                }
            }
        });
    }

    private ActionMap initActions(){
        ActionMap am = getActionMap();
        am.setParent(SiriusActionManager.ROOT_MANAGER);
        return am;
    }
}
