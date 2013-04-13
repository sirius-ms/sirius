package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.special.Erf;

/**
 * @author Kai Dührkop
 */

public class MassDeviationVertexScorer implements DecompositionScorer<double[]> {

    private final static double sqrt2 = Math.sqrt(2);

    private double lambda;
    private final double massDeviationPenalty;

    public MassDeviationVertexScorer(double massDeviationPenalty, double lambda) {
        this.massDeviationPenalty = 1d/massDeviationPenalty;
        this.lambda = lambda;
    }

    @Override
    public double[] prepare(ProcessedInput input) {
        final double[] noiseProbabilities = new double[input.getMergedPeaks().size()];
        for (ProcessedPeak p : input.getMergedPeaks()) {
            noiseProbabilities[p.getIndex()] = -lambda*p.getRelativeIntensity();
        }
        return noiseProbabilities;
    }

    public double getLambda() {
        return lambda;
    }

    public double getMassDeviationPenalty() {
        return massDeviationPenalty;
    }

    public double computeScore(MolecularFormula formula, ProcessedPeak peak, double noiseProbability, ProcessedInput input) {
        final double theoreticalMass = formula.getMass();
        final double realMass = peak.getUnmodifiedMass();
        final double sd = massDeviationPenalty * input.getExperimentInformation().getMassError().absoluteFor(theoreticalMass);
        double sco= Math.log(Erf.erfc(Math.abs(realMass-theoreticalMass)/(sd * sqrt2)));
        return sco- noiseProbability;
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, double[] noiseProbabilities) {
        return computeScore(formula, peak, noiseProbabilities[peak.getIndex()], input);
    }
}
