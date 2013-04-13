package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternScorer;

import static java.lang.Math.abs;
import static java.lang.Math.log;

public class PluscalScorer<P extends Peak, T extends Spectrum<P>> implements IsotopePatternScorer<P, T> {

    private final Normalization normalization;

    public PluscalScorer(Normalization n) {
        this.normalization = n;
    }

    public PluscalScorer() {
        this(Normalization.Max(1));
    }

    @Override
    public double score(T measuredSpectrum, T theoreticalSpectrum, Normalization norm) {
        final Normalization normalization = Normalization.Max(1);
        final Spectrum<? extends Peak> measured, theoretical;
        if (!norm.equals(normalization)) {
            measured = normalization.call(measuredSpectrum);
            theoretical = normalization.call(theoreticalSpectrum);
        } else {
            measured = measuredSpectrum;
            theoretical = theoreticalSpectrum;
        }
        double score = 0d;
        for (int i=1; i < measured.size(); ++i) {
            final double intensity = measured.getIntensityAt(i);
            final double thIntensity = theoretical.getIntensityAt(i);
            score += log(1 - abs(intensity - thIntensity));
        }
        return score;
    }

}
