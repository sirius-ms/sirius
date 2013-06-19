package de.unijena.bioinf.IsotopePatternAnalysis.scoring;


import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.special.Erf;

import static java.lang.Math.*;

public class LogNormDistributedIntensityScorer implements IsotopePatternScorer {

    private final static double root2 = sqrt(2d);

    private final double intensityDeviationPenalty;
    private final IntensityDependency intensityDependency;

    public LogNormDistributedIntensityScorer(double intensityDeviationPenalty, IntensityDependency intensityDependency) {
        this.intensityDeviationPenalty = intensityDeviationPenalty;
        this.intensityDependency = intensityDependency;
    }

    public LogNormDistributedIntensityScorer(double intensityDeviationPenalty, double fullIntensityPrecision, double minIntensityPrecision) {
        this(intensityDeviationPenalty, new LinearIntensityDependency(fullIntensityPrecision, minIntensityPrecision));
    }

    public LogNormDistributedIntensityScorer() {
        this(3, 0.1, 0.9);
    }

    public LogNormDistributedIntensityScorer(double fullIntensityPrecision, double minIntensityPrecision) {
        this(3, fullIntensityPrecision, minIntensityPrecision);
    }


    @Override
    public double score(Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalPattern, Normalization norm, MsExperiment experiment) {
        // remove peaks from theoretical pattern until the length of both spectra is equal
        final MutableSpectrum<Peak> theoreticalSpectrum = new SimpleMutableSpectrum(theoreticalPattern);
        while (measuredSpectrum.size() < theoreticalSpectrum.size()) {
            theoreticalSpectrum.removePeakAt(theoreticalSpectrum.size()-1);
        }
        // re-normalize
        Spectrums.normalize(theoreticalSpectrum, Normalization.Sum(1));
        final Spectrum<? extends Peak> measured =
                (norm.equals(Normalization.Sum(1))) ? measuredSpectrum
                                                    : Spectrums.getNormalizedSpectrum(measuredSpectrum, norm);
        // score
        double score = 0d;
        final double intensityDeviation = experiment.getMeasurementProfile().getExpectedIntensityDeviation();
        for (int i=0; i < measured.size(); ++i) {
            final double intensity = measured.getIntensityAt(i);
            final double thIntensity = theoreticalSpectrum.getIntensityAt(i);
            final double sd = 1d/intensityDeviationPenalty * log(1 + intensityDeviation*intensityDependency.getValueAt(intensity));
            score += log(Erf.erfc(abs(log(thIntensity / intensity)/(root2*sd))));
        }
        return score;
    }
}
