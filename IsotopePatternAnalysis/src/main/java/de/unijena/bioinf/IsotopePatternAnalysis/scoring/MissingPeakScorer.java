package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import org.apache.commons.math3.special.Erf;

public class MissingPeakScorer implements IsotopePatternScorer {

    private double lambda;
    private Normalization normalization;
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
        if (normalization.getMode()!=null && !norm.equals(normalization)) {
            measured = normalization.call(measuredSpectrum);
            theoretical = normalization.call(theoreticalSpectrum);
        } else {
            measured = measuredSpectrum;
            theoretical = theoreticalSpectrum;
        }
        double score = 0;
        final double standardDeviation = 0.04;
        for (int i=measured.size(); i < theoretical.size(); ++i) {
            final double diff = theoretical.getIntensityAt(i);
            score += Math.log(Erf.erfc(diff / (sqrt2 * standardDeviation)));
            //score -=  lambda*theoretical.getIntensityAt(i);
        }
        return score;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.normalization = (Normalization)helper.unwrap(document, document.getFromDictionary(dictionary, "normalization"));
        this.lambda = document.getDoubleFromDictionary(dictionary, "lambda");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "normalization", helper.wrap(document, normalization));
        document.addToDictionary(dictionary, "lambda", lambda);
    }
}
