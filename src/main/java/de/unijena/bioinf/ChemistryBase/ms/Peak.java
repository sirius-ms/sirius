/**
 * 
 */
package de.unijena.bioinf.ChemistryBase.ms;


/**
 * @author Martin Engler
 *
 */
public class Peak implements Comparable<Peak>, Cloneable {
	
	private static final double DELTA = 1e-8;
	
	protected double mass;
	protected double intensity;

    public Peak(Peak x) {
        this(x.getMass(), x.getIntensity());
    }

	public Peak(double mass, double intensity) {
		super();
		this.mass = mass;
		this.intensity = intensity;
	}

	public double getMass() {
		return mass;
	}

	public double getIntensity() {
		return intensity;
	}

	@Override
	public int compareTo(Peak o) {
		return Double.compare(mass, o.mass);
	}

	@Override
	public Peak clone() {
		try {
			return (Peak) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
        if (obj instanceof Peak) {
			Peak p = (Peak) obj;
			return Math.abs(mass - p.mass) < DELTA && Math.abs(intensity - p.intensity) < DELTA;
		}
		return false;
	}

	@Override
	public int hashCode() {
        long mbits = Double.doubleToLongBits(mass);
        long ibits = Double.doubleToLongBits(intensity);
        return (int) (((mbits ^ (mbits >>> 32)) >> 13) ^ (ibits ^ (ibits >>> 32)));
	}

	@Override
	public String toString() {
		return "["+mass+","+intensity+"]";
	}

}
