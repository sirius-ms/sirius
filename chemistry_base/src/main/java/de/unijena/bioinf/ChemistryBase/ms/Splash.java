package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

public final class Splash implements Ms2ExperimentAnnotation {

    private final String splash;

    public Splash(String splash) {
        this.splash = splash;
    }

    public String getSplash() {
        return splash;
    }

}
