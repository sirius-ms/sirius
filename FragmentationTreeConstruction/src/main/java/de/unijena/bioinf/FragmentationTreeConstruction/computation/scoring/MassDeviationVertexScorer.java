package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
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

    public MassDeviationVertexScorer() {
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

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        // nothing ^^
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        // nothing ^^
    }
}
