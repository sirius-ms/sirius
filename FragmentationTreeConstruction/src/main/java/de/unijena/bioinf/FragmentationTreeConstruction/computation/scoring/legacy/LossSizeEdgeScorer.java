package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakPairScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.ScoreName;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

import java.util.List;

/**
 * @author Kai Dührkop
 */
@ScoreName("loss size")
public class LossSizeEdgeScorer implements PeakPairScorer {

    private final static double LEARNED_LAMBDA = 0.03223324;

    private final RealDistribution distribution;

    public LossSizeEdgeScorer(RealDistribution distribution) {
        this.distribution = distribution;
    }

    public LossSizeEdgeScorer() {
        this(LEARNED_LAMBDA);
    }

    public LossSizeEdgeScorer(double lambda) {
        this.distribution = new ExponentialDistribution(1d/lambda);
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
        for (int i=0; i < peaks.size(); ++i) {
            for (int j=0; j < peaks.size(); ++j) {
                final double parentMass = peaks.get(i).getMz();
                final double fragmentMass = peaks.get(j).getMz();
                final double lossMass = parentMass-fragmentMass;
                scores[i][j] +=  (lossMass < 0) ? Double.NEGATIVE_INFINITY : Math.log(distribution.density(lossMass));
            }
        }
    }
}
