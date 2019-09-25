package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

@Deprecated
public class Ms2IsotopePatternMatch implements DataAnnotation {

    private SimpleSpectrum simulated;
    private SimpleSpectrum matched;
    private double score;

    public Ms2IsotopePatternMatch(SimpleSpectrum simulated, SimpleSpectrum matched, double score) {
        this.simulated = simulated;
        this.matched = matched;
        this.score = score;
    }

    public SimpleSpectrum getMatched() {
        return matched;
    }

    public SimpleSpectrum getSimulated() {
        return simulated;
    }

    public double getScore() {
        return score;
    }
}
