package de.unijena.bioinf.ChemistryBase.ms.ft;

import gnu.trove.decorator.TObjectDoubleMapDecorator;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class TreeScoring {

    private double rootScore;
    private double recalibrationBonus;
    private double overallScore;
    private TObjectDoubleHashMap<String> additionalScores;

    public TreeScoring() {
        this.additionalScores = null;
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

    public Map<String, Double> getAdditionalScores() {
        if (additionalScores==null) return Collections.emptyMap();
        else return new TObjectDoubleMapDecorator<String>(additionalScores);
    }

    public double getAdditionalScore(String name) {
        if (additionalScores==null) return 0d;
        else return additionalScores.get(name);
    }

    public void addAdditionalScore(String name, double value) {
        if (additionalScores==null) additionalScores = new TObjectDoubleHashMap<String>(2, 0.75f, 0);
        overallScore -= additionalScores.get(name);
        additionalScores.put(name, value);
        overallScore += value;
    }

    public double getAdditionalScoresSum() {
        if (additionalScores==null) return 0d;
        double sum=0d;
        for (String k : additionalScores.keySet())
            sum += additionalScores.get(k);
        return sum;
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
