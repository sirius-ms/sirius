package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.recal.MzRecalibration;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import java.util.Arrays;

/**
 * Recommended recalibration strategy.
 */
public class LeastSquare extends AbstractRecalibrationStrategy {

    public LeastSquare() {
        super();
    }

    public LeastSquare(Deviation epsilon, int minNumberOfPeaks, double threshold) {
        super(epsilon, minNumberOfPeaks, threshold);
    }

    @Override
    public UnivariateFunction recalibrate(MutableSpectrum<Peak> spectrum, Spectrum<Peak> referenceSpectrum) {
        spectrum = new SimpleMutableSpectrum(spectrum);
        SimpleMutableSpectrum refSpectrum = new SimpleMutableSpectrum(referenceSpectrum);
        preprocess(spectrum, refSpectrum);
        final double[] eps = new double[spectrum.size()];
        for (int k=0; k < eps.length; ++k) eps[k] = this.epsilon.absoluteFor(spectrum.getMzAt(k));
        final double[][] values = MzRecalibration.maxIntervalStabbing(spectrum, refSpectrum, eps, threshold);
        if (values[0].length<minNumberOfPeaks) return new Identity();

        if (forceParentPeakIn) forceParentPeakInRecalibration(spectrum,referenceSpectrum,values);


        final UnivariateFunction recalibration = MzRecalibration.getLinearRecalibration(values[0], values[1]);
        MzRecalibration.recalibrate(spectrum, recalibration);
        return recalibration;
    }
}
