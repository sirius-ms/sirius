package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

public class RelativeLossSizeScorer implements PeakPairScorer {
    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
        for (int i=0; i < peaks.size(); ++i) {
            for (int j=i+1; j < peaks.size(); ++j) {
                final double parentMass = peaks.get(j).getMass();
                scores[j][i] = Math.log(1d-(parentMass-peaks.get(i).getMass())/parentMass);
            }
        }
    }
}
