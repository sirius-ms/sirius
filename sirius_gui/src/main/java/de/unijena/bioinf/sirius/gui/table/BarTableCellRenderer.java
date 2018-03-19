package de.unijena.bioinf.sirius.gui.table;

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
