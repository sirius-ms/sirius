package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.recal.MzRecalibration;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

/**
 * Recommended recalibration strategy.
 */
public class MedianSlope implements RecalibrationStrategy, Parameterized {

    private Deviation epsilon;
    private int minNumberOfPeaks;
    private double minIntensity;

    public MedianSlope() {
        this(new Deviation(4, 0.001), 10);
    }

    public MedianSlope(Deviation epsilon, int minNumberOfPeaks) {
        this.epsilon = epsilon;
        this.minNumberOfPeaks = minNumberOfPeaks;
        this.minIntensity = 0d;
    }

    public int getMinNumberOfPeaks() {
        return minNumberOfPeaks;
    }

    public void setEpsilon(Deviation epsilon) {
        this.epsilon = epsilon;
    }

    public void setMinNumberOfPeaks(int minNumberOfPeaks) {
        this.minNumberOfPeaks = minNumberOfPeaks;
    }

    public double getMinIntensity() {
        return minIntensity;
    }

    public void setMinIntensity(double minIntensity) {
        this.minIntensity = minIntensity;
    }

    public Deviation getEpsilon() {
        return epsilon;
    }

    @Override
    public UnivariateFunction recalibrate(MutableSpectrum<Peak> spectrum, Spectrum<Peak> referenceSpectrum) {
        final Deviation dev = epsilon;
        spectrum = new SimpleMutableSpectrum(spectrum);
        final SimpleMutableSpectrum ref = new SimpleMutableSpectrum(referenceSpectrum);
        for (int i=0; i < ref.size(); ++i) {
            if (spectrum.getIntensityAt(i) < minIntensity) {
                ref.removePeakAt(i);
                spectrum.removePeakAt(i);
            }
        }
        final double[][] values = MzRecalibration.maxIntervalStabbing(spectrum, referenceSpectrum, new UnivariateFunction() {
            @Override
            public double value(double x) {
                return dev.absoluteFor(x);
            }
        });
        if (values[0].length<minNumberOfPeaks) return new Identity();
        final UnivariateFunction recalibration = MzRecalibration.getMedianLinearRecalibration(values[0], values[1]);
        MzRecalibration.recalibrate(spectrum, recalibration);
        return recalibration;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        epsilon = Deviation.fromString(document.getStringFromDictionary(dictionary, "epsilon"));
        minNumberOfPeaks = (int)document.getIntFromDictionary(dictionary, "minNumberOfPeaks");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "epsilon", epsilon.toString());
        document.addToDictionary(dictionary, "minNumberOfPeaks", minNumberOfPeaks);
    }
}
