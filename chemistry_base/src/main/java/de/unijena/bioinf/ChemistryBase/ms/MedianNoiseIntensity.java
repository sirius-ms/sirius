package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

@DefaultProperty
public class MedianNoiseIntensity implements Ms2ExperimentAnnotation {
    public final double value;

    public MedianNoiseIntensity(double value) {
        this.value = value;
    }

    MedianNoiseIntensity() {
        this(Double.NaN);
    }
}
