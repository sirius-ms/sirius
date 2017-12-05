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
    protected float getMax(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return (float) stats.getMax();
    }

    @Override
    protected float getMin(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return (float) stats.getMin();
    }


    protected float getSum(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return percentage ? (float) stats.getSum() : Float.NaN;
    }

    @Override
    protected float getPercentage(JTable table, float value, boolean isSelected, boolean hasFocus, int row, int column) {
        return (value / getSum(table, isSelected, hasFocus, row, column) * 100f);
    }
}
