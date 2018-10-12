/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package de.unijena.bioinf.ChemistryBase.ms;


/**
 * @author Martin Engler
 *
 */
public class Peak implements Comparable<Peak>, Cloneable {
	
	protected static final double DELTA = 1e-8;
	
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
