package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.util.Comparator;

/**
 * @author Kai Dührkop
 */
public class MS2Peak extends Peak {

    private final Ms2Spectrum spectrum;

    public MS2Peak(Ms2Spectrum spectrum, double mz, double intensity) {
        super(mz, intensity);
        this.spectrum = spectrum;
    }

    public MS2Peak(MS2Peak p) {
        this(p.spectrum, p.mass, p.intensity);
    }

    public Ms2Spectrum getSpectrum() {
        return spectrum;
    }

    public double getMz() {
        return mass;
    }

    @Override
    public MS2Peak clone() {
        return new MS2Peak(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MS2Peak peak = (MS2Peak) o;

        if (Double.compare(peak.intensity, intensity) != 0) return false;
        if (Double.compare(peak.mass, mass) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = mass != +0.0d ? Double.doubleToLongBits(mass) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = intensity != +0.0d ? Double.doubleToLongBits(intensity) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return intensity + "@" + mass + " Da";
    }

    public static class IntensityComparator implements Comparator<MS2Peak> {

        @Override
        public int compare(MS2Peak o1, MS2Peak o2) {
            return Double.compare(o1.getIntensity(), o2.getIntensity());
        }
    }
}
