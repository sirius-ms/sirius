package de.unijena.bioinf.sirius.gui.table;

import de.unijena.bioinf.fingerid.CSIFingerIdComputation;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaScoreListStats;

import javax.swing.*;

/**
 * Created by fleisch on 24.05.17.
 */
public class FingerIDScoreBarRenderer extends ListStatBarTableCellRenderer {
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
        return CSIFingerIdComputation.calculateThreshold(
                getMax(table, isSelected, hasFocus, row, column)
        );
    }

    @Override
    protected double getSum(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return ((FormulaScoreListStats) stats).getExpScoreSum();
    }

    @Override
    protected double getPercentage(JTable table, double value, boolean isSelected, boolean hasFocus, int row, int column) {
        return Math.exp(value) / getSum(table, isSelected, hasFocus, row, column) * 100d;
    }
}
