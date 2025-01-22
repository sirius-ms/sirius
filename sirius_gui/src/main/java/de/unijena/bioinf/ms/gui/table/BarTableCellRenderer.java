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

import javax.swing.*;
import java.text.NumberFormat;

/**
 * Created by fleisch on 24.05.17.
 */
public class BarTableCellRenderer extends AbstractBarTableCellRenderer {

    private final double min;
    private final double max;
    private final double sum;


    protected BarTableCellRenderer(int highlightColumn, float min, float max, float sum, PercentageMode percentage) {
        this(highlightColumn, min, max, sum, percentage, false, null);
    }

    protected BarTableCellRenderer(int highlightColumn, float min, float max, float sum, PercentageMode percentage, boolean printValue, NumberFormat lableFormat) {
        super(highlightColumn, percentage, printValue, lableFormat);
        this.min = min;
        this.max = max;
        this.sum = sum;
    }

    @Override
    protected double getMax(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return max;
    }

    @Override
    protected double getMin(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return min;
    }

    @Override
    protected double getSum(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return sum;
    }

    public static BarTableCellRenderer newRawValueBar(int highlightColumn, float min, float max){
        return new BarTableCellRenderer(highlightColumn, min, max, Float.NaN, PercentageMode.DEACTIVATED);
    }

    public static BarTableCellRenderer newProbabilityBar(int highlightColumn){
        return new BarTableCellRenderer(highlightColumn, 0, 1, Float.NaN, PercentageMode.MULTIPLY_PROBABILITIES);
    }

    public static BarTableCellRenderer newPercentageBar(int highlightColumn){
        return new BarTableCellRenderer(highlightColumn, 0, 100, Float.NaN, PercentageMode.NO_TRANFORMATION);
    }

    public static BarTableCellRenderer newMaxNormalizedPercentageBar(int highlightColumn, float min, float max){
        return new BarTableCellRenderer(highlightColumn, min, max, Float.NaN, PercentageMode.NORMALIZE_TO_MAXIMUM);
    }

    public static BarTableCellRenderer newSumNormalizedPercentageBar(int highlightColumn, float min, float max, float sum){
        return new BarTableCellRenderer(highlightColumn, min, max, sum, PercentageMode.NORMALIZE_TO_SUM);
    }
}
