package de.unijena.bioinf.ms.gui.dialogs.filter;

import eu.hansolo.rangeslider.RangeSlider;
import io.sirius.ms.sdk.model.DataQuality;
import org.apache.commons.text.CaseUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * A range slider over the selectable data qualities (LOWEST to GOOD; NOT_APPLICABLE is never on the slider),
 * editing one quality selection of a {@link de.unijena.bioinf.ms.gui.utils.filter.FeatureFilter} as a
 * contiguous range. The dialog binds it to the working value through {@link #getSelection()} and
 * {@link #setSelection(Set)}, see {@link FilterWidgetBindings#quality}.
 */
public class QualityFilterPanel extends JPanel {

    /** The selectable qualities in slider order: everything but NOT_APPLICABLE. */
    private static final List<DataQuality> QUALITIES = Arrays.asList(DataQuality.values()).subList(1, DataQuality.values().length);

    private final RangeSlider slider;
    private final int lastIndex;

    /** A slider spanning the whole scale (not filtering). */
    public QualityFilterPanel() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        lastIndex = QUALITIES.size() - 1;

        slider = new RangeSlider(0, lastIndex);
        slider.setSnapToTicks(true);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        for (int i = 0; i <= lastIndex; i++)
            labels.put(i, new JLabel(CaseUtils.toCamelCase(QUALITIES.get(i).name(), true, '_', ' ', '\t')));
        slider.setLabelTable(labels);

        reset();

        add(Box.createHorizontalStrut(10));
        add(slider);
        add(Box.createHorizontalStrut(10));
    }

    public void reset() {
        setRange(0, lastIndex);
    }

    /** The selected qualities as the slider shows them: the contiguous range between its two thumbs. */
    public Set<DataQuality> getSelection() {
        int lower = slider.getLowerValue();
        // the upper thumb is read through the extent, as FilterRangeSlider does: RangeSlider only refreshes its
        // upperValue field in setUpperValue, so it goes stale once the user drags the slider
        int upper = lower + slider.getModel().getExtent();
        EnumSet<DataQuality> selection = EnumSet.noneOf(DataQuality.class);
        for (int i = lower; i <= upper && i <= lastIndex; i++)
            selection.add(QUALITIES.get(i));
        return selection;
    }

    /**
     * Shows a selection. A slider can only express a contiguous range: a selection with a gap - which the
     * filter still allows, e.g. from a typed query - is widened to the span it covers rather than silently
     * dropping the ends. An empty selection falls back to the full range.
     */
    public void setSelection(@NotNull Set<DataQuality> selection) {
        int lower = -1;
        int upper = -1;
        for (int i = 0; i <= lastIndex; i++) {
            if (selection.contains(QUALITIES.get(i))) {
                if (lower < 0)
                    lower = i;
                upper = i;
            }
        }
        setRange(lower < 0 ? 0 : lower, upper < 0 ? lastIndex : upper);
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
