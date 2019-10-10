package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

@Deprecated
public class ScorePerPeak implements IsotopePatternScorer {

    private double scorePerPeak;

    public ScorePerPeak() {
        this.scorePerPeak = 5d;
    }


    @Override
    public void score(double[] scoreUptoKPeaks, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment) {
        for (int k=0; k < scoreUptoKPeaks.length; ++k) {
            if (measuredSpectrum.size() > k) scoreUptoKPeaks[k] += scorePerPeak;
        }

    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        scorePerPeak = document.getDoubleFromDictionary(dictionary, "scorePerPeak");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "scorePerPeak", scorePerPeak);
    }
}
