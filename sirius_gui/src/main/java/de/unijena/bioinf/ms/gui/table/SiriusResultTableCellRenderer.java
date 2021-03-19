/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.table;

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableCellRenderer extends DefaultTableCellRenderer {
    protected NumberFormat nf = new DecimalFormat("#0.000");


    protected Color foreColor = Colors.LIST_ACTIVATED_FOREGROUND;
    protected Color backColor = Colors.LIST_EVEN_BACKGROUND;
    protected String value;
    private final int highlightColumn;


    public SiriusResultTableCellRenderer(int highlightColumn) {
        this(highlightColumn,null);
    }

    public SiriusResultTableCellRenderer(int highlightColumn, NumberFormat lableFormat) {
        this.highlightColumn = highlightColumn;
        if (lableFormat != null)
            this.nf = lableFormat;
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        final boolean best = highlightColumn >= 0 && ((boolean) table.getModel().getValueAt(row, highlightColumn));

        if (isSelected) {
            if (best) {
                backColor = Colors.LIST_SELECTED_GREEN;
            } else {
                backColor = Colors.LIST_SELECTED_BACKGROUND;
            }
            foreColor = Colors.LIST_SELECTED_FOREGROUND;
        } else {
            if (best) {
                backColor = Colors.LIST_LIGHT_GREEN;
            } else {
                if (row % 2 == 0) backColor = Colors.LIST_EVEN_BACKGROUND;
                else backColor = Colors.LIST_UNEVEN_BACKGROUND;
            }
            foreColor = Colors.LIST_ACTIVATED_FOREGROUND;

        }

        setBackground(backColor);
        setForeground(foreColor);

        this.value = value == null ? "" : value.toString();
        setHorizontalAlignment(SwingConstants.LEFT);

        if (value instanceof Number) {
            if (value instanceof Double)
                this.value = nf.format(value);
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        if (table.getColumnName(column).equals("Rank")) {
            table.getColumnModel().getColumn(column).setMaxWidth(50);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        setToolTipText(GuiUtils.formatToolTip(Math.min(getFontMetrics(getFont()).stringWidth(this.value), GuiUtils.toolTipWidth), this.value));

        return super.getTableCellRendererComponent(table, this.value, isSelected, hasFocus, row, column);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }
}
