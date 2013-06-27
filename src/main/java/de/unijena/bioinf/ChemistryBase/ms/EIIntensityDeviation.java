package de.unijena.bioinf.ChemistryBase.ms;

/**
 * For peaks in EI spectra the allowed deviation increases with increasing intensity.
 */
public class EIIntensityDeviation extends Deviation{
    private double smallAbsError;
    private double largeAbsError;
    private double smallErrorPpm;
    private double largeErrorPpm;
    private double intensity;

    /**
     * small errors at 0 relative intensity and large errors at full intensity. absolute error in Da.
     * @param smallErrorPpm
     * @param largeErrorPpm
     * @param smallAbsError
     * @param largeAbsError
     */
    public EIIntensityDeviation(double smallErrorPpm, double largeErrorPpm, double smallAbsError, double largeAbsError) {
        super(0, 0);
        this.smallErrorPpm = smallErrorPpm;
        this.largeErrorPpm = largeErrorPpm;
        this.smallAbsError = smallAbsError;
        this.largeAbsError = largeAbsError;

    }

    @Override
    public double absoluteFor(double value) {
        final double absolute = (intensity*(largeAbsError-smallAbsError)/1d+smallAbsError);
        final double relative = (intensity*(largeErrorPpm-smallErrorPpm)/1d+smallErrorPpm)*1e-6*value;
        return Math.max(relative,absolute);
    }

    @Override
    public boolean inErrorWindow(double center, double value) {
        return super.inErrorWindow(center, value);
    }

    public void setRelIntensity(double relIntensity){
        this.intensity = relIntensity;
    }

    public double getSmallAbsError() {
        return smallAbsError;
    }

    public double getLargeAbsError() {
        return largeAbsError;
    }

    public double getSmallErrorPpm() {
        return smallErrorPpm;
    }

    public double getLargeErrorPpm() {
        return largeErrorPpm;
    }

    public double getIntensity() {
        return intensity;
    }
}
