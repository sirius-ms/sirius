package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.BarTableCellRenderer;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultMatcherEditor;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultTableCellRenderer;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultTableFormat;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.utils.ActionTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaTableDetailView extends FormulaTableView {
    private static final int[] BAR_COLS = {2, 3, 4};
    private final ActionTable<SiriusResultElement> table;
    private final JTextField searchField = new JTextField();

    public FormulaTableDetailView(FormulaTable source) {
        super(source);
        setLayout(new BorderLayout());
        searchField.setPreferredSize(new Dimension(100, searchField.getPreferredSize().height));
        this.table = new ActionTable<>(source.resultList,
                new SiriusResultTableFormat(),
                new SiriusResultMatcherEditor(searchField),
                SiriusResultElement.class);
        table.setSelectionModel(source.selectionModel);

        table.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer());

        for (int i = 0; i < BAR_COLS.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(BAR_COLS[i]);
            col.setCellRenderer(new BarTableCellRenderer());
        }


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
