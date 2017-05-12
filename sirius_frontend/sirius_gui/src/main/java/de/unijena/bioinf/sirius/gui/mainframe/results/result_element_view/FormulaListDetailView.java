package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.BarTableCellRenderer;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultMatcherEditor;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultTableCellRenderer;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultTableFormat;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.utils.ActionTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaListDetailView extends FormulaListView {
    //    private static final int[] BAR_COLS = {2, 3, 4};
    private final ActionTable<SiriusResultElement> table;
    private final JTextField searchField = new JTextField();
    private final ConnectedSelection<SiriusResultElement> selectionConnection;

    public FormulaListDetailView(final FormulaList source) {
        super(source);
        setLayout(new BorderLayout());
        searchField.setPreferredSize(new Dimension(100, searchField.getPreferredSize().height));
        final SortedList<SiriusResultElement> sorted = new SortedList<SiriusResultElement>(source.resultList);
        final DefaultEventSelectionModel<SiriusResultElement> model = new DefaultEventSelectionModel<>(sorted);

        this.table = new ActionTable<>(source.resultList, sorted,
                new SiriusResultTableFormat(),
                new SiriusResultMatcherEditor(searchField),
                SiriusResultElement.class);
        table.setSelectionModel(model);


        selectionConnection = new ConnectedSelection<>(source.selectionModel, model, source.resultList, sorted);

        table.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer());

        table.getColumnModel().getColumn(2).setCellRenderer(new BarTableCellRenderer(true, true, source.scoreStats));
        table.getColumnModel().getColumn(3).setCellRenderer(new BarTableCellRenderer(false, false, source.isotopeScoreStats));
        table.getColumnModel().getColumn(4).setCellRenderer(new BarTableCellRenderer(false, false, source.treeScoreStats));

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
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JPanel sp = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        sp.add(new JLabel("Filter"));
        sp.add(searchField);
        top.add(sp);
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
