package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import static de.unijena.bioinf.ChemistryBase.ms.NormalizationMode.MAX;
import static de.unijena.bioinf.ChemistryBase.ms.NormalizationMode.SUM;

public class Normalization {
	
	private final NormalizationMode mode;
	private final double norm;
	
	public final static Normalization Sum = new Normalization(SUM, 1d);
	public final static Normalization Max = new Normalization(MAX, 1d);
	
	public static Normalization Sum(double norm) {
		return new Normalization(SUM, norm);
	}
	
	public static Normalization Max(double norm) {
		return new Normalization(MAX, norm);
	}
	
	public Normalization(NormalizationMode mode, double norm) {
		this.mode = mode;
		this.norm = norm;
	}

    /**
     * Given a value from 0.0 to 1.0, this function returns the scaled value from 0.0 to MAX.
     */
    public double scale(double value) {
        return value*norm;
    }

    /**
     * Given a value from 0.0 to MAX, this function returns the rescaled value from 0.0 to 1.0.
     */
    public double rescale(double value) {
        return value/norm;
    }

    public void run(MutableSpectrum<? extends Peak> s) {
        Spectrums.normalize(s, this);
    }

    public <P extends Peak, S extends Spectrum<P>> SimpleSpectrum call(S s) {
        return Spectrums.getNormalizedSpectrum(s, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Normalization that = (Normalization) o;

        if (Double.compare(that.norm, norm) != 0) return false;
        if (mode != that.mode) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = mode.hashCode();
        temp = norm != +0.0d ? Double.doubleToLongBits(norm) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public NormalizationMode getMode() {
		return mode;
	}

	public double getBase() {
		return norm;
	}
	
}
