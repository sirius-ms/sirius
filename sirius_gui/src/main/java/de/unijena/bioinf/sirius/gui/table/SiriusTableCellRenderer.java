package de.unijena.bioinf.sirius.gui.table;

import de.unijena.bioinf.sirius.gui.configs.Colors;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class SiriusTableCellRenderer extends DefaultTableCellRenderer {
    protected Color foreColor = Colors.LIST_ACTIVATED_FOREGROUND;
    protected Color backColor = Colors.LIST_EVEN_BACKGROUND;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            backColor = Colors.LIST_SELECTED_BACKGROUND;
        } else {
            if (row % 2 == 0) backColor = Colors.LIST_EVEN_BACKGROUND;
            else backColor = Colors.LIST_UNEVEN_BACKGROUND;
            foreColor = Colors.LIST_ACTIVATED_FOREGROUND;
        }

        setBackground(backColor);
        setForeground(foreColor);

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
