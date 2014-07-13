package de.unijena.bioinf.FragmentationTreeConstruction.model;

public class TreeScoring {

    private double rootScore;
    private double recalibrationBonus;
    private double overallScore;

    public TreeScoring() {
    }

    public double getRootScore() {
        return rootScore;
    }

    public void setRootScore(double rootScore) {
        this.rootScore = rootScore;
    }

    public double getRecalibrationBonus() {
        return recalibrationBonus;
    }

    public void setRecalibrationBonus(double recalibrationBonus) {
        this.recalibrationBonus = recalibrationBonus;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }
}
