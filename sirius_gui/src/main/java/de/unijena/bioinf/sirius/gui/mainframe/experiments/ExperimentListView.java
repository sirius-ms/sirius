package de.unijena.bioinf.sirius.gui.mainframe.experiments;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 01.02.17.
 */

import ca.odell.glazedlists.swing.DefaultEventListModel;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ExperimentListView extends JScrollPane {

    final ExperimentList sourceList;
    final JList<ExperimentContainer> compoundListView;
    final JPopupMenu expPopMenu;

    public ExperimentListView(ExperimentList sourceList) {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.sourceList = sourceList;

        //todo move texfield and filter funktion here
        compoundListView = new JList<>(new DefaultEventListModel<>(sourceList.compoundList));
        compoundListView.setSelectionModel(sourceList.compountListSelectionModel);
        compoundListView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        compoundListView.setCellRenderer(new CompoundCellRenderer());

        this.expPopMenu = new SiriusExperimentPopUpMenu();

        compoundListView.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    int index = compoundListView.locationToIndex(e.getPoint());
                    compoundListView.setSelectedIndex(index);
                    SiriusActions.COMPUTE.getInstance().actionPerformed(new ActionEvent(compoundListView, 123, SiriusActions.COMPUTE.name()));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int indx = compoundListView.locationToIndex(e.getPoint());
                    boolean select = true;
                    for (int i : compoundListView.getSelectedIndices()) {
                        if (indx == i) {
                            select = false;
                            break;
                        }
                    }
                    if (select) {
                        compoundListView.setSelectedIndex(indx);
                    }

                    if (e.isPopupTrigger()) {
                        expPopMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });


        setViewportView(compoundListView);

        //decorate this guy
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        compoundListView.getInputMap().put(enterKey, SiriusActions.COMPUTE.name());

        KeyStroke delKey = KeyStroke.getKeyStroke("DELETE");
        compoundListView.getInputMap().put(delKey, SiriusActions.DELETE_EXP.name());

        compoundListView.getActionMap().put(SiriusActions.DELETE_EXP.name(), SiriusActions.DELETE_EXP.getInstance());
        compoundListView.getActionMap().put(SiriusActions.COMPUTE.name(), SiriusActions.COMPUTE.getInstance());
    }

    public JPopupMenu getExpPopMenu() {
        return expPopMenu;
    }
}
