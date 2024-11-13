/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.table;

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.function.Function;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableCellRenderer extends DefaultTableCellRenderer {
    protected NumberFormat nf = new DecimalFormat("#0.000");


    protected Color foreColor = Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR;
    protected Color backColor = Colors.CellsAndRows.Tables.ALTERNATING_ROW_1;
    protected String value;
    private final int highlightColumn;

    private final Function<Object, String> toString;

    private final Font customFont;

    public SiriusResultTableCellRenderer(int highlightColumn, @NotNull Function<Object, String> toString, @Nullable Font customFont) {
        this(highlightColumn,null, toString, customFont);
    }

    public SiriusResultTableCellRenderer(int highlightColumn) {
        this(highlightColumn, (NumberFormat) null, null);
    }

    public SiriusResultTableCellRenderer(int highlightColumn, @Nullable Font customFont) {
        this(highlightColumn, (NumberFormat) null, customFont);
    }

    public SiriusResultTableCellRenderer(int highlightColumn, @Nullable NumberFormat lableFormat) {
        this(highlightColumn, lableFormat, (v) -> v == null ? "" : v.toString(), null);
    }

    public SiriusResultTableCellRenderer(int highlightColumn, @Nullable NumberFormat lableFormat, @Nullable Font customFont) {
        this(highlightColumn, lableFormat, (v) -> v == null ? "" : v.toString(), customFont);
    }

    public SiriusResultTableCellRenderer(int highlightColumn, @Nullable NumberFormat lableFormat, @NotNull Function<Object, String> toString, @Nullable Font customFont) {
        this.toString = toString;
        this.highlightColumn = highlightColumn;
        this.customFont = customFont;
        if (lableFormat != null)
            this.nf = lableFormat;
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        final boolean best = highlightColumn >= 0 && ((boolean) table.getModel().getValueAt(row, highlightColumn));

        if (isSelected) {
            if (best) {
                backColor = Colors.CellsAndRows.BEST_HIT_SELECTED;
                foreColor = Colors.CellsAndRows.BEST_HIT_TEXT;
            } else {
                backColor = Colors.CellsAndRows.Tables.SELECTED_ROW;
                foreColor = Colors.CellsAndRows.Tables.SELECTED_ROW_TEXT;
            }
        } else {
            if (best) {
                backColor = Colors.CellsAndRows.BEST_HIT;
                foreColor = Colors.CellsAndRows.BEST_HIT_TEXT;
            } else {
                if (row % 2 == 0) {
                    backColor = Colors.CellsAndRows.Tables.ALTERNATING_ROW_1;}
                else {
                    backColor = Colors.CellsAndRows.Tables.ALTERNATING_ROW_2;
                }
                foreColor = Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR;
            }


        }

        setBackground(backColor);
        setForeground(foreColor);

        this.value = toString.apply(value);
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

        setFont(customFont != null ? customFont : table.getFont());
        setValue(this.value);
        setToolTipText(GuiUtils.formatToolTip(Math.min(getFontMetrics(getFont()).stringWidth(this.value), GuiUtils.toolTipWidth), this.value));

        return this;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }
}
