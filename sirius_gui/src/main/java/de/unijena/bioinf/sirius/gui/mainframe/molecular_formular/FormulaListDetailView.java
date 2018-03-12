package de.unijena.bioinf.sirius.gui.mainframe.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.*;
import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaListDetailView extends ActionListDetailView<SiriusResultElement, ExperimentContainer, FormulaList> {
    //    private static final int[] BAR_COLS = {2, 3, 4};
    private final ActionTable<SiriusResultElement> table;
    private final ConnectedSelection<SiriusResultElement> selectionConnection;

    private final SiriusResultTableFormat tableFormat = new SiriusResultTableFormat(source.scoreStats);
    private SortedList<SiriusResultElement> sortedSource;

    public FormulaListDetailView(final FormulaList source) {
        super(source);


        table = new ActionTable<>(filteredSource, sortedSource, tableFormat);

        table.setSelectionModel(filteredSelectionModel);
        filteredSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionConnection = new ConnectedSelection<>(source.getResultListSelectionModel(), filteredSelectionModel, source.getElementList(), sortedSource);

        table.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer(tableFormat.highlightColumnIndex()));

        table.getColumnModel().getColumn(2).setCellRenderer(new FingerIDScoreBarRenderer(tableFormat.highlightColumnIndex(), source.scoreStats,true));
        table.getColumnModel().getColumn(3).setCellRenderer(new ListStatBarTableCellRenderer(tableFormat.highlightColumnIndex(), source.isotopeScoreStats,false));
        //table.getColumnModel().getColumn(4).setCellRenderer(new ListStatBarTableCellRenderer(tableFormat.highlightColumnIndex(), source.treeScoreStats,false));

        TableColumnModel v1 = table.getColumnModel();
        TableColumn v2 = v1.getColumn(5);
        //v2.setCellRenderer(new ListStatBarTableCellRenderer(8, source.treeScoreStats,false));

        /*
        TableColumn hui = table.getColumnModel().getColumn(5);
        DoubleListStats hui2 = source.treeScoreStats;

        table.getColumnModel().getColumn(5).setCellRenderer(new ListStatBarTableCellRenderer(tableFormat.highlightColumnIndex(), source.treeScoreStats,false));
*/
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
    }

    @Override
    protected JToolBar getToolBar() {
        return null;
    }

    @Override
    protected EventList<MatcherEditor<SiriusResultElement>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                (MatcherEditor<SiriusResultElement>) new StringMatcherEditor<>(tableFormat, searchField.textField)
        );
    }

    @Override
    protected FilterList<SiriusResultElement> configureFiltering(EventList<SiriusResultElement> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
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
