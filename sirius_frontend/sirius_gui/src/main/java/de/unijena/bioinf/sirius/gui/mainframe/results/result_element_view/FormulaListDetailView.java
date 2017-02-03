package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

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

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaListDetailView extends FormulaListView {
    private static final int[] BAR_COLS = {2, 3, 4};
    private final ActionTable<SiriusResultElement> table;
    private final JTextField searchField = new JTextField();

    public FormulaListDetailView(final FormulaList source) {
        super(source);
        setLayout(new BorderLayout());
        searchField.setPreferredSize(new Dimension(100, searchField.getPreferredSize().height));
        final SortedList<SiriusResultElement>  sorted  = new SortedList<SiriusResultElement>(source.resultList);
        final DefaultEventSelectionModel<SiriusResultElement> model = new DefaultEventSelectionModel<>(sorted);

        this.table = new ActionTable<>(source.resultList,sorted,
                new SiriusResultTableFormat(),
                new SiriusResultMatcherEditor(searchField),
                SiriusResultElement.class);
        table.setSelectionModel(model);

        //todo this is ugly -> make nice
        source.selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (model.isSelectionEmpty() && source.selectionModel.isSelectionEmpty())
                    return;
                else if (!model.isSelectionEmpty() && !source.selectionModel.isSelectionEmpty() && source.selectionModel.getSelected().get(0) == model.getSelected().get(0)) {
                    return;
                }else{
                    model.setValueIsAdjusting(true);
                    source.selectionModel.setValueIsAdjusting(true);
                    if (source.selectionModel.isSelectionEmpty()) {
                        model.clearSelection();
                    }else{
                        SiriusResultElement element = source.selectionModel.getSelected().get(0);
                        int i = sorted.indexOf(element);
                        model.setSelectionInterval(i,i);
                    }
                    model.setValueIsAdjusting(false);
                    source.selectionModel.setValueIsAdjusting(false);
                }
            }
        });

        model.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (model.isSelectionEmpty() && source.selectionModel.isSelectionEmpty())
                    return;
                else if (!model.isSelectionEmpty() && !source.selectionModel.isSelectionEmpty() && source.selectionModel.getSelected().get(0) == model.getSelected().get(0)) {
                    return;
                }else{
                    model.setValueIsAdjusting(true);
                    source.selectionModel.setValueIsAdjusting(true);
                    if (model.isSelectionEmpty()) {
                        source.selectionModel.clearSelection();
                    }else{
                        SiriusResultElement element = model.getSelected().get(0);
                        int i = source.resultList.indexOf(element);
                        source.selectionModel.setSelectionInterval(i,i);
                    }
                    model.setValueIsAdjusting(false);
                    source.selectionModel.setValueIsAdjusting(false);
                }
            }
        });




        table.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer());

        for (int i = 0; i < BAR_COLS.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(BAR_COLS[i]);
            col.setCellRenderer(new BarTableCellRenderer(col.getHeaderValue().equals("Score")));
        }

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
        table.getActionMap().put(SiriusActions.COMPUTE_CSI_LOCAL.name(),SiriusActions.COMPUTE_CSI_LOCAL.getInstance());

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
}
