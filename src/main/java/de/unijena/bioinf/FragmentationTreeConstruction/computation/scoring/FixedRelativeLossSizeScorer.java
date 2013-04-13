package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.ScoreName;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * @author Kai Dührkop
 */
@ScoreName("loss size")
public class FixedRelativeLossSizeScorer implements MS2ClosureScorer {

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
        for (int i=0; i < peaks.size(); ++i) {
            for (int j=0; j < peaks.size(); ++j) {
                final double parentMass = peaks.get(i).getMz();
                final double fragmentMass = peaks.get(j).getMz();
                final double lossMass = parentMass-fragmentMass;
                scores[i][j] +=  (lossMass < 0) ? Double.NEGATIVE_INFINITY : Math.log(1 - (lossMass / parentMass));
            }
        }
    }
}
