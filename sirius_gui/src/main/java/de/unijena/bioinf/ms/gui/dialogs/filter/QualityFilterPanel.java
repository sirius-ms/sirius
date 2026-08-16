package de.unijena.bioinf.ms.gui.dialogs.filter;

import de.unijena.bioinf.ms.gui.utils.filter.QualityFilter;
import eu.hansolo.rangeslider.RangeSlider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Hashtable;
import java.util.List;

/**
 * Selects a range of data qualities, from the lowest to the highest that should still pass the filter.
 * <p>
 * Quality is an ordered scale, so a range says what a set of check boxes could only imply: everything from here
 * to there. The scale shown is the one the user can reason about - Lowest to Good; the NOT_APPLICABLE state is
 * deliberately absent, since it does not mean bad quality but that there was nothing to judge. Features in that
 * state always pass, which the query builder takes care of by adding the term unconditionally.
 */
class QualityFilterPanel extends JPanel {

    private final RangeSlider slider;
    private final int lastIndex;

    public QualityFilterPanel(@NotNull QualityFilter qualityFilterModel) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        List<String> qualities = qualityFilterModel.getPossibleQualities();
        lastIndex = qualities.size() - 1;

        slider = new RangeSlider(0, lastIndex);
        slider.setSnapToTicks(true);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        for (int i = 0; i <= lastIndex; i++)
            labels.put(i, new JLabel(qualities.get(i)));
        slider.setLabelTable(labels);

        setFromModel(qualityFilterModel);

        add(Box.createHorizontalStrut(10));
        add(slider);
        add(Box.createHorizontalStrut(10));
    }

    public void reset() {
        setRange(0, lastIndex);
    }

    /** Sets the slider from the given quality filter's selection (mirrors the constructor). */
    public void setFromModel(@NotNull QualityFilter qualityFilter) {
        int lower = -1;
        int upper = -1;
        for (int i = 0; i <= lastIndex; i++) {
            if (qualityFilter.isQualitySelected(i)) {
                if (lower < 0)
                    lower = i;
                upper = i;
            }
        }
        // A slider can only express a contiguous range. A selection that is not contiguous - which the model
        // still allows, e.g. from a typed query - is widened to the range it spans rather than silently
        // dropping the ends. An empty selection falls back to the full range.
        setRange(lower < 0 ? 0 : lower, upper < 0 ? lastIndex : upper);
    }

    public void updateModel(QualityFilter qualityFilter) {
        int lower = slider.getLowerValue();
        // the upper thumb is read through the extent, as FilterRangeSlider does: RangeSlider only refreshes its
        // upperValue field in setUpperValue, so it goes stale once the user drags the slider
        int upper = lower + slider.getModel().getExtent();
        for (int i = 0; i <= lastIndex; i++)
            qualityFilter.setQualitySelected(i, i >= lower && i <= upper);
    }

    /** Runs {@code onChange} whenever the selected quality range changes. */
    public void onChange(@NotNull Runnable onChange) {
        slider.addChangeListener(e -> {
            if (!slider.getValueIsAdjusting())
                onChange.run();
        });
    }

    private void setRange(int lower, int upper) {
        // Both setters clamp against the range that is currently set, so neither order alone can move the range
        // freely: setValue caps the new lower bound at the current upper one (value + extent), and setUpperValue
        // derives the extent from the lower bound as it is at that moment. A fresh JSlider(0, n) starts at
        // value = n/2 with extent 0, so setting a higher range first would silently collapse onto that.
        // Opening the range to the bottom first removes the cap, then upper and lower can be set exactly.
        slider.setValue(slider.getMinimum());
        slider.setUpperValue(upper);
        slider.setLowerValue(lower);
    }
}
