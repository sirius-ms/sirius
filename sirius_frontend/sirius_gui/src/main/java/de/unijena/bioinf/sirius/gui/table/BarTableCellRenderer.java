package de.unijena.bioinf.sirius.gui.table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import de.unijena.bioinf.sirius.gui.configs.Colors;
import de.unijena.bioinf.sirius.gui.fingerid.CSIFingerIdComputation;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaScoreListStats;
import de.unijena.bioinf.sirius.gui.table.list_stats.ListStats;
import org.jdesktop.beans.AbstractBean;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class BarTableCellRenderer extends SiriusResultTableCellRenderer {
    private Color[] colors = {Colors.ICON_RED, Colors.ICON_YELLOW, Colors.ICON_GREEN};
    private float[] fractions = {.1f, .5f, 1f};
    private float toFill;
    private String percentageValue;
    private boolean selected;
    private String max;
    private float thresh;
    private final boolean drawThresh;
    private final boolean percentage;

    private final ListStats stats;

    public BarTableCellRenderer(ListStats stats) {
        this(false, stats);
    }

    public BarTableCellRenderer(boolean drawThresh, ListStats stats) {
        this(-1, drawThresh, false, stats);
    }

    public BarTableCellRenderer(int highlightColumn, boolean drawThresh, boolean percentage, ListStats stats) {
        super(highlightColumn);
        this.drawThresh = drawThresh;
        this.percentage = percentage;
        this.stats = stats;
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        double max = stats.getMax();
        double min = stats.getMin() - Math.abs(0.1 * max);
        //todo this is dirty
        double normSum = stats instanceof FormulaScoreListStats ? ((FormulaScoreListStats) stats).getExpScoreSum() : stats.getSum();

        double current = (Double) value;
        percentageValue = String.format("%.2f", Math.exp(current) / normSum * 100d) + "%";
        this.max = NF.format(max);
        toFill = (float) normalize(min, max, current);
        selected = isSelected;
        thresh = (float) normalize(min, max, Math.abs(CSIFingerIdComputation.calculateThreshold(max)));
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    private double normalize(double min, double max, double value) {
        return ((value - min) / (max - min));
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


        if (drawThresh) {
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
        if (percentage)
            v = percentageValue;

        g2d.drawString(v, (getWidth() / 2) + (maxWord / 2) - (g2d.getFontMetrics().stringWidth(v)), (getHeight() - 4));
    }
}




