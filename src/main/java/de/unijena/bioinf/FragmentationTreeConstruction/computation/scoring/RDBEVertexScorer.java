package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.ScoreName;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

@ScoreName("rdbe")
public class RDBEVertexScorer extends AbstractDecompositionScorer {

    private final RealDistribution distribution;
    private final double maxPenalty;

    public RDBEVertexScorer(RealDistribution distribution, double maxPenalty) {
        this.distribution = distribution;
        this.maxPenalty = maxPenalty;
    }

    public RDBEVertexScorer(RealDistribution distribution) {
        this(distribution, Math.log(0.001));
    }

    public RDBEVertexScorer(double mean, double variance) {
        this(new NormalDistribution(mean, Math.sqrt(variance)));
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input) {
        return Math.max(maxPenalty, Math.log(distribution.density(formula.rdbe())));
    }
}
