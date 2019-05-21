package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

/**
 * find best scoring alignment, intensity weighted. Each peak matches at most one peak in the other spectrum.
 */
public class IntensityWeightedSpectralAlignment extends AbstractSpectralAlignment {

    public IntensityWeightedSpectralAlignment(Deviation deviation) {
        super(deviation);
    }

    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        return score1To1(left, right);
    }

    @Override
    protected double scorePeaks(Peak lp, Peak rp) {
        return lp.getIntensity()*rp.getIntensity();
    }

    @Override
    protected double maxAllowedDifference(double mz) {
        return deviation.absoluteFor(mz);
    }
}
