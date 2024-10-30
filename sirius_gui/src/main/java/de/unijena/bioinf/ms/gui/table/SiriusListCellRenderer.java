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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class SiriusListCellRenderer extends DefaultListCellRenderer {
    protected Color foreColor = Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR;
    protected Color backColor = Colors.CellsAndRows.Tables.ALTERNATING_ROW_1;
    @Nullable
    protected final Function<Object, String> toStringMapper;

    public SiriusListCellRenderer() {
        this(null);
    }
    public SiriusListCellRenderer(@Nullable Function<Object, String> toStringMapper) {
        this.toStringMapper = toStringMapper;
    }
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, toStringMapper != null ? toStringMapper.apply(value) : value, index, isSelected, cellHasFocus);

        if (isSelected) {
            backColor = Colors.CellsAndRows.Tables.SELECTED_ROW;
            foreColor = Colors.CellsAndRows.Tables.SELECTED_ROW_TEXT;
        } else {
            if (index % 2 == 0) backColor = Colors.CellsAndRows.Tables.ALTERNATING_ROW_1;
            else backColor = Colors.CellsAndRows.Tables.ALTERNATING_ROW_2;
            foreColor = Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR;
        }

        setBackground(backColor);
        setForeground(foreColor);
        return this;
    }
}
