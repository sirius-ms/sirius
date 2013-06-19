package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.ms.MsExperiment;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.special.Erf;

public class MassDifferenceDeviationScorer implements IsotopePatternScorer {

    private final static double root2 = Math.sqrt(2d);

    private final double massDeviationPenalty;
    private final IntensityDependency dependency;

    public MassDifferenceDeviationScorer(double massDeviationPenalty, double lowestIntensityAccuracy) {
        this(massDeviationPenalty, new LinearIntensityDependency(1d, lowestIntensityAccuracy));
    }

    public MassDifferenceDeviationScorer(double massDeviationPenalty, IntensityDependency dependency) {
        this.massDeviationPenalty = massDeviationPenalty;
        this.dependency = dependency;
    }

    @Override
    public double score(Spectrum<Peak> measured, Spectrum<Peak> theoretical, Normalization norm, MsExperiment experiment) {
        final double mz0 = measured.getMzAt(0);
        final double thMz0 = theoretical.getMzAt(0);
        double score = 0d;
        for (int i=0; i < measured.size(); ++i) {
            final double mz = measured.getMzAt(i) - (i==0 ? 0 : measured.getMzAt(i-1));
            final double thMz = theoretical.getMzAt(i) - (i==0 ? 0 : theoretical.getMzAt(i-1));
            final double intensity = norm.rescale(measured.getIntensityAt(i));
            // TODO: thMz hier richtig?
            final double sd = 1d/massDeviationPenalty * experiment.getMeasurementProfile().getExpectedMassDifferenceDeviation().getPpm() * dependency.getValueAt(intensity) * 1e-6 * measured.getMzAt(i);
            score += Math.log(Erf.erfc(Math.abs(thMz - mz)/(root2*sd)));
        }
        return score;
    }
}
