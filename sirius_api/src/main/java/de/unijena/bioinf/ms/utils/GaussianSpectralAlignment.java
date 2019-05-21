package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

/**
 * treat peaks as (unnormalized) Gaussians and score overlapping areas of PDFs. Each peak might score agains multiple peaks in the other spectrum.
 */
public class GaussianSpectralAlignment extends AbstractSpectralAlignment {
    public GaussianSpectralAlignment(Deviation deviation) {
        super(deviation);
    }

    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        return scoreAllAgainstAll(left, right);
    }

    protected double scorePeaks(Peak lp, Peak rp) {
        //formula from Jebara: Probability Product Kernels. multiplied by intensities
        // (1/(4*pi*sigma**2))*exp(-(mu1-mu2)**2/(4*sigma**2))
        final double mzDiff = Math.abs(lp.getMass()-rp.getMass());

        final double variance = Math.pow(deviation.absoluteFor(Math.min(lp.getMass(), rp.getMass())),2);
//        final double variance = Math.pow(0.01,2); //todo same sigma for all?
        final double varianceTimes4 = 4*variance;
        final double constTerm = 1.0/(Math.PI*varianceTimes4);

        final double propOverlap = constTerm*Math.exp(-(mzDiff*mzDiff)/varianceTimes4);
        return (lp.getIntensity()*rp.getIntensity())*propOverlap;
    }


    protected double maxAllowedDifference(double mz) {
        //change to, say 3*dev, when using gaussians
        return deviation.absoluteFor(mz);
//        return 0.01;
    }
}
