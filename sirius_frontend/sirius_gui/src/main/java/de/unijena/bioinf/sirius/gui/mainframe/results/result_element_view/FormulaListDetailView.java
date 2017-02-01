package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.BarTableCellRenderer;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultMatcherEditor;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultTableCellRenderer;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultTableFormat;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.utils.ActionTable;

import javax.swing.*;
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

    public FormulaListDetailView(FormulaList source) {
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
