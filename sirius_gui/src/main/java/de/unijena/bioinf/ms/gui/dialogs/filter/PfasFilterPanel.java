package de.unijena.bioinf.ms.gui.dialogs.filter;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.filter.PfasFilter;
import eu.hansolo.rangeslider.RangeSlider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Selects a range of PFAS evidence, from features without any PFAS annotation to those with a PFAS
 * molecular structure.
 * <p>
 * Like {@link QualityFilterPanel} this is a range rather than a set of check boxes because the scale is
 * ordered: the levels above "None" are the values of the {@code pfas} tag, ordered by how strong the
 * evidence is. The full scale means every feature passes, i.e. no filter.
 */
class PfasFilterPanel extends JPanel {

    /** Space kept free between two neighbouring scale labels. */
    private static final int LABEL_GAP = 16;

    /** Used until (or unless) the search index tells us what the pfas tag means. */
    private static final String FALLBACK_DESCRIPTION = "Features SIRIUS flagged as potential PFAS.";

    private final RangeSlider slider;
    private final int lastIndex;
    private final Supplier<String> fieldDescription;

    /**
     * @param fieldDescription the description of the pfas tag field as reported by the search index
     *                         (may return null while the field list has not been fetched yet, which is
     *                         why the tooltip is composed on demand rather than once here)
     */
    public PfasFilterPanel(@NotNull PfasFilter pfasFilterModel, @NotNull Supplier<String> fieldDescription) {
        super();
        this.fieldDescription = fieldDescription;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        List<String> levels = pfasFilterModel.getPossibleLevels();
        lastIndex = levels.size() - 1;

        slider = new RangeSlider(0, lastIndex) {
            @Override
            public String getToolTipText() {
                return composeToolTip(); // the slider is the biggest hover target of the row
            }
        };
        slider.setSnapToTicks(true);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        // Register both explicitly (a parent tooltip does not show over a child): setToolTipText would
        // NOT register them, since it only does so when the previous text was null and our
        // getToolTipText() never returns null. The text itself comes from that getter, so a field
        // description that arrives later still shows up.
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().registerComponent(slider);

        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        int labelWidth = 0;
        for (int i = 0; i <= lastIndex; i++) {
            JLabel label = new JLabel(levels.get(i));
            labelWidth += label.getPreferredSize().width;
            labels.put(i, label);
        }
        slider.setLabelTable(labels);
        // the scale labels are wider than a slider's default preferred width, so they would overlap in a
        // narrow row: claim the width they need (plus breathing room between them) as the minimum
        Dimension scaleSize = new Dimension(labelWidth + LABEL_GAP * lastIndex, slider.getPreferredSize().height);
        slider.setMinimumSize(scaleSize);
        slider.setPreferredSize(scaleSize);

        setFromModel(pfasFilterModel);

        add(Box.createHorizontalStrut(10));
        add(slider);
        add(Box.createHorizontalStrut(10));
    }

    /**
     * What the pfas tag means (as described by the search index, else a fallback) plus what the scale
     * does. The dialog uses the same text for the row's name label, so hovering either explains it.
     */
    public String composeToolTip() {
        String description = fieldDescription.get();
        return GuiUtils.formatToolTip(
                description == null || description.isBlank() ? FALLBACK_DESCRIPTION : description,
                "Filter by how strong that evidence is; selecting the whole scale means no filter.",
                "None: no PFAS tag at all",
                "Potential: tagged as '" + PfasFilter.PfasEvidence.POTENTIAL.getTagValue() + "'",
                "Formula: tagged as '" + PfasFilter.PfasEvidence.MOLECULAR_FORMULA.getTagValue() + "'",
                "Structure: tagged as '" + PfasFilter.PfasEvidence.MOLECULAR_STRUCTURE.getTagValue() + "'");
    }

    @Override
    public String getToolTipText() {
        return composeToolTip();
    }

    public void reset() {
        setRange(0, lastIndex);
    }

    /** Sets the slider from the given filter's selection (mirrors the constructor). */
    public void setFromModel(@NotNull PfasFilter pfasFilter) {
        int lower = -1;
        int upper = -1;
        for (int i = 0; i <= lastIndex; i++) {
            if (pfasFilter.isLevelSelected(i)) {
                if (lower < 0)
                    lower = i;
                upper = i;
            }
        }
        // A slider can only express a contiguous range, so a non-contiguous selection - which the model
        // still allows - is widened to the range it spans; an empty selection falls back to the full scale.
        setRange(lower < 0 ? 0 : lower, upper < 0 ? lastIndex : upper);
    }

    public void updateModel(@NotNull PfasFilter pfasFilter) {
        int lower = slider.getLowerValue();
        // the upper thumb is read through the extent, as QualityFilterPanel does: RangeSlider only refreshes
        // its upperValue field in setUpperValue, so it goes stale once the user drags the slider
        int upper = lower + slider.getModel().getExtent();
        for (int i = 0; i <= lastIndex; i++)
            pfasFilter.setLevelSelected(i, i >= lower && i <= upper);
    }

    /** Runs {@code onChange} whenever the selected evidence range changes. */
    public void onChange(@NotNull Runnable onChange) {
        slider.addChangeListener(e -> {
            if (!slider.getValueIsAdjusting())
                onChange.run();
        });
    }

    private void setRange(int lower, int upper) {
        // Opening the range to the bottom first, then setting upper and lower - see QualityFilterPanel for
        // why the order matters (both setters clamp against the range that is currently set).
        slider.setValue(slider.getMinimum());
        slider.setUpperValue(upper);
        slider.setLowerValue(lower);
    }
}
