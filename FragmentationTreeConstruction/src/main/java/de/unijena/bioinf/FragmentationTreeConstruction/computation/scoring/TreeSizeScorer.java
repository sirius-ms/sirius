package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.Arrays;
import java.util.List;

public class TreeSizeScorer implements PeakScorer {

    private double treeSizeScore;

    public TreeSizeScorer(double treeSizeScore) {
        this.treeSizeScore = treeSizeScore;
    }

    public double getTreeSizeScore() {
        return treeSizeScore;
    }

    public void setTreeSizeScore(double treeSizeScore) {
        this.treeSizeScore = treeSizeScore;
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        Arrays.fill(scores, treeSizeScore);
    }
}
