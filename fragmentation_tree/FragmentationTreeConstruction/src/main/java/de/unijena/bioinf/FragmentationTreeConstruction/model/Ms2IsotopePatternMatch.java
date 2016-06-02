package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

public class Ms2IsotopePatternMatch {

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
