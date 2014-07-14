package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

import java.util.Comparator;

public class TreeScoring {

    private double rootScore;
    private double recalibrationBonus;
    private double overallScore;

    public TreeScoring() {
    }

    public static Comparator<FTree> OrderedByscoreAsc() {
        return new Comparator<FTree>() {
            @Override
            public int compare(FTree o1, FTree o2) {
                return new Double(o1.getAnnotationOrThrow(TreeScoring.class).getOverallScore()).compareTo(o2.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
            }
        };
    }

    public static Comparator<FTree> OrderedByscoreDesc() {
        return new Comparator<FTree>() {
            @Override
            public int compare(FTree o1, FTree o2) {
                return new Double(o2.getAnnotationOrThrow(TreeScoring.class).getOverallScore()).compareTo(o1.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
            }
        };
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
