package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.math.RealDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.special.Erf;

/**
 * @author Kai Dührkop
 */

public class MassDeviationVertexScorer implements DecompositionScorer<Object> {
    private final static double sqrt2 = Math.sqrt(2);
    private final double massPenalty;
    private final double sigmaquot;

    public MassDeviationVertexScorer() {
        this(3);
    }

    public MassDeviationVertexScorer(double massPenalty) {
        this.massPenalty = massPenalty;
        this.sigmaquot = 1d/massPenalty;
    }

    public double getMassPenalty() {
        return massPenalty;
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object _) {
        final double theoreticalMass = formula.getMass();
        final double realMass = peak.getUnmodifiedMass();
        final double sd = sigmaquot * input.getExperimentInformation().getMassError().absoluteFor(theoreticalMass);
        return Math.log(Erf.erfc(Math.abs(realMass-theoreticalMass)/(sd * sqrt2)));
    }
}
