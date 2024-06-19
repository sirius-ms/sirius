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


import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.util.Map;


public class LinkedSiriusTableCellRenderer extends MouseAdapter implements TableCellRenderer {
    private final LinkCreator linker;
    private final LinkTextCreator texter;
    private final DefaultTableCellRenderer sourceRenderer;
    private JTable table;
    private final IntSet columns = new IntOpenHashSet();


    public <T> LinkedSiriusTableCellRenderer(DefaultTableCellRenderer sourceRenderer, LinkCreator<T> linker) {
        this(sourceRenderer, linker, s -> s != null ? s.toString():"N/A");
    }

    public <T, O> LinkedSiriusTableCellRenderer(DefaultTableCellRenderer sourceRenderer, LinkCreator<T> linker, LinkTextCreator<T, O> texter) {
        super();
        this.linker = linker;
        this.texter = texter;
        this.sourceRenderer = sourceRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel c = (JLabel) sourceRenderer.getTableCellRendererComponent(table, texter.makeLinkTextFromValue(value), isSelected, hasFocus, row, column);

        URI l = linker.makeLink(value);
        if (l != null) {
            if (!isSelected)
                c.setForeground(Color.BLUE.darker());

            Font font = c.getFont();
            Map attributes = font.getAttributes();
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            c.setFont(font.deriveFont(attributes));
        }
        return c;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
        int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
        if (columns.contains(col)) {
            URI url = linker.makeLink(table.getModel().getValueAt(row, col));
            if (url != null) {
                try {
                    Desktop.getDesktop().browse(url);
                } catch (IOException e1) {
                    LoggerFactory.getLogger(getClass()).error("Error opening link: " + url.toString(), e1);
                }
            }
        }
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
        if (columns.contains(col)) {
            int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
            final boolean handCursor = linker.makeLink(table.getModel().getValueAt(row, col)) != null;
            if (handCursor && table.getCursor().getType() != Cursor.HAND_CURSOR)
                table.setCursor(new Cursor(Cursor.HAND_CURSOR));
            else if (!handCursor) {
                table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        } else {
            if (table.getCursor().getType() != Cursor.DEFAULT_CURSOR)
                table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public void registerToTable(JTable table, int column) {
        if (this.table != table) {
            if (this.table != null) {
                this.table.removeMouseListener(this);
                this.table.removeMouseMotionListener(this);
            }
            columns.clear();
            this.table = table;
        }

        columns.add(column);
        table.getColumnModel().getColumn(column).setCellRenderer(this);
        table.addMouseListener(this);
        table.addMouseMotionListener(this);
    }


    @FunctionalInterface
    public interface LinkCreator<T> {
        @Nullable
        URI makeLink(T s);
    }

    @FunctionalInterface
    public interface LinkTextCreator<T, O> {
        @NotNull
        O makeLinkTextFromValue(T s);
    }
}