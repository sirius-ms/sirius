package de.unijena.bioinf.sirius.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import de.unijena.bioinf.sirius.cli.SiriusApplication;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.Icons;

import javax.swing.*;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusExperimentPopUpMenu extends JPopupMenu {
    private JMenuItem newExpMI, batchMI, editMI, closeMI, computeMI, cancelMI;

//    JList<ExperimentContainer> parent;

    final ActionMap am = initActions();

    public SiriusExperimentPopUpMenu(ExperimentListPanel toObserve) {
        newExpMI = new JMenuItem("Import Experiment",Icons.ADD_DOC_16);
        batchMI = new JMenuItem("Batch Import", Icons.BATCH_DOC_16);
        editMI = new JMenuItem("Edit Experiment", Icons.EDIT_16);
        closeMI = new JMenuItem("Remove Experiment(s)", Icons.REMOVE_DOC_16);
        computeMI = new JMenuItem("Compute", Icons.RUN_16);
        cancelMI = new JMenuItem("Cancel Computation", Icons.CANCEL_16);

        newExpMI.addActionListener(am.get("new_exp"));
        batchMI.addActionListener(am.get("batch_import"));
        editMI.addActionListener(am.get("edit_exp"));
        closeMI.addActionListener(am.get("delete"));
        computeMI.addActionListener(am.get("compute"));
        cancelMI.addActionListener(am.get("cancel_compute"));

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
            public void listChanged(ExperimentListChangeEvent listChanges) {
                if (listChanges.types.contains(ExperimentListChangeEvent.SELECTION)) {
                    if (listChanges.sourceList.isSelectionEmpty()){
                        closeMI.setEnabled(false);
                        editMI.setEnabled(false);
                        computeMI.setEnabled(false);
                    }else{
                        closeMI.setEnabled(true);
                        editMI.setEnabled(true);
                        computeMI.setEnabled(true);
                    }
                }


                //hast to be last because of return
                for (ExperimentContainer ec : listChanges.getSelected()) {
                    if (ec != null && (ec.isComputing() || ec.isQueued())) {
                        cancelMI.setEnabled(true);
                        return;
                    }
                }
                cancelMI.setEnabled(false);
            }
        });


    }

    private ActionMap initActions(){
        ActionMap am = getActionMap();
        am.setParent(MF.getACTIONS());
        return am;
    }
}
