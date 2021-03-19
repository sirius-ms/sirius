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

import de.unijena.bioinf.ms.gui.molecular_formular.FormulaScoreListStats;

import javax.swing.*;

/**
 * Created by fleisch on 24.05.17.
 */
public class FingerIDScoreBarRenderer extends ListStatBarTableCellRenderer<FormulaScoreListStats> {
    public FingerIDScoreBarRenderer(FormulaScoreListStats stats) {
        super(stats);
    }

    public FingerIDScoreBarRenderer(FormulaScoreListStats stats, boolean percentage) {
        super(stats, percentage);
    }

    public FingerIDScoreBarRenderer(int highlightColumn, FormulaScoreListStats stats, boolean percentage) {
        super(highlightColumn, stats, percentage);
    }


    @Override
    protected double getThresh(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        System.out.println("Reimplement threshold separator line computation"); //todo threshold
        return getMax(table, isSelected, hasFocus, row, column) * 0.5;

    }

    @Override
    protected double getSum(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return stats.getExpScoreSum();
    }

    @Override
    protected double getPercentage(JTable table, double value, boolean isSelected, boolean hasFocus, int row, int column) {
        return Math.exp(value) / getSum(table, isSelected, hasFocus, row, column) * 100d;
    }
}
