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

public class AbstractRecalibrationStrategy implements RecalibrationStrategy, Parameterized {

    protected Deviation epsilon;
    protected int minNumberOfPeaks;
    protected double minIntensity, threshold;
    protected Deviation maxDeviation;

    public AbstractRecalibrationStrategy() {
        this(new Deviation(4, 0.001), 10, 0.1);
    }

    public AbstractRecalibrationStrategy(Deviation epsilon, int minNumberOfPeaks, double threshold) {
        this.epsilon = epsilon;
        this.minNumberOfPeaks = minNumberOfPeaks;
        this.minIntensity = 0d;
        this.maxDeviation = new Deviation(10, 5e-4);
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public Deviation getMaxDeviation() {
        return maxDeviation;
    }

    public void setMaxDeviation(Deviation maxDeviation) {
        this.maxDeviation = maxDeviation;
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
        spectrum = new SimpleMutableSpectrum(spectrum);
        final SimpleMutableSpectrum ref = new SimpleMutableSpectrum(referenceSpectrum);

        preprocess(spectrum, ref);
        final double[][] values = MzRecalibration.maxIntervalStabbing(spectrum, referenceSpectrum, new UnivariateFunction() {
            @Override
            public double value(double x) {
                return epsilon.absoluteFor(x);
            }
        });
        if (values[0].length<minNumberOfPeaks) return new Identity();
        final UnivariateFunction recalibration = MzRecalibration.getMedianLinearRecalibration(values[0], values[1]);
        MzRecalibration.recalibrate(spectrum, recalibration);
        return recalibration;
    }

    protected void preprocess(MutableSpectrum<? extends Peak> spectrum, MutableSpectrum<? extends Peak> ref) {
        int i=0;
        while (i < ref.size()) {
            if (spectrum.getIntensityAt(i) < minIntensity || !maxDeviation.inErrorWindow(spectrum.getMzAt(i), ref.getMzAt(i))) {
                ref.removePeakAt(i);
                spectrum.removePeakAt(i);
            } else ++i;
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        epsilon = Deviation.fromString(document.getStringFromDictionary(dictionary, "epsilon"));
        minNumberOfPeaks = (int)document.getIntFromDictionary(dictionary, "minNumberOfPeaks");
        threshold = document.getDoubleFromDictionary(dictionary, "threshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "epsilon", epsilon.toString());
        document.addToDictionary(dictionary, "minNumberOfPeaks", minNumberOfPeaks);
        document.addToDictionary(dictionary, "threshold", threshold);
    }



}
