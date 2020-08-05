/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms;

public class SimplePeak implements Peak {
    protected static final double DELTA = 1e-8;

    protected double mass;
    protected double intensity;

    public SimplePeak(Peak x) {
        this(x.getMass(), x.getIntensity());
    }

    public SimplePeak(double mass, double intensity) {
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
        return Double.compare(mass, o.getMass());
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
        if (obj instanceof SimplePeak) {
            Peak p = (Peak) obj;
            return Math.abs(mass - p.getMass()) < DELTA && Math.abs(intensity - p.getIntensity()) < DELTA;
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
        return "[" + mass + "," + intensity + "]";
    }
}