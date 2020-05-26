package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

@DefaultProperty
public class IsotopeMs2Settings implements Ms2ExperimentAnnotation {

    public static enum Strategy {
        IGNORE,FILTER,SCORE;
    }

    public final Strategy value;

    public IsotopeMs2Settings() {
        this.value = Strategy.IGNORE;
    }
    public IsotopeMs2Settings(Strategy strategy) {
        this.value = strategy;
    }
}
