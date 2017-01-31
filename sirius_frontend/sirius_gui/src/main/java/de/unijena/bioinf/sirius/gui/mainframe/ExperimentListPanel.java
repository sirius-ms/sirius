package de.unijena.bioinf.sirius.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.sirius.gui.actions.SiriusActionManager;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.settings.TwoCloumnPanel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;
import java.util.List;

import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.COMPOUNT_LIST;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ExperimentListPanel extends TwoCloumnPanel {

    protected final JList<ExperimentContainer> compoundListView;
    protected final FilterList<ExperimentContainer> compoundList;

    private final JTextField searchField;
    private final List<ExperimentListChangeListener> listeners = new LinkedList<>();
    private JPopupMenu expPopMenu;

    public ExperimentListPanel() {
        searchField = new JTextField();

        compoundList = new FilterList<>(new ObservableElementList<>(COMPOUNT_LIST, GlazedLists.beanConnector(ExperimentContainer.class)),
                new TextComponentMatcherEditor<>(searchField, new TextFilterator<ExperimentContainer>() {
                    @Override
                    public void getFilterStrings(List<String> baseList, ExperimentContainer element) {
                        baseList.add(element.getGUIName());
                        baseList.add(element.getIonization().toString());
                        baseList.add(String.valueOf(element.getFocusedMass()));
                    }
                }, true));


        compoundListView = new JList<>(new DefaultEventListModel<>(compoundList));
        compoundListView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        compoundListView.setCellRenderer(new CompoundCellRenderer());

        compoundListView.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                notifyListenerSelectionChange();
            }
        });

        expPopMenu = new SiriusExperimentPopUpMenu(this);
        compoundListView.setComponentPopupMenu(expPopMenu);

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
                System.out.println("LEFT MOUSE");
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


        compoundList.addListEventListener(new ListEventListener<ExperimentContainer>() {
            @Override
            public void listChanged(final ListEvent<ExperimentContainer> listChanges) {
                notifyListenerDataChange(listChanges);
            }
        });


        JScrollPane pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setViewportView(compoundListView);

        add(new JLabel(" Filter:"), searchField);
        add(pane, 0, true);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        //decorate this guy
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        compoundListView.getInputMap().put(enterKey, SiriusActions.COMPUTE.name());

        KeyStroke delKey = KeyStroke.getKeyStroke("DELETE");
        compoundListView.getInputMap().put(delKey, SiriusActions.DELETE_EXP.name());
        compoundListView.getActionMap().setParent(SiriusActionManager.ROOT_MANAGER);


    }

    private void notifyListenerDataChange(ListEvent<ExperimentContainer> event) {
        for (ExperimentListChangeListener l : listeners) {
            l.listChanged(event,compoundListView);
        }
    }

    private void notifyListenerSelectionChange() {
        for (ExperimentListChangeListener l : listeners) {
            l.listSelectionChanged(compoundListView);
        }
    }

    //API methods
    public void addChangeListener(ExperimentListChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ExperimentListChangeListener l) {
        listeners.remove(l);
    }

    public JList<ExperimentContainer> getCompoundListView() {
        return compoundListView;
    }

    public FilterList<ExperimentContainer> getCompoundList() {
        return compoundList;
    }
}
