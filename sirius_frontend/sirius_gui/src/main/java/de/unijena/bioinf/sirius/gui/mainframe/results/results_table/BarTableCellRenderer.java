package de.unijena.bioinf.sirius.gui.mainframe.results.results_table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import de.unijena.bioinf.sirius.gui.utils.Colors;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class BarTableCellRenderer extends SiriusResultTableCellRenderer {
    Color[] colors = {Colors.ICON_RED,Colors.ICON_YELLOW, Colors.ICON_GREEN};
//    Color[] colors = {Colors.ICON_RED, Colors.ICON_YELLOW, Colors.ICON_GREEN};
    float[] fractions = {.1f,.5f,1f};
//    float[] fractions = {1f / 8f, 3f / 8f, 1f};
    float toFill;
    boolean selected;
    String max;


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        double max = 0d; //todo cache the max?
        for (int i = 0; i < table.getRowCount(); i++) {
            max = Math.max(max, ((Double) table.getValueAt(i, column)).doubleValue());

        }
        this.max = NF.format(max);
        toFill = (float) (((Double) value).doubleValue() / max);
        selected = isSelected;

//        this.value = new DecimalFormat("#.00").format(((Double)value).doubleValue());
        return this;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int maxWord = g2d.getFontMetrics().stringWidth(max);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        g2d.setPaint(backColor);
        g2d.fillRect(0, 0, (getWidth()), getHeight());

        LinearGradientPaint gp = new LinearGradientPaint(5, 0, getWidth()-5, getHeight(), fractions, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g2d.setPaint(gp);
        g2d.fillRect(5, 2, (int) (getWidth() * toFill)-5, getHeight() - 4);


        if (selected) {
            g2d.setPaint(new Color(backColor.getRed(), backColor.getGreen(), backColor.getBlue(), 200));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setPaint(foreColor);
        } else {
            g2d.setPaint(Color.BLACK);
        }

        g2d.drawString(value, (getWidth()/2) + (maxWord/2)  - (g2d.getFontMetrics().stringWidth(value)), (getHeight() - 4));

    }
}




