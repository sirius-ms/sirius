package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternScorer;
import org.apache.commons.math3.special.Erf;

public class MassDifferenceDeviationScorer<P extends Peak, T extends Spectrum<P>> implements IsotopePatternScorer<P, T> {

    private final static double root2 = Math.sqrt(2d);

    private final double massDeviationPenalty;
    private final double ppmFullIntensity, ppmLowestIntensity;

    public MassDifferenceDeviationScorer(double massDeviationPenalty, double ppmFullIntensity, double ppmLowestIntensity) {
        this.massDeviationPenalty = massDeviationPenalty;
        this.ppmFullIntensity = ppmFullIntensity;
        this.ppmLowestIntensity = ppmLowestIntensity;
    }

    public MassDifferenceDeviationScorer(double ppmHighestIntensity, double ppmLowestIntensity) {
        this(3, ppmHighestIntensity, ppmLowestIntensity);
    }

    @Override
    public double score(T measured, T theoretical, Normalization norm) {
        final double mz0 = measured.getMzAt(0);
        final double thMz0 = theoretical.getMzAt(0);
        double score = 0d;
        for (int i=0; i < measured.size(); ++i) {
            final double mz = measured.getMzAt(i) - (i==0 ? 0 : measured.getMzAt(i-1));
            final double thMz = theoretical.getMzAt(i) - (i==0 ? 0 : theoretical.getMzAt(i-1));
            final double intensity = norm.rescale(measured.getIntensityAt(i));
            // TODO: thMz hier richtig?
            final double sd = 1d/massDeviationPenalty * (intensity * ppmFullIntensity + (1-intensity) * ppmLowestIntensity) * 1e-6 * measured.getMzAt(i);
            score += Math.log(Erf.erfc(Math.abs(thMz - mz)/(root2*sd)));
        }
        return score;
    }
}
