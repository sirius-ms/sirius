package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class IsotopeMs2Settings implements Ms2ExperimentAnnotation {

    public static enum Strategy {
        IGNORE,FILTER,SCORE;
    }

    @DefaultProperty
    public final Strategy strategy;

    public IsotopeMs2Settings() {
        this.strategy = Strategy.IGNORE;
    }
    public IsotopeMs2Settings(Strategy strategy) {
        this.strategy = strategy;
    }
}
