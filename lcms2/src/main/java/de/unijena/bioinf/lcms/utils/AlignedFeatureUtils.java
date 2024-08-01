package de.unijena.bioinf.lcms.utils;

import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractFeature;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;

import java.util.List;

public class AlignedFeatureUtils {

    public static List<Feature> selectRepresentativeFeatures(List<Feature> features) {
        double maxInt = features.stream().mapToDouble(AbstractFeature::getApexIntensity).max().orElse(0d);
        double threshold = maxInt/3d;
        return features.stream().filter(x->x.getApexIntensity()>=threshold).toList();

    }

}
