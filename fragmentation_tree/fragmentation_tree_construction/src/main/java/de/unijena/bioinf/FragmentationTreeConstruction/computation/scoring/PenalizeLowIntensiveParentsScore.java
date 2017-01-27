package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

public class PenalizeLowIntensiveParentsScore implements PeakPairScorer {

    private double penalty = Math.log(0.5);
    private double minMass = 100d;
    private double minIntensity = 0.02;
    private double ratio = 2;

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.penalty = document.getDoubleFromDictionary(dictionary, "penalty");
        this.penalty = document.getDoubleFromDictionary(dictionary, "ratio");
        this.minMass = document.getDoubleFromDictionary(dictionary, "minMass");
        this.ratio = document.getDoubleFromDictionary(dictionary, "ratio");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "penalty", penalty);
        document.addToDictionary(dictionary, "minMass", minMass);
        document.addToDictionary(dictionary, "minIntensity", minIntensity);
        document.addToDictionary(dictionary, "ratio", ratio);
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
        for (int fragment=0; fragment < peaks.size(); ++fragment) {
            double fragmentIntensity = peaks.get(fragment).getRelativeIntensity();
            for (int parent = fragment + 1; parent < peaks.size(); ++parent) {
                final double parentMass = peaks.get(parent).getMass();
                double parentIntensity = peaks.get(parent).getRelativeIntensity();
                if (parentMass > minMass && parentIntensity < minIntensity && fragmentIntensity/parentIntensity < ratio) {
                    scores[parent][fragment] += penalty;
                }
            }
        }
    }
}
