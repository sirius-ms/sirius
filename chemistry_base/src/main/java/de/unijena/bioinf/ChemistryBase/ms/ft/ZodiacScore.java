package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

public class ZodiacScore implements TreeAnnotation {
    private final double probability;

    public ZodiacScore(double probability) {
        this.probability = probability;
    }

    public double getProbability() {
        return probability;
    }
}
