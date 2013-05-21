package de.unijena.bioinf.ChemistryBase.ms;

public class Deviation {

    private final double ppm;
    private final double absolute;

    public Deviation(double ppm) {
        this.ppm = ppm;
        this.absolute = 1e-4*ppm; // set absolute to 100 Da with given ppm
    }

    public Deviation(double ppm, double absolute) {
        this.ppm = ppm;
        this.absolute = absolute;
    }

    public Deviation multiply(int scalar) {
        return new Deviation(ppm*scalar, absolute*2);
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
}
