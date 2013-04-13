package de.unijena.bioinf.IsotopePatternAnalysis.scoring;


import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.special.Erf;

import static java.lang.Math.*;

public class LogNormDistributedIntensityScorer<P extends Peak, T extends Spectrum<P>> implements IsotopePatternScorer<P, T> {

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
    public double score(T measuredSpectrum, T theoreticalPattern, Normalization norm) {
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
        for (int i=0; i < measured.size(); ++i) {
            final double intensity = measured.getIntensityAt(i);
            final double thIntensity = theoreticalSpectrum.getIntensityAt(i);
            final double sd = 1d/intensityDeviationPenalty * log(1 + intensityDependency.getValueAt(intensity));
            score += log(Erf.erfc(abs(log(thIntensity / intensity)/(root2*sd))));
        }
        return score;
    }
}
