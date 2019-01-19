package de.unijena.bioinf.sirius.peakprocessor;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

/**
 * If each input spectra is already normalized by its base peak, we renormalize them such that every spectrum sum up to 1.
 */
public class NormalizeToSumPreprocessor implements UnmergedSpectrumProcessor {
    @Override
    public void process(MutableMs2Experiment experiment) {
        if (experiment.getMs2Spectra().size()<=1) return;
        double basePeak = Double.NaN;
        // first: find base peak
        for (MutableMs2Spectrum spec : experiment.getMs2Spectra()) {
            double base = Spectrums.getMaximalIntensity(spec);
            if (Double.isNaN(basePeak)) {
                basePeak = base;
            } else if (Math.abs(basePeak-base)>1e-6) {
                return;
            }
        }
        // now renormalize
        for (MutableMs2Spectrum spec : experiment.getMs2Spectra()) {
            Spectrums.normalize(spec, Normalization.Sum(1d));
        }
    }
}
