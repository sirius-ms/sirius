package de.unijena.bioinf.sirius.gui.mainframe.results.results_table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 24.01.17.
 */

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.utils.ActionTable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTablePanel extends JPanel {

    public final ActionTable<SiriusResultElement> table;
    private final JTextField searchField = new JTextField();
    private final ListSelectionListener selectionObserver = new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            addData(toObserve.getSelectedValuesList());

        }
    };

    final ListDataListener entryObserver = new ListDataListener() {
        @Override
        public void intervalAdded(ListDataEvent e) {
            // done by selection listener
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            //done bey selection listener
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            if (toObserve.getMinSelectionIndex() <= e.getIndex0() && toObserve.getMaxSelectionIndex() >= e.getIndex1()) {
                table.elements.clear();
                selected.clear();
                addData(toObserve.getSelectedValuesList());
            }
        }
    };


    private JList<ExperimentContainer> toObserve;
    private Set<ExperimentContainer> selected = new HashSet<>();


    public SiriusResultTablePanel(JList<ExperimentContainer> listToObserve) {
        this();
        observe(listToObserve);
    }

    private void addData(final Collection<ExperimentContainer> data) {
        if (data == null || data.isEmpty()) {
            table.elements.clear();
            selected.clear();
        } else {
            Set<ExperimentContainer> toRemove = new HashSet<>(selected);
            toRemove.removeAll(data);

            Set<ExperimentContainer> toAdd = new HashSet<>(data);
            toAdd.removeAll(selected);
            for (ExperimentContainer container : toRemove) {
                table.elements.removeAll(container.getResults());
            }
            for (ExperimentContainer container : toAdd) {
                table.elements.addAll(container.getResults());
            }
            selected = new HashSet<>(data);
        }
    }


    public SiriusResultTablePanel() {
        super(new BorderLayout());
        searchField.setPreferredSize(new Dimension(100, searchField.getPreferredSize().height));
        this.table = new ActionTable<SiriusResultElement>(new ArrayList<SiriusResultElement>(), new SiriusResultTableFormat(), new SiriusResultMatcherEditor(searchField), SiriusResultElement.class);
        TableColumn col = table.getColumnModel().getColumn(2);
        col.setCellRenderer(new BarTableCellRenderer());
        table.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer());

        this.add(
                new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER
        );

        addNorthPanel();
        addSouthPanel();
        addLeftPanel();
        addRightPanel();
    }

    private void addNorthPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JPanel sp = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        sp.add(new JLabel("Filter"));
        sp.add(searchField);
        top.add(sp);

        this.add(top, BorderLayout.NORTH);
    }

    private void addRightPanel() {

    }

    private void addLeftPanel() {

    }

    private void addSouthPanel() {

    }


    public void observe(JList<ExperimentContainer> dataList) {
        if (toObserve != null) {
            toObserve.removeListSelectionListener(selectionObserver);
            toObserve.getModel().removeListDataListener(entryObserver);
        }
        toObserve = dataList;

        addData(toObserve.getSelectedValuesList());
        toObserve.addListSelectionListener(selectionObserver);
        toObserve.getModel().addListDataListener(entryObserver);
    }

    public static void main(String[] args) {

        SiriusResultTablePanel srt = new SiriusResultTablePanel();
        // create a frame with that panel
        JFrame frame = new JFrame("Issues");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(540, 380);
        frame.getContentPane().add(srt);
        frame.show();
    }


}
