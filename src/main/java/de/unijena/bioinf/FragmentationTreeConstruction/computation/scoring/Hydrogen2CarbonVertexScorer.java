package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.ScoreName;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.distribution.RealDistribution;

/**
 * @author Kai Dührkop
 */
@ScoreName("hydrogen2c")
public class Hydrogen2CarbonVertexScorer extends AbstractDecompositionScorer {

    private final RealDistribution distribution;
    private final double maxPenalty;

    public Hydrogen2CarbonVertexScorer(RealDistribution distribution, double maxPenalty) {
        this.distribution = distribution;
        this.maxPenalty = maxPenalty;
    }

    public Hydrogen2CarbonVertexScorer(RealDistribution distribution) {
        this(distribution, Math.log(0.001));
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input) {
        return Math.max(maxPenalty, Math.log(distribution.density(formula.hydrogen2CarbonRatio())));
    }
}
