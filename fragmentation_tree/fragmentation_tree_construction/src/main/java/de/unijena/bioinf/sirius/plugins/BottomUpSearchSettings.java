package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.sirius.ProcessedInput;

public class BottomUpSearchSettings implements Ms2ExperimentAnnotation {

    public final double enabledFromMass;
    public final double replaceDeNovoFromMass;

    protected final static BottomUpSearchSettings ALWAYS = new BottomUpSearchSettings(0,0),
    USE_DENOVO_FOR_LOW_MASSES = new BottomUpSearchSettings(0,400d),
            DISABLED = new BottomUpSearchSettings(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    public static BottomUpSearchSettings disabled() {
        return DISABLED;
    }

    @DefaultInstanceProvider
    public static BottomUpSearchSettings newInstance(@DefaultProperty(propertyKey = "enabledFromMass") double enabledFromMass, @DefaultProperty(propertyKey = "replaceDeNovoFromMass") double replaceDeNovoFromMass) {
        return new BottomUpSearchSettings(enabledFromMass, replaceDeNovoFromMass);
    }


    public BottomUpSearchSettings(double enabledFromMass, double replaceDeNovoFromMass) {
        this.enabledFromMass = enabledFromMass;
        this.replaceDeNovoFromMass = replaceDeNovoFromMass;
    }

    public boolean isEnabledFor(double ionMass) {
        return ionMass > enabledFromMass;
    }

    public boolean stillUseDeNovoFor(double mass) {
        return mass < replaceDeNovoFromMass;
    }
}
