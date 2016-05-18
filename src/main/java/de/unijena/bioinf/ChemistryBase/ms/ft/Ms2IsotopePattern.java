package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.ms.Peak;

public class Ms2IsotopePattern {

    private Peak[] peaks;
    private double score;

    public Ms2IsotopePattern(Peak[] peaks, double score) {
        this.peaks = peaks;
        this.score = score;
    }


    public Peak[] getPeaks() {
        return peaks;
    }

    public double getScore() {
        return score;
    }
}
