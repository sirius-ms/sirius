package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.NormalizationMode;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternScorer;

public class MissingPeakScorer<P extends Peak, T extends Spectrum<P>> implements IsotopePatternScorer<P, T> {

    private final double lambda;
    private final Normalization normalization;
    private final static double sqrt2 = Math.sqrt(2);

    public MissingPeakScorer(NormalizationMode normalization, double lambda) {
        this.lambda = lambda;
        this.normalization = new Normalization(normalization, 1);
    }

    public MissingPeakScorer(double lambda) {
        this(NormalizationMode.MAX, lambda);
    }

    public MissingPeakScorer() {
        this(1);
    }

    @Override
    public double score(T measuredSpectrum, T theoreticalSpectrum, Normalization norm) {
        final Spectrum<? extends Peak> measured, theoretical;
        if (!norm.equals(normalization)) {
            measured = normalization.call(measuredSpectrum);
            theoretical = normalization.call(theoreticalSpectrum);
        } else {
            measured = measuredSpectrum;
            theoretical = theoreticalSpectrum;
        }
        double score = 0;
        for (int i=measured.size(); i < theoretical.size(); ++i) {
            final double diff = theoretical.getIntensityAt(i);
            //score += Erf.erf(diff/(sqrt2*standardDeviation));
            score -=  lambda*theoretical.getIntensityAt(i);
        }
        return score;
    }
}
