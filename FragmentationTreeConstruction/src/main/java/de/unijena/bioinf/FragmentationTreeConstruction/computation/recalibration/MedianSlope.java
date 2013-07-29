package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.recal.MzRecalibration;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

/**
 * Recommended recalibration strategy.
 */
public class MedianSlope implements RecalibrationStrategy, Parameterized {

    private Deviation epsilon;

    public MedianSlope() {
        this(new Deviation(5, 0.001));
    }

    public MedianSlope(Deviation epsilon) {
        this.epsilon = epsilon;
    }

    public Deviation getEpsilon() {
        return epsilon;
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
        if (values[0].length<5) return new Identity();
        final UnivariateFunction recalibration = MzRecalibration.getMedianLinearRecalibration(values[0], values[1]);
        MzRecalibration.recalibrate(spectrum, recalibration);
        return recalibration;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        epsilon = Deviation.fromString(document.getStringFromDictionary(dictionary, "epsilon"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "epsilon", epsilon.toString());
    }
}
