package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.ms.properties.PropertyManager;

@DefaultProperty
public class MedianNoiseIntensity implements Ms2ExperimentAnnotation {
    public final static MedianNoiseIntensity DEFAULT(){
        return PropertyManager.DEFAULTS.createInstanceWithDefaults(MedianNoiseIntensity.class);
    }

    public final double value;

    public MedianNoiseIntensity(double value) {
        this.value = value;
    }

    private MedianNoiseIntensity() {
        this(Double.NaN);
    }
}
