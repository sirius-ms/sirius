
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *  
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker, 
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ftalign.view;

import gnu.trove.list.array.TDoubleArrayList;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeMap;

public class PlotWindow extends JPanel {

    private final ApplicationState state;
    private final double[] values;
    private ApplicationWindow applicationWindow;

    public PlotWindow(ApplicationWindow applicationWindow, ApplicationState state) {
        this.state = state;
        this.applicationWindow = applicationWindow;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        final HistogramDataset histogram = new HistogramDataset();
        {
            final TDoubleArrayList list = new TDoubleArrayList();
            final TreeMap<String, DataElement> map = state.getTreeMap();
            for (DataElement elem : map.values()) {
                for (DataElement elem2 : map.tailMap(elem.getName(), false).values()) {
                    final double tanimoto = elem.tanimoto(elem2);
                    list.add(tanimoto);
                }
            }
            values = list.toArray();
            Arrays.sort(values);
        }
        histogram.addSeries("molecules", values, 20);
        final JFreeChart chart =
                ChartFactory.createHistogram("Tanimoto", "Tanimoto Score", "Amount", histogram, PlotOrientation.VERTICAL, false, false, false);
        chart.getXYPlot().getRenderer().setBaseToolTipGenerator(new XYToolTipGenerator() {
            @Override
            public String generateToolTip(XYDataset dataset, int series, int item) {
                final double tanimoto = dataset.getX(series, item).doubleValue();
                final int amount = dataset.getY(series, item).intValue();
                return String.format(Locale.US, "%d compounds with tanimoto %f", amount, tanimoto);
            }
        });
        final ChartPanel panel = new ChartPanel(chart);
        add(panel);
        add(new SelectThreshold());
    }

    private class SelectThreshold extends JPanel {

        private SelectThreshold() {
            final JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
            final JLabel label = new JLabel(String.format(Locale.US, "Set Tanimoto Threshold: %d %% (%d alignments)", 50, getAlignmentNumber(50)));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(label);
            add(slider);
            final int[] oldVal = new int[]{50};
            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int newVal = slider.getValue();
                    if (newVal != oldVal[0]) {
                        oldVal[0] = newVal;
                        final int alnum = getAlignmentNumber(newVal);
                        label.setText(String.format(Locale.US, "Set Tanimoto Threshold: %d %% (%d alignments)", newVal, alnum));
                        label.repaint();
                    }
                }
            });
            final JButton button = new JButton();
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    state.buildSubset(slider.getValue());
                    applicationWindow.showAlignmentWindow();
                }
            });
            button.setText("START");
            button.setPreferredSize(new Dimension(640, 32));
            add(button);
        }

        private int getAlignmentNumber(int value) {
            double val = value / 100d;
            int i = Arrays.binarySearch(values, val);
            if (i < 0) i = -(i + 1);
            return values.length - i;
        }
    }
}
