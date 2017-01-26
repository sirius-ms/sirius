package de.unijena.bioinf.sirius.gui.mainframe.results.results_table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import de.unijena.bioinf.sirius.gui.utils.Colors;
import de.unijena.bioinf.sirius.gui.utils.Fonts;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class BarTableCellRenderer extends SiriusResultTableCellRenderer {
    Color[] colors = {Colors.ICON_RED, Colors.ICON_YELLOW, Colors.ICON_GREEN};
    float[] fractions = {1f / 8f, 3f / 8f, 1f};
    float toFill;
    boolean selected;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        double max = 0d;
        for (int i = 0; i < table.getRowCount(); i++) {
            max = Math.max(max, ((Double) table.getValueAt(i, column)).doubleValue());

        }

        toFill = (float) (((Double) value).doubleValue() / max);
        selected = isSelected;
        setToolTipText(value.toString());
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);


        LinearGradientPaint gp = new LinearGradientPaint(0, 0, getWidth(), getHeight(), fractions, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g2d.setPaint(backColor);
        g2d.fillRect(0, 0, (getWidth()), getHeight());
        g2d.setPaint(gp);

        g2d.fillRect(0, 2, (int) (getWidth() * toFill), getHeight() -2);
        if (selected){
            g2d.setPaint(new Color(backColor.getRed(),backColor.getGreen(),backColor.getBlue(),200));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        g2d.setColor(Color.black);
        g2d.setPaint(Color.black);
        g2d.setFont(Fonts.FONT);
        g2d.drawString("Test", 0, 0);



    }
}




