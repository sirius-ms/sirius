package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakPairScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.ScoreName;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;
@ScoreName("loss size")
public class MixedLossSizeScorer implements PeakPairScorer {

    private final double lambda;

    public MixedLossSizeScorer(double lambda) {
        this.lambda = lambda;
    }

    public MixedLossSizeScorer(){
        this(0.005);
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
        for (int i=0; i < peaks.size(); ++i) {
            for (int j=0; j < peaks.size(); ++j) {
                final double parentMass = peaks.get(i).getMz();
                final double fragmentMass = peaks.get(j).getMz();
                final double lossMass = parentMass-fragmentMass;
                scores[i][j] +=  (lossMass < 0) ? Double.NEGATIVE_INFINITY : Math.max(
                        Math.log(1 - (lossMass / parentMass)),
                        -lambda * lossMass
                );
            }
        }
    }

}
