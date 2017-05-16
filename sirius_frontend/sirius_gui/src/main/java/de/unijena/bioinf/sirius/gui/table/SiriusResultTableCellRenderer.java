package de.unijena.bioinf.sirius.gui.table;

/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import de.unijena.bioinf.sirius.gui.configs.Colors;
import org.jdesktop.beans.AbstractBean;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableCellRenderer<E extends AbstractBean, F extends SiriusTableFormat<E>> extends DefaultTableCellRenderer {
    public final static NumberFormat NF = new DecimalFormat("#0.00");


    protected Color foreColor = Colors.LIST_ACTIVATED_FOREGROUND;
    protected Color backColor = Colors.LIST_EVEN_BACKGROUND;
    protected String value;
    private final int highlightColumn;

    public SiriusResultTableCellRenderer(int highlightColumn) {
        this.highlightColumn = highlightColumn;
    }

    public SiriusResultTableCellRenderer() {
        this.highlightColumn = -1;
    }



    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        final boolean best = highlightColumn >= 0?((Boolean) table.getModel().getValueAt(row, highlightColumn)).booleanValue():false;

        if (isSelected) {
            if (best) {
                backColor = Colors.LIST_SELECTED_GREEN;
            } else {
                backColor = Colors.LIST_SELECTED_BACKGROUND;
            }
            foreColor = Colors.LIST_SELECTED_FOREGROUND;
        } else {
            if (best) {
                this.backColor = Colors.LIST_LIGHT_GREEN;
            }else{
                if (row % 2 == 0) backColor = Colors.LIST_EVEN_BACKGROUND;
                else backColor = Colors.LIST_UNEVEN_BACKGROUND;
            }
            foreColor = Colors.LIST_ACTIVATED_FOREGROUND;

        }

        setBackground(backColor);
        setForeground(foreColor);

        if (value == null) System.out.println("col=" + column+ " row=" + row);
        this.value = value.toString();
        setHorizontalAlignment(SwingConstants.LEFT);

        if (value instanceof Number) {
            if (value instanceof Double)
                this.value = NF.format(value);
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        if (table.getColumnName(column).equals("Rank")) {
            table.getColumnModel().getColumn(column).setMaxWidth(50);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        return super.getTableCellRendererComponent(table, this.value, isSelected, hasFocus, row, column);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }
}
