package de.unijena.bioinf.ms.gui.utils.filter;

import org.jetbrains.annotations.NotNull;

/**
 * A mutable view over the sample/blank fold-change facet of the {@link FeatureFilter} held by a
 * {@link FeatureFilterModel}, for the widgets that edit it in place. Reads go to the current value, writes
 * replace it.
 */
public class FoldChangeFilter {
    private final FeatureFilterModel model;

    FoldChangeFilter(@NotNull FeatureFilterModel model) {
        this.model = model;
    }

    /** The threshold the facet starts from (and resets to). */
    public double getMinFoldChange() {
        return FeatureFilter.DEFAULT_MIN_BLANK_FOLD_CHANGE;
    }

    public double getCurrentMinFoldChange() {
        return model.get().minBlankFoldChange();
    }

    public void setCurrentMinFoldChange(double currentMinFoldChange) {
        model.update(f -> f.withMinBlankFoldChange(currentMinFoldChange));
    }

    public boolean isEnabled() {
        return model.get().blankFoldChangeEnabled();
    }

    public void setEnabled(boolean enabled) {
        model.update(f -> f.withBlankFoldChangeEnabled(enabled));
    }

    public void reset() {
        model.update(f -> f.withBlankFoldChangeEnabled(false).withMinBlankFoldChange(getMinFoldChange()));
    }
}
