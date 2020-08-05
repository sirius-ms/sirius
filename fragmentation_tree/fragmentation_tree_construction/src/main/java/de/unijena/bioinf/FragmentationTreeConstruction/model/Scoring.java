
package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

public class Scoring implements DataAnnotation {

    private double[] peakScores;
    private double[][] peakPairScores;

    public Scoring() {

    }

    public void initializeScoring(int numberOfPeaks) {
        this.peakPairScores = new double[numberOfPeaks][numberOfPeaks];
        this.peakScores = new double[numberOfPeaks];
    }

    public double[] getPeakScores() {
        return peakScores;
    }

    public double[][] getPeakPairScores() {
        return peakPairScores;
    }

    @Override
    public Scoring clone() {
        final Scoring copy = new Scoring();
        copy.peakPairScores = peakPairScores.clone();
        copy.peakScores = peakScores.clone();
        return copy;
    }

}
