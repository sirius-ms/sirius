package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

public class ZodiacScore implements DataAnnotation {
    private double probability;

    public ZodiacScore(double probability) {
        this.probability = probability;
    }

    public double getProbability() {
        return probability;
    }
}
