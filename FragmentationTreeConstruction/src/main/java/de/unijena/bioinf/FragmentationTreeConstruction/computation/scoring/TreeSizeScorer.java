package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.ParameterHelper;
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
        for (int i=0; i < peaks.size(); ++i) {
            scores[i] += treeSizeScore;
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        treeSizeScore = document.getDoubleFromDictionary(dictionary, "score");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "score", treeSizeScore);
    }
}
