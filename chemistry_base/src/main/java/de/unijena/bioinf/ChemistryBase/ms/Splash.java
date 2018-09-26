package de.unijena.bioinf.ChemistryBase.ms;

public final class Splash implements Ms2ExperimentAnnotation {

    private final String splash;

    public Splash(String splash) {
        this.splash = splash;
    }

    public String getSplash() {
        return splash;
    }

}
