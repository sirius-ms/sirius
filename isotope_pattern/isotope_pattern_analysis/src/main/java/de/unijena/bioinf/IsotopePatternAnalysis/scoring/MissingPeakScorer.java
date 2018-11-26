package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class MissingPeakScorer implements IsotopePatternScorer {

    protected double lambda = 50;
    protected double threshold = 0.05;

    public MissingPeakScorer() {
    }

    @Override
    public void score(double[] scoreUptoKPeaks, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment) {
        if (usedNormalization.getBase() != 1 || usedNormalization.getMode() != NormalizationMode.MAX) {
            theoreticalSpectrum = Spectrums.getNormalizedSpectrum(theoreticalSpectrum, Normalization.Max(1));
        }
        double score = 0d;
        for (int k=theoreticalSpectrum.size()-1; k >= 0; --k) {
            final double intensity = theoreticalSpectrum.size() > k ? (theoreticalSpectrum.getIntensityAt(k)) : 0d;
            if (intensity >= threshold) {
                if (k < scoreUptoKPeaks.length) scoreUptoKPeaks[k] += score;
                score -= intensity*lambda;
            }

        }

    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary,"lambda"))
            this.lambda = document.getDoubleFromDictionary(dictionary, "lambda");
        if (document.hasKeyInDictionary(dictionary,"threshold"))
            this.threshold = document.getDoubleFromDictionary(dictionary, "threshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "lambda", lambda);
        document.addToDictionary(dictionary, "threshold", threshold);
    }
}
