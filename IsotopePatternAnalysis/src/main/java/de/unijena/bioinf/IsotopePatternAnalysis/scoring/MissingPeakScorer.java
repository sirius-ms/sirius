package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.ms.*;

public class MissingPeakScorer implements IsotopePatternScorer {

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
    public double score(Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization norm, MsExperiment experiment) {
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
