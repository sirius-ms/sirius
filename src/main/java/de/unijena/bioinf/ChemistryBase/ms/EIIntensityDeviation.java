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
        if (smallAbsError>largeAbsError || smallErrorPpm>largeErrorPpm) throw new IllegalArgumentException("large errors have to be greater than small errors");
        this.smallErrorPpm = smallErrorPpm;
        this.largeErrorPpm = largeErrorPpm;
        this.smallAbsError = smallAbsError;
        this.largeAbsError = largeAbsError;

    }

    @Override
    /**
     * use for intensity Normalization.Max(1d) only
     */
    public double absoluteFor(double value) {
        final double absolute = (intensity*(largeAbsError-smallAbsError)/1d+smallAbsError);
        final double relative = (intensity*(largeErrorPpm-smallErrorPpm)/1d+smallErrorPpm)*1e-6*value;
        return Math.max(relative,absolute);
    }

    @Override
    public boolean inErrorWindow(double center, double value) {
        return super.inErrorWindow(center, value);
    }

    @Override
    public Deviation multiply(int scalar) {
        return new EIIntensityDeviation(smallErrorPpm*scalar, largeErrorPpm*scalar, smallAbsError*2, largeAbsError*2);
    }

    @Override
    public Deviation multiply(double scalar) {
        return new EIIntensityDeviation(smallErrorPpm*scalar, largeErrorPpm*scalar, smallAbsError*2, largeAbsError*2);
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
