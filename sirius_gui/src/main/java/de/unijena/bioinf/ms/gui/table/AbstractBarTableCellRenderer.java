/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.table;

import de.unijena.bioinf.ms.gui.configs.Colors;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

/**
 * @author Markus Fleischauer
 */
public abstract class AbstractBarTableCellRenderer extends SiriusResultTableCellRenderer {
    //View modes
    protected final PercentageMode percentageMode;
    protected final boolean printMaxValue;


    protected Color[] colors = {Colors.FormulasView.SCORE_BAR, Colors.FormulasView.SCORE_BAR, Colors.FormulasView.SCORE_BAR}; //it is now one-color-only. Keep it as gradient for simplicity and easier updating
    protected float[] fractions = {.1f, .5f, 1f};
    protected float[] fractionsTwoWay = {.3f, 1f};

    public enum PercentageMode {
        DEACTIVATED,
        NO_TRANFORMATION,
        MULTIPLY_PROBABILITIES,
        NORMALIZE_TO_MAXIMUM,
        NORMALIZE_TO_SUM
    }

    private double percentageValue;
    private boolean selected;

    private String max;
    private float thresh;
    private float toFill;
    @Setter
    private boolean twoWayBar = false;

    public AbstractBarTableCellRenderer() {
        this(-1, PercentageMode.DEACTIVATED, false, null);
    }

    public AbstractBarTableCellRenderer(int highlightColumn, PercentageMode percentageMode, boolean printMaxValue, NumberFormat lableFormat) {
        super(highlightColumn, lableFormat);
        this.percentageMode = percentageMode;
        this.printMaxValue = printMaxValue;
    }

    protected abstract double getMax(JTable table, boolean isSelected, boolean hasFocus, int row, int column);

    protected abstract double getMin(JTable table, boolean isSelected, boolean hasFocus, int row, int column);

    protected abstract double getSum(JTable table, boolean isSelected, boolean hasFocus, int row, int column);

    //override this method if want a threshold line
    protected double getThresh(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        return Double.NaN;
    }

    //override this method if you have to modify the value of the cell for view
    protected Double getValue(Object value) {
        return value == null ? Double.NaN : (Double) value;
    }

    protected double getPercentage(JTable table, double value, boolean isSelected, boolean hasFocus, int row, int column) {
        return switch (percentageMode) {
            case NO_TRANFORMATION -> value;
            case MULTIPLY_PROBABILITIES -> value * 100d;
            case NORMALIZE_TO_MAXIMUM -> value / getMax(table, isSelected, hasFocus, row, column) * 100d;
            case NORMALIZE_TO_SUM -> value / getSum(table, isSelected, hasFocus, row, column) * 100d;
            default -> Double.NaN;
        };
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        double max = getMax(table, isSelected, hasFocus, row, column);
        double min = getMin(table, isSelected, hasFocus, row, column);

        final Double current = getValue(value);

        percentageValue = getPercentage(table, current, isSelected, hasFocus, row, column);
        this.max = nf.format(max);
        toFill = (float) normalize(min, max, current);
        selected = isSelected;
        thresh = (float) Math.max(0f, normalize(min, max, getThresh(table, isSelected, hasFocus, row, column)));

        return super.getTableCellRendererComponent(table, value != null ? current : "\u2699", isSelected, hasFocus, row, column);


    }

    private double normalize(double min, double max, double value) {
        return (value - min) / (max - min);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setPaint(backColor);
        g2d.fillRect(0, 0, (getWidth()), getHeight());

        if (twoWayBar) {
            if (toFill > 0.5) {
                LinearGradientPaint gp = new LinearGradientPaint((int) ((double)getWidth() / 2d), 0, getWidth() - 5, getHeight(), new float[]{0f,1}, new Color[]{backColor,Colors.FingerprintsView.PROBABILITY_OVER_50}, MultipleGradientPaint.CycleMethod.NO_CYCLE);
                g2d.setPaint(gp);
                g2d.fillRect(getWidth() / 2, 2, (int) (((double)getWidth() / 2d) * (toFill-0.5)/0.5) - 5, getHeight() - 4);
            } else {
                LinearGradientPaint gp = new LinearGradientPaint(5, 0, (int) ((double)getWidth() / 2d), getHeight(), new float[]{0f,1}, new Color[]{Colors.FingerprintsView.PROBABILITY_UNDER_50,backColor}, MultipleGradientPaint.CycleMethod.NO_CYCLE);
                g2d.setPaint(gp);
                double start = ((double)getWidth() / 2d * toFill/0.5);
                g2d.fillRect(5 + (int)start, 2, (int) ((((double)getWidth() / 2d) - start)), getHeight() - 4);
            }
        } else {
            LinearGradientPaint gp = new LinearGradientPaint(5, 0, getWidth() - 5, getHeight(), fractions, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE);
            g2d.setPaint(gp);
            g2d.fillRect(5, 2, (int) (getWidth() * toFill) - 5, getHeight() - 4);
        }


        if (!Float.isNaN(thresh)) {
            g2d.setPaint(Colors.Menu.ICON_BLUE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine((int) (getWidth() * thresh) - 5, 0, (int) (getWidth() * thresh) - 5, getHeight()/* - 4*/);
        }

        if (selected) {
            g2d.setPaint(new Color(backColor.getRed(), backColor.getGreen(), backColor.getBlue(), 100));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        g2d.setPaint(foreColor);
        int maxWord = g2d.getFontMetrics().stringWidth(max);
        String v = value;
        if (!Double.isNaN(percentageValue)) {
            maxWord = g2d.getFontMetrics().stringWidth(nf.format(100) + "%");
            if (Double.isInfinite(percentageValue)){
                v = "N/A";
            }else {
                v = nf.format(percentageValue) + "%";
                if (printMaxValue)
                    v = v + " (" + value + "/" + max + ")";
            }
        } else if (printMaxValue) {
            maxWord = g2d.getFontMetrics().stringWidth(max + "/" + max);
            v = value + "/" + max;
        }


        g2d.drawString(v, (getWidth() / 2) + (maxWord / 2) - (g2d.getFontMetrics().stringWidth(v)), (getHeight() - 4));
    }
}




