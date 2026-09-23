package de.unijena.bioinf.ms.gui.utils.filter;

import de.unijena.bioinf.ChemistryBase.utils.DefaultQualityCategory;
import io.sirius.ms.sdk.model.DataQuality;
import lombok.Getter;
import org.apache.commons.text.CaseUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A mutable view over one quality selection of the {@link FeatureFilter} held by a {@link FeatureFilterModel}
 * - the overall feature quality ({@code category == null}) or one {@link DefaultQualityCategory} - for the
 * widgets that edit a selection in place. Reads go to the current value, writes replace it.
 */
public class QualityFilter {
    /** The selectable qualities in slider order (everything but NOT_APPLICABLE). */
    private final static List<DataQuality> DEFAULT_STATE = Arrays.asList(DataQuality.values()).subList(1, DataQuality.values().length);

    @Getter
    private final String name;
    @Getter
    private final String id;
    @Nullable
    private final DefaultQualityCategory category;
    private final FeatureFilterModel featureFilterModel;

    public QualityFilter(@Nullable DefaultQualityCategory category, FeatureFilterModel featureFilterModel) {
        this(category == null ? null : category.name(), category == null ? "Feature Quality" : category.getDisplayName(), featureFilterModel);
    }

    /** {@code id} names a {@link DefaultQualityCategory}; any other id (or null) means the overall feature quality. */
    public QualityFilter(@Nullable String id, String name, FeatureFilterModel featureFilterModel) {
        this.name = name;
        this.id = id;
        this.category = categoryOf(id);
        this.featureFilterModel = featureFilterModel;
    }

    @Nullable
    private static DefaultQualityCategory categoryOf(@Nullable String id) {
        if (id == null)
            return null;
        try {
            return DefaultQualityCategory.valueOf(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Set<DataQuality> selection() {
        FeatureFilter filter = featureFilterModel.get();
        return category == null ? filter.featureQuality() : filter.categorizedQuality().get(category);
    }

    private void select(@NotNull Set<DataQuality> qualities) {
        featureFilterModel.update(f -> category == null
                ? f.withFeatureQuality(qualities)
                : f.withCategorizedQuality(category, qualities));
    }

    /** The current selection as a fresh, modifiable copy; changes to it do not reach the filter. */
    public EnumSet<DataQuality> getDataQualities() {
        Set<DataQuality> selection = selection();
        return selection.isEmpty() ? EnumSet.noneOf(DataQuality.class) : EnumSet.copyOf(selection);
    }

    public boolean addQuality(int publicIndex) {
        return addQuality(DEFAULT_STATE.get(publicIndex));
    }

    public boolean addQuality(DataQuality quality) {
        if (quality == DataQuality.NOT_APPLICABLE) //no error thrown because this is GUI stuff, just silently ignore
            return false;

        EnumSet<DataQuality> qualities = getDataQualities();
        if (qualities.add(quality)) {
            select(qualities);
            featureFilterModel.pcs().firePropertyChange(name, null, quality);
            return true;
        }
        return false;
    }

    public boolean removeQuality(int publicIndex) {
        return removeQuality(DEFAULT_STATE.get(publicIndex));
    }

    public boolean removeQuality(DataQuality quality) {
        if (quality == DataQuality.NOT_APPLICABLE) //no error thrown because this is GUI stuff, just silently ignore
            return false;

        EnumSet<DataQuality> qualities = getDataQualities();
        if (qualities.remove(quality)) {
            select(qualities);
            featureFilterModel.pcs().firePropertyChange(name, quality, null);
            return true;
        }
        return false;
    }

    public boolean isQualitySelected(int publicIndex) {
        return isQualitySelected(DEFAULT_STATE.get(publicIndex));
    }

    public boolean isQualitySelected(DataQuality quality) {
        return selection().contains(quality);
    }

    public boolean setQualitySelected(int publicIndex, boolean selected) {
        if (selected)
            return addQuality(publicIndex);
        return removeQuality(publicIndex);
    }

    /** Whether the selection filters at all, i.e. is a strict subset of the selectable qualities. */
    public boolean isEnabled() {
        return selection().size() < DEFAULT_STATE.size();
    }

    /** Selects every quality: the filter is off. */
    public void reset() {
        select(FeatureFilter.ALL_QUALITIES);
    }

    public List<String> getPossibleQualities() {
        return DEFAULT_STATE.stream()
                .map(dq -> CaseUtils.toCamelCase(dq.name(), true, '_', ' ', '\t'))
                .toList();
    }
}
