package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.special.Erf;

public class MassDeviationScorer implements IsotopePatternScorer {

    private final static double root2 = Math.sqrt(2d);

    private final double massDeviationPenalty;
    private final IntensityDependency intensityDependency;

    public MassDeviationScorer(double massDeviationPenalty, double lowestIntensityAccuracy) {
        this(massDeviationPenalty, new LinearIntensityDependency(1d, lowestIntensityAccuracy));
    }

    public MassDeviationScorer(double massDeviationPenalty, IntensityDependency intensityDependency) {
        this.massDeviationPenalty = massDeviationPenalty;
        this.intensityDependency  = intensityDependency;
    }

    public MassDeviationScorer(double lowestIntensityAccuracy) {
        this(3, lowestIntensityAccuracy);
    }

    @Override
    public double score(Spectrum<Peak> measured, Spectrum<Peak> theoretical, Normalization norm, MsExperiment experiment) {
        if (measured.size() > theoretical.size()) throw new IllegalArgumentException("Theoretical spectrum is smaller than measured spectrum");
        // remove peaks from theoretical pattern until the length of both spectra is equal
        final MutableSpectrum<Peak> theoreticalSpectrum = new SimpleMutableSpectrum(theoretical);
        while (measured.size() < theoreticalSpectrum.size()) {
            theoreticalSpectrum.removePeakAt(theoreticalSpectrum.size()-1);
        }
        // re-normalize
        Spectrums.normalize(theoreticalSpectrum, Normalization.Sum(1));
        final double mz0 = measured.getMzAt(0);
        final double thMz0 = theoreticalSpectrum.getMzAt(0);
        final double int0 = norm.rescale(measured.getIntensityAt(0));
        double score = Math.log(Erf.erfc(Math.abs(thMz0 - mz0)/
                (root2*(1d/(massDeviationPenalty) * experiment.getMeasurementProfile().getExpectedIonMassDeviation().getPpm() *  intensityDependency.getValueAt(int0) * 1e-6 * mz0))));
        for (int i=1; i < measured.size(); ++i) {
            final double mz = measured.getMzAt(i) - mz0;
            final double thMz = theoreticalSpectrum.getMzAt(i) - thMz0;
            final double thIntensity = norm.rescale(measured.getIntensityAt(i));
            // TODO: thMz hier richtig?
            final double sd = 1d/massDeviationPenalty * experiment.getMeasurementProfile().getExpectedMassDifferenceDeviation().getPpm() * intensityDependency.getValueAt(thIntensity) * 1e-6 * measured.getMzAt(i);
            score += Math.log(Erf.erfc(Math.abs(thMz - mz)/(root2*sd)));
        }
        return score;
    }
}
