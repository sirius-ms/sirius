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

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.COMPOUNT_LIST;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ExperimentListPanel extends TwoCloumnPanel {

    public final JList<ExperimentContainer> compoundList;
    protected final FilterList<ExperimentContainer> compoundEventList;
    private final JTextField searchField;
    private final List<ExperimentListChangeListener> listeners = new LinkedList<>();
    private JPopupMenu expPopMenu;

    public ExperimentListPanel() {
        searchField = new JTextField();

        compoundEventList = new FilterList<>(new ObservableElementList<>(COMPOUNT_LIST, GlazedLists.beanConnector(ExperimentContainer.class)),
                new TextComponentMatcherEditor<>(searchField, new TextFilterator<ExperimentContainer>() {
                    @Override
                    public void getFilterStrings(List<String> baseList, ExperimentContainer element) {
                        baseList.add(element.getGUIName());
                        baseList.add(element.getIonization().toString());
                        baseList.add(String.valueOf(element.getFocusedMass()));
                    }
                }, true));


        compoundList = new JList<>(new DefaultEventListModel<>(compoundEventList));
        compoundList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        compoundList.setCellRenderer(new CompoundCellRenderer());

        compoundList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                notifyListener(new ExperimentListChangeEvent(compoundList));
            }
        });

        expPopMenu = new SiriusExperimentPopUpMenu(this);
        compoundList.setComponentPopupMenu(expPopMenu);

        compoundList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    int index = compoundList.locationToIndex(e.getPoint());
                    compoundList.setSelectedIndex(index);
                    ActionMap am = MF.getACTIONS();
                    MF.getACTIONS().get("compute").actionPerformed(new ActionEvent(compoundList, 123, "compute"));
                    //todo does this work?
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println("HERE");
                if (SwingUtilities.isRightMouseButton(e)) {
                    int indx = compoundList.locationToIndex(e.getPoint());
                    boolean select = true;
                    for (int i : compoundList.getSelectedIndices()) {
                        if (indx == i) {
                            select = false;
                            break;
                        }
                    }
                    if (select) {
                        compoundList.setSelectedIndex(indx);
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


        compoundEventList.addListEventListener(new ListEventListener<ExperimentContainer>() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> listChanges) {
                notifyListener(new ExperimentListChangeEvent(compoundList, listChanges));
            }
        });


        JScrollPane pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setViewportView(compoundList);

        add(new JLabel(" Filter:"), searchField);
        add(pane, 0, true);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        //decorate this guy
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        compoundList.getInputMap().put(enterKey, "compute");

        KeyStroke delKey = KeyStroke.getKeyStroke("DELETE");
        compoundList.getInputMap().put(delKey, "delete");
        compoundList.getActionMap().setParent(MF.getACTIONS());


    }

    protected void notifyListener(final ExperimentListChangeEvent event) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (ExperimentListChangeListener l : listeners) {
                    l.listChanged(event);
                }
            }
        });

    }

    public void addChangeListener(ExperimentListChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ExperimentListChangeListener l) {
        listeners.remove(l);
    }
}
