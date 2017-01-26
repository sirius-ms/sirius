package de.unijena.bioinf.sirius.gui.mainframe.results.results_table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import de.unijena.bioinf.sirius.gui.utils.Colors;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableCellRenderer extends DefaultTableCellRenderer {

    protected Color foreColor;
    protected Color backColor;


    /*public  Component getComponent(double value, double max) {
        float toFill = (float) value / (float) max;
        p.setBackground(new Color(0, 0, 0, 0));


        Graphics2D g2d = (Graphics2D) p.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        LinearGradientPaint gp = new LinearGradientPaint(0, 0, p.getWidth(), p.getHeight(), fractions, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, (int) (p.getWidth() * toFill), p.getHeight());
        return p;
    }*/


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            backColor = Colors.LIST_SELECTED_BACKGROUND;
            foreColor = Colors.LIST_SELECTED_FOREGROUND;
        } else {
            if (row % 2 == 0) backColor = Colors.LIST_EVEN_BACKGROUND;
            else backColor = Colors.LIST_UNEVEN_BACKGROUND;
            foreColor = Colors.LIST_ACTIVATED_FOREGROUND;
        }
        setBackground(backColor);
        setForeground(foreColor);
        return super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
    }


    /*@Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(this.backColor);

        g2.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getWidth());

    }*/

}
