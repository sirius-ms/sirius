package de.unijena.bioinf.FragmentationTreeConstruction.model;

public class Scoring {

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

}
