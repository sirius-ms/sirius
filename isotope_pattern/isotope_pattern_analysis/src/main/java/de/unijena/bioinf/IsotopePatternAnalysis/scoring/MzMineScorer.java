package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.Arrays;

public class MzMineScorer implements IsotopePatternScorer {
    @Override
    public void score(double[] scoreUptoKPeaks, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment) {
        double intensityDifference = 0d;
        // normalize to sum
        final SimpleSpectrum left = Spectrums.getNormalizedSpectrum(measuredSpectrum, Normalization.Sum(1));
        final SimpleSpectrum right = Spectrums.getNormalizedSpectrum(theoreticalSpectrum, Normalization.Sum(1));
        for (int i=0; i < left.size(); ++i) {
            intensityDifference += Math.abs(left.getIntensityAt(i)-right.getIntensityAt(i));
        }
        Arrays.fill(scoreUptoKPeaks, 0d);
        scoreUptoKPeaks[scoreUptoKPeaks.length-1] = 1d-intensityDifference;
        //for (int i=0; i < scoreUptoKPeaks.length; ++i) {
        //    scoreUptoKPeaks[i] += 1d - intensityDifference;//10*Math.log(1d-intensityDifference);
        //}
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
