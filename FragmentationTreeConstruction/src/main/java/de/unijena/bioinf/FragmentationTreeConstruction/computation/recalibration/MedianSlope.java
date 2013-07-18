package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.recal.MzRecalibration;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

@HasParameters
public class MedianSlope implements SpectrumRecalibration{

    private Deviation epsilon;

    public MedianSlope() {
        this(new Deviation(5, 0.001));
    }

    public MedianSlope(@Parameter Deviation epsilon) {
        this.epsilon = epsilon;
    }

    @Override
    public UnivariateFunction recalibrate(MutableSpectrum<Peak> spectrum, Spectrum<Peak> referenceSpectrum) {
        final Deviation dev = epsilon;
        final double[][] values = MzRecalibration.maxIntervalStabbing(spectrum, referenceSpectrum, new UnivariateFunction() {
            @Override
            public double value(double x) {
                return dev.absoluteFor(x);
            }
        });
        if (values[0].length<6) return new Identity();
        final UnivariateFunction recalibration = MzRecalibration.getMedianLinearRecalibration(values[0], values[1]);
        MzRecalibration.recalibrate(spectrum, recalibration);
        return recalibration;
    }
}
