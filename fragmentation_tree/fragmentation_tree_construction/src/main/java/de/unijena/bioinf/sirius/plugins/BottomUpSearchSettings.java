package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class BottomUpSearchSettings implements Ms2ExperimentAnnotation {
    public final boolean enabled;

    protected final static BottomUpSearchSettings ENABLED = new BottomUpSearchSettings(true), DISABLED = new BottomUpSearchSettings(false);

    public static BottomUpSearchSettings enabled() {
        return ENABLED;
    }
    public static BottomUpSearchSettings disabled() {
        return DISABLED;
    }

    @DefaultInstanceProvider
    public static BottomUpSearchSettings newInstance(@DefaultProperty boolean enabled) {
        return enabled ? ENABLED : DISABLED;
    }


    public BottomUpSearchSettings(boolean enabled) {
        this.enabled = enabled;
    }
}
