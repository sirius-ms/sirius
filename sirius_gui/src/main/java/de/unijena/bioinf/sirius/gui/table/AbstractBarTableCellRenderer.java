package de.unijena.bioinf.sirius.gui.table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import de.unijena.bioinf.sirius.gui.configs.Colors;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class AbstractBarTableCellRenderer extends SiriusResultTableCellRenderer {
    protected Color[] colors = {Colors.ICON_RED, Colors.ICON_YELLOW, Colors.ICON_GREEN};
    protected float[] fractions = {.1f, .5f, 1f};


    private double percentageValue;
    protected final boolean percentage;

    private boolean selected;

    private String max;
    private float thresh;
    private float toFill;

    public AbstractBarTableCellRenderer() {
        this(-1, false);
    }

    public AbstractBarTableCellRenderer(int highlightColumn, boolean percentage) {
        super(highlightColumn);
        this.percentage = percentage;
    }

    protected abstract double getMax(JTable table, boolean isSelected, boolean hasFocus, int row, int column);

    protected abstract double getMin(JTable table, boolean isSelected, boolean hasFocus, int row, int column);

    //override this method if want a threshold line
    protected double getThresh(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return Double.NaN;
    }

    //override this method if you have to modify the value of the cell for view
    protected double getValue(Object value) {
        return ((Double) value).floatValue();
    }

    protected double getPercentage(JTable table, double value, boolean isSelected, boolean hasFocus, int row, int column) {
        return percentage ? (value / getMax(table, isSelected, hasFocus, row, column) * 100d) : Double.NaN;
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        double max = getMax(table, isSelected, hasFocus, row, column);
        double min = getMin(table, isSelected, hasFocus, row, column);

        final double current = getValue(value);
        percentageValue = getPercentage(table, current, isSelected, hasFocus, row, column);

        this.max = NF.format(max);
        toFill = (float) normalize(min, max, current);
        selected = isSelected;
        thresh = (float) Math.max(0f, normalize(min, max, getThresh(table, isSelected, hasFocus, row, column)));
        return super.getTableCellRendererComponent(table, current, isSelected, hasFocus, row, column);
    }

    private double normalize(double min, double max, double value) {
        return (value - min) / (max - min);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int maxWord = g2d.getFontMetrics().stringWidth(max);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        g2d.setPaint(backColor);
        g2d.fillRect(0, 0, (getWidth()), getHeight());

        LinearGradientPaint gp = new LinearGradientPaint(5, 0, getWidth() - 5, getHeight(), fractions, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g2d.setPaint(gp);
        g2d.fillRect(5, 2, (int) (getWidth() * toFill) - 5, getHeight() - 4);


        if (!Float.isNaN(thresh)) {
            g2d.setPaint(Colors.ICON_BLUE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine((int) (getWidth() * thresh) - 5, 0, (int) (getWidth() * thresh) - 5, getHeight()/* - 4*/);
        }

        if (selected) {
            g2d.setPaint(new Color(backColor.getRed(), backColor.getGreen(), backColor.getBlue(), 200));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setPaint(foreColor);
        } else {
            g2d.setPaint(Color.BLACK);
        }

        String v = value;
        if (!Double.isNaN(percentageValue))
            v = String.format("%.2f", percentageValue) + "%";

        g2d.drawString(v, (getWidth() / 2) + (maxWord / 2) - (g2d.getFontMetrics().stringWidth(v)), (getHeight() - 4));
    }
}




