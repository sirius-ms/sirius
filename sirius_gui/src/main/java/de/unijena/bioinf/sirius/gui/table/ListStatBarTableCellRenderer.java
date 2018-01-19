package de.unijena.bioinf.sirius.gui.table;

import de.unijena.bioinf.sirius.gui.table.list_stats.ListStats;

import javax.swing.*;

/**
 * Created by fleisch on 24.05.17.
 */
public class ListStatBarTableCellRenderer extends AbstractBarTableCellRenderer {
    protected final ListStats stats;


    public ListStatBarTableCellRenderer(ListStats stats) {
        this(stats, false);
    }

    public ListStatBarTableCellRenderer(ListStats stats, boolean percentage) {
        this(-1, stats, percentage);
    }

    public ListStatBarTableCellRenderer(int highlightColumn, ListStats stats, boolean percentage) {
        super(highlightColumn,percentage);
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
        return percentage ?  stats.getSum() : Double.NaN;
    }

    @Override
    protected double getPercentage(JTable table, double value, boolean isSelected, boolean hasFocus, int row, int column) {
        return (value / getSum(table, isSelected, hasFocus, row, column) * 100f);
    }
}
