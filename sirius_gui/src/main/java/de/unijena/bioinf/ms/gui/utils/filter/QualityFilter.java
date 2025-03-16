package de.unijena.bioinf.ms.gui.utils.filter;

import de.unijena.bioinf.ms.persistence.model.core.DefaultQualityCategory;
import io.sirius.ms.sdk.model.DataQuality;
import lombok.Getter;
import org.apache.commons.text.CaseUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class QualityFilter {
    private final static List<DataQuality> DEFAULT_STATE = Arrays.asList(DataQuality.values()).subList(1, DataQuality.values().length);

    @Getter
    private final String name;
    @Getter
    private final String id;
    @Getter
    private final EnumSet<DataQuality> dataQualities = EnumSet.copyOf(DEFAULT_STATE);
    private final FeatureFilterModel featureFilterModel;

    public QualityFilter(DefaultQualityCategory category, FeatureFilterModel featureFilterModel) {
        this(category.name(), category.getDisplayName(), featureFilterModel);
    }

    public QualityFilter(String id, String name, FeatureFilterModel featureFilterModel) {
        this.name = name;
        this.id = id;
        this.featureFilterModel = featureFilterModel;
    }

    public boolean addQuality(int publicIndex) {
        return addQuality(DEFAULT_STATE.get(publicIndex));
    }

    public boolean addQuality(DataQuality quality) {
        if (quality == DataQuality.NOT_APPLICABLE) //no error thrown because this is GUI stuff, just silently ignore
            return false;

        if (dataQualities.add(quality)) {
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

        if (dataQualities.remove(quality)) {
            featureFilterModel.pcs().firePropertyChange(name, quality, null);
            return true;
        }
        return false;
    }

    public boolean isQualitySelected(int publicIndex) {
        return isQualitySelected(DEFAULT_STATE.get(publicIndex));
    }

    public boolean isQualitySelected(DataQuality quality) {
        return dataQualities.contains(quality);
    }

    public boolean setQualitySelected(int publicIndex, boolean selected) {
        if (selected)
            return addQuality(publicIndex);
        return removeQuality(publicIndex);
    }

    public boolean isEnabled() {
        return dataQualities.size() < DEFAULT_STATE.size();
    }

    public void reset() {
        dataQualities.clear();
        dataQualities.addAll(DEFAULT_STATE);
    }

    public List<String> getPossibleQualities() {
        return DEFAULT_STATE.stream()
                .map(dq -> CaseUtils.toCamelCase(dq.name(), true, '_', ' ', '\t'))
                .toList();
    }
}
