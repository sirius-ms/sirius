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

import de.unijena.bioinf.ms.gui.table.list_stats.ListStats;

import javax.swing.*;
import java.text.NumberFormat;

/**
 * Created by fleisch on 24.05.17.
 */
public class ListStatBarTableCellRenderer<L extends ListStats> extends AbstractBarTableCellRenderer {
    protected final L stats;


    public ListStatBarTableCellRenderer(L stats) {
        this(stats, false);
    }

    public ListStatBarTableCellRenderer( L stats, boolean percentage) {
        this(-1, stats, percentage, false, null);
    }

    public ListStatBarTableCellRenderer(int highlightColumn,  L stats, boolean percentage) {
        this(highlightColumn, stats, percentage, false, null);
    }

    public ListStatBarTableCellRenderer(int highlightColumn,  L stats, boolean percentage, boolean printMaxValue, NumberFormat lableFormat) {
        super(highlightColumn, percentage, printMaxValue, lableFormat);
        this.stats = stats;
    }

    @Override
    protected double getMax(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return stats.getMax();
    }

    @Override
    protected double getMin(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return stats.getMin();
    }


    protected double getSum(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return percentage ? stats.getSum() : Double.NaN;
    }


    @Override
    protected double getPercentage(JTable table, double value, boolean isSelected, boolean hasFocus, int row, int column) {
        return (value / getSum(table, isSelected, hasFocus, row, column) * 100d);
    }
}
