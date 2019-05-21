package de.unijena.bioinf.ChemistryBase.ms;

/**
 * Created by ge28quv on 11/07/17.
 */
public class ImmutableIsolationWindow extends EstimatedIsolationWindow {

    public ImmutableIsolationWindow(double maxWindowSize, double massShift, double[] relMz, double[] filterRatio) {
        super(maxWindowSize, massShift, false, null);
        this.relMz = relMz;
        this.filterRatio = filterRatio;
    }

    public ImmutableIsolationWindow(double maxWindowSize, double[] relMz, double[] filterRatio) {
        super(maxWindowSize);
        this.relMz = relMz;
        this.filterRatio = filterRatio;
    }

    public ImmutableIsolationWindow(EstimatedIsolationWindow window){
        super(window.getMaxWindowSize(), window.getMassShift(), false, null);
        this.relMz = window.getFilterMassValues();
        this.filterRatio = window.getFilterIntensityRatios();
    }

    @Override
    protected void estimateDistribution(IsotopeRatioInformation isotopeRatioInformation, Ms2Dataset dataset) {
        throw new NoSuchMethodError("cannot estimate distribution. immutable.");
    }
}
