package de.unijena.bioinf.sirius.gui.table;

import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;
import eu.hansolo.rangeslider.RangeSlider;
import org.jdesktop.beans.AbstractBean;
import oshi.jna.platform.unix.solaris.LibKstat;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

public abstract class FilterRangeSlider<L extends ActionList<E, D>, E extends AbstractBean, D> extends JPanel implements ActiveElementChangedListener<E, D> {
    public static final String DEFAUTL_INT_FORMAT = "##0";
    public static final String DEFAUTL_DOUBLE_FORMAT = "##0.00";

    public final boolean percentage;
    protected final RangeSlider rangeSlider;
    protected final JLabel left, right;
    protected final DecimalFormat format;
    protected boolean isRefreshing;
    protected final DoubleListStats stats;

    private final double valueMultiplier;
    private final double viewMultiplier;
    private final int numberLength;


    public FilterRangeSlider(L source) {
        this(source, false, DEFAUTL_DOUBLE_FORMAT);
    }

    public FilterRangeSlider(L source, boolean percentage) {
        this(source, percentage, DEFAUTL_DOUBLE_FORMAT);
    }

    public FilterRangeSlider(L source, boolean percentage, String decimalFormat) {
        this.percentage = percentage;
        stats = getDoubleListStats(source);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());

        rangeSlider = new RangeSlider(Integer.MIN_VALUE, Integer.MAX_VALUE);
        rangeSlider.setLowerValue(rangeSlider.getMinimum());
        rangeSlider.setUpperValue(rangeSlider.getMaximum());
        rangeSlider.setThumbShape(RangeSlider.ThumbShape.DROP);
        rangeSlider.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!isRefreshing)
                    refreshText();
            }
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

    }

    public void addChangeListener(ChangeListener listener) {
        rangeSlider.addChangeListener(listener);
    }

    protected abstract DoubleListStats getDoubleListStats(L list);

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
        if (!isRefreshing) {
            isRefreshing = true;
            try {
                int pmin = ((int) Math.floor(stats.getMin())) * (int) valueMultiplier;
                int pmax = ((int) Math.ceil(stats.getMax())) * (int) valueMultiplier;

                rangeSlider.setMinimum(pmin);
                rangeSlider.setLowerValue(pmin);

                rangeSlider.setMaximum(pmax);
                rangeSlider.setUpperValue(pmax);


                refreshText();

            } finally {
                isRefreshing = false;
            }
        }
    }
}
