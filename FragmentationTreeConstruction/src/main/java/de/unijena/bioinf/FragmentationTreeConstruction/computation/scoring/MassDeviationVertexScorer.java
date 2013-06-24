package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.special.Erf;

/**
 * @author Kai Dührkop
 */
@Called("Mass Deviation")
public class MassDeviationVertexScorer implements DecompositionScorer<Object> {
    private final static double sqrt2 = Math.sqrt(2);
    private double massPenalty;
    private final boolean scoreRoot;

    public MassDeviationVertexScorer(boolean scoreRoot) {
        this(3, scoreRoot);
    }

    public MassDeviationVertexScorer(double massPenalty, boolean scoreRoot) {
        this.massPenalty = massPenalty;
        this.scoreRoot = scoreRoot;
    }


    public void setMassPenalty(double massPenalty) {
        this.massPenalty = massPenalty;
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
        if (peak.getOriginalPeaks().isEmpty()) return 0d; // don't score synthetic peaks
        final double theoreticalMass = formula.getMass();
        final double realMass = peak.getUnmodifiedMass();
        final MeasurementProfile profile = input.getExperimentInformation().getMeasurementProfile();
        final Deviation dev = profile.getStandardMs2MassDeviation();
        final double sd = dev.absoluteFor(realMass);
        return Math.log(Erf.erfc(Math.abs(realMass-theoreticalMass)/(sd * sqrt2)));
    }
}
