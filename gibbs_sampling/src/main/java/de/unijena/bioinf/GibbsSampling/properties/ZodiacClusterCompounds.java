package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * cluster compounds before running ZODIAC
 * */
@DefaultProperty
public class ZodiacClusterCompounds implements Ms2ExperimentAnnotation {

    public final boolean value;

    public ZodiacClusterCompounds(boolean value) {
        this.value = value;
    }

    public ZodiacClusterCompounds() {
        this(false);
    }
}
