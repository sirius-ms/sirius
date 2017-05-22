package de.unijena.bioinf.sirius.gui.mainframe.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.*;
import de.unijena.bioinf.sirius.gui.utils.SearchTextField;
import javafx.collections.transformation.FilteredList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaListDetailView extends ActionListView<FormulaList> { //todo change to detailview
    //    private static final int[] BAR_COLS = {2, 3, 4};
    private final ActionTable<SiriusResultElement> table;
    private final SearchTextField searchField = new SearchTextField();
    private final ConnectedSelection<SiriusResultElement> selectionConnection;

    public FormulaListDetailView(final FormulaList source) {
        super(source);
        setLayout(new BorderLayout());
        searchField.setPreferredSize(new Dimension(100, searchField.getPreferredSize().height));

        final SiriusResultTableFormat tf = new SiriusResultTableFormat();
        final SortedList<SiriusResultElement> sorted = new SortedList<>(source.getElementList());
        final FilterList<SiriusResultElement> filtered = new FilterList<>(sorted, new StringMatcherEditor<>(tf, searchField.textField));
        final DefaultEventSelectionModel<SiriusResultElement> model = new DefaultEventSelectionModel<>(sorted);


        this.table = new ActionTable<>(filtered,sorted, tf);
        table.setSelectionModel(model);

        selectionConnection = new ConnectedSelection<>(source.getResultListSelectionModel(), model, source.getElementList(), sorted);

        table.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer(tf.highlightColumn()));

        table.getColumnModel().getColumn(2).setCellRenderer(new BarTableCellRenderer(tf.highlightColumn(),true, true, source.scoreStats));
        table.getColumnModel().getColumn(3).setCellRenderer(new BarTableCellRenderer(tf.highlightColumn(),false, false, source.isotopeScoreStats));
        table.getColumnModel().getColumn(4).setCellRenderer(new BarTableCellRenderer(tf.highlightColumn(),false, false, source.treeScoreStats));

        table.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    int index = table.rowAtPoint(e.getPoint());
                    table.setRowSelectionInterval(index, index);
                    SiriusActions.COMPUTE_CSI_LOCAL.getInstance().actionPerformed(new ActionEvent(table, 112, SiriusActions.COMPUTE_CSI_LOCAL.name()));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

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

        //decorate this guy
        //decorate this guy
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        table.getInputMap().put(enterKey, SiriusActions.COMPUTE_CSI_LOCAL.name());
        table.getActionMap().put(SiriusActions.COMPUTE_CSI_LOCAL.name(), SiriusActions.COMPUTE_CSI_LOCAL.getInstance());

        this.add(
                new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER
        );

        this.add(createNorth(), BorderLayout.NORTH);


    }

    ///////////////// Internal //////////////////////////
    protected JPanel createNorth() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

        top.add(searchField);
        return top;
    }


    private class ConnectedSelection<T> {
        final DefaultEventSelectionModel<T> model1;
        final DefaultEventSelectionModel<T> model2;

        final EventList<T> model1List;
        final EventList<T> model2List;

        final Map<DefaultEventSelectionModel, ListSelectionListener> modelTListener = new HashMap<>();

        public ConnectedSelection(DefaultEventSelectionModel<T> model1, DefaultEventSelectionModel<T> model2, EventList<T> model1List, EventList<T> model2List) {
            this.model1 = model1;
            this.model2 = model2;

            this.model1List = model1List;
            this.model2List = model2List;
            addListeners();
        }

        public void addListeners() {
            modelTListener.put(model1, createAndAddListener(model1, model2, model2List));
            modelTListener.put(model2, createAndAddListener(model2, model1, model1List));
        }

        public void removeListeners() {
            model1.removeListSelectionListener(modelTListener.get(model1));
            model2.removeListSelectionListener(modelTListener.get(model2));
            modelTListener.clear();
        }

        private ListSelectionListener createAndAddListener(final DefaultEventSelectionModel<T> notifier, final DefaultEventSelectionModel<T> listener, final EventList<T> listenerList) {
            ListSelectionListener l = new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (notifier.isSelectionEmpty()) {
                        if (!listener.isSelectionEmpty())
                            listener.clearSelection();
                        return;
                    } else {
                        EventList<T> s1 = notifier.getSelected();
                        T s = s1.get(0);
                        if (!listener.isSelectionEmpty()) {
                            EventList<T> s2 = listener.getSelected();
                            if ((s1.size() == 1 || s2.size() == 1) && (s == s2.get(0))) {
                                return;
                            }
                        }

                        listener.removeListSelectionListener(modelTListener.get(listener));
                        int i = listenerList.indexOf(s);
                        listener.setSelectionInterval(i, i);
                        listener.addListSelectionListener(modelTListener.get(listener));
                    }
                }
            };
            notifier.addListSelectionListener(l);
            return l;
        }
    }
}
