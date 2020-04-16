package de.unijena.bioinf.ms.gui.table;

import de.unijena.bioinf.ms.gui.molecular_formular.FormulaScoreListStats;

import javax.swing.*;
import java.text.NumberFormat;
import java.util.function.Function;

/**
 * Created by fleisch on 24.05.17.
 */
public class ScoreListStatBarTableCellRenderer extends ListStatBarTableCellRenderer<FormulaScoreListStats> {
//    private Function<FormulaScoreListStats, Double> thresholder = (stats) -> Math.max(stats.getMax(), 0) - Math.max(5, stats.getMax() * 0.25);

    public ScoreListStatBarTableCellRenderer(FormulaScoreListStats stats) {
        super(stats);
    }

    public ScoreListStatBarTableCellRenderer(FormulaScoreListStats stats, boolean percentage) {
        super(stats, percentage);
    }

    public ScoreListStatBarTableCellRenderer(int highlightColumn, FormulaScoreListStats stats, boolean percentage) {
        super(highlightColumn, stats, percentage);
    }

    public ScoreListStatBarTableCellRenderer(int highlightColumn, FormulaScoreListStats stats, boolean percentage, boolean printMaxValue, NumberFormat lableFormat) {
        super(highlightColumn, stats, percentage, printMaxValue, lableFormat);
    }

   /* @Override
    protected double getThresh(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        System.out.println("Reimplement threshold separator line computation"); //todo threshold
        double v = Math.exp(thresholder.apply(stats) - stats.getMax());
        System.out.println("Thresh:" + v);
        return v;
    }
*/
    @Override
    protected double getMax(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return stats.getExpMaxScore();
    }

    @Override
    protected double getMin(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return stats.getExpMinScore();
    }

    @Override
    protected double getSum(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return stats.getExpScoreSum();
    }

    @Override
    protected Double getValue(Object value) {
        return Math.exp(super.getValue(value) - stats.getMax());
    }
}
