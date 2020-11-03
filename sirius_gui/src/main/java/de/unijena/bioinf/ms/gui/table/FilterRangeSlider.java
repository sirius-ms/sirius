/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.table;

import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import eu.hansolo.rangeslider.RangeSlider;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FilterRangeSlider<L extends ActionList<E, D>, E extends SiriusPCS, D> extends JPanel implements ActiveElementChangedListener<E, D> {
    private static final String RANGE_CHANGE = "range-change";
    public static final String DEFAUTL_INT_FORMAT = "##0";
    public static final String DEFAUTL_DOUBLE_FORMAT = "##0.00";

    public final boolean percentage;
    protected final RangeSlider rangeSlider;
    protected final JLabel left, right;
    protected final DecimalFormat format;
    private AtomicBoolean isRefreshing = new AtomicBoolean(false);
    protected final DoubleListStats stats;

    private final double valueMultiplier;
    private final double viewMultiplier;
    private final int numberLength;


    public FilterRangeSlider(L source, DoubleListStats stats) {
        this(source, stats, false, DEFAUTL_DOUBLE_FORMAT);
    }

    public FilterRangeSlider(L source, DoubleListStats stats, boolean percentage) {
        this(source, stats, percentage, DEFAUTL_DOUBLE_FORMAT);
    }

    public FilterRangeSlider(L source, DoubleListStats stats, boolean percentage, String decimalFormat) {

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());

        this.percentage = percentage;
        this.stats = stats;

        rangeSlider = new RangeSlider(Integer.MIN_VALUE, Integer.MAX_VALUE);
        rangeSlider.setLowerValue(rangeSlider.getMinimum());
        rangeSlider.setUpperValue(rangeSlider.getMaximum());
        rangeSlider.setThumbShape(RangeSlider.ThumbShape.DROP);
        rangeSlider.getModel().addChangeListener(e -> {
            if (!isRefreshing.get())
                refreshText();
        });


        String prototype = "0000";
        if (percentage) {
            format = new DecimalFormat("##0");
            prototype = "100";
            viewMultiplier = valueMultiplier = 100d;
        } else {
            format = new DecimalFormat(decimalFormat);
            viewMultiplier = 1d;
            valueMultiplier = 20d;
        }

        left = new JLabel(prototype);
        Dimension s = left.getPreferredSize();
        left.setMinimumSize(s);
        left.setMaximumSize(s);
        left.setPreferredSize(s);

        right = new JLabel(prototype);
        s = right.getPreferredSize();
        right.setMinimumSize(s);
        right.setMaximumSize(s);
        right.setPreferredSize(s);


        numberLength = prototype.length();

        add(left, BorderLayout.WEST);
        add(rangeSlider, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        source.addActiveResultChangedListener(this);
        rangeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                firePropertyChange(RANGE_CHANGE, null, rangeSlider);
            }
        });
    }


    public void addRangeListener(PropertyChangeListener listener) {
        addPropertyChangeListener(RANGE_CHANGE, listener);
    }


    public double getMaxSelected() {
        return (rangeSlider.getLowerValue() + rangeSlider.getModel().getExtent()) / valueMultiplier;
    }

    public double getMinSelected() {
        return rangeSlider.getLowerValue() / valueMultiplier;
    }

    private void refreshText() {
        left.setText(format.format(getMinSelected() * viewMultiplier));
        right.setText(format.format(getMaxSelected() * viewMultiplier));
    }

    @Override
    public void resultsChanged(D experiment, E sre, List<E> resultElements, ListSelectionModel selections) {
        if (!isRefreshing.getAndSet(true)) {
            try {
                int pmin = ((int) Math.floor(stats.getMin())) * (int) valueMultiplier;
                int pmax = ((int) Math.ceil(stats.getMax())) * (int) valueMultiplier;

                rangeSlider.setMinimum(pmin);
                rangeSlider.setLowerValue(pmin);

                rangeSlider.setMaximum(pmax);
                rangeSlider.setUpperValue(pmax);

                refreshText();

            } finally {
                isRefreshing.set(false);
            }

            firePropertyChange(RANGE_CHANGE, null, rangeSlider);
        }
    }
}
