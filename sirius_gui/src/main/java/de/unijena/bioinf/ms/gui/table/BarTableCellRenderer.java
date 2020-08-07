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

import javax.swing.*;
import java.text.NumberFormat;

/**
 * Created by fleisch on 24.05.17.
 */
public class BarTableCellRenderer extends AbstractBarTableCellRenderer {

    private double min = Double.MIN_VALUE;
    private double max = Double.MAX_VALUE;


    public BarTableCellRenderer(int highlightColumn, float min, float max, boolean percentage) {
        this(highlightColumn, percentage);
        setMin(min);
        setMax(max);
    }

    public BarTableCellRenderer(int highlightColumn, boolean percentage) {
        this(highlightColumn, percentage, false,null);
    }

    public BarTableCellRenderer(int highlightColumn, boolean percentage, boolean printValue, NumberFormat lableFormat) {
        super(highlightColumn, percentage, printValue, lableFormat);
    }

    public BarTableCellRenderer() {
        this(-1, false);
    }

    @Override
    protected double getMax(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return max;
    }

    @Override
    protected double getMin(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public void setMax(float max) {
        this.max = max;
    }
}
