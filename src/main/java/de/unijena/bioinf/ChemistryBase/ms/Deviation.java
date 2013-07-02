package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@HasParameters
public class Deviation {

    private final double ppm;
    private final double absolute;

    public Deviation(double ppm) {
        this.ppm = ppm;
        this.absolute = 1e-4*ppm; // set absolute to 100 Da with given ppm
    }

    public Deviation(@Parameter("ppm") double ppm, @Parameter("absolute") double absolute) {
        this.ppm = ppm;
        this.absolute = absolute;
    }

    public Deviation multiply(int scalar) {
        return new Deviation(ppm*scalar, absolute*2);
    }

    public Deviation multiply(double scalar) {
        return new Deviation(ppm*scalar, absolute*2);
    }

    public Deviation divide(double scalar) {
        return multiply(1d/scalar);
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

    public String toString() {
        return ppm + " ppm (" + absolute + " m/z)";
    }

    private static Pattern pattern = Pattern.compile("(.+) ppm \\((.+) m\\/z\\)");
    public static Deviation fromString(String s) {
        final Matcher m = pattern.matcher(s);
        if (!m.find()) throw new IllegalArgumentException("Pattern should have the format <number> ppm (<number> m/z)");
        return new Deviation(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)));
    }
}
