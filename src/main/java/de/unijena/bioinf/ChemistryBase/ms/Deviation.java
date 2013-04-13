package de.unijena.bioinf.ChemistryBase.ms;

public class Deviation {

    private final double ppm;
    private final double absolute;
    private final double precision;

    public Deviation(double ppm, double absolute, double precision) {
        this.ppm = ppm;
        this.absolute = absolute;
        this.precision = precision;
    }
    
    public Deviation(double ppm, double absolute) {
        this(ppm, absolute, 1e-5);
    }

    public Deviation multiply(int scalar) {
        return new Deviation(ppm*scalar, absolute*2, precision);
    }

    public double absoluteFor(double value) {
        return Math.max(ppm * value * 1e-6, absolute);
    }

    public boolean inErrorWindow(double center, double value) {
        final double diff = Math.abs(center - value);
        return diff <= absoluteFor(center);
    }

    public double getPpm() {
        return ppm;
    }

    public double getAbsolute() {
        return absolute;
    }

    public double getPrecision() {
        return precision;
    }
}
