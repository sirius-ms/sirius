
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

package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;

import java.util.Comparator;

/**
 * @author Kai Dührkop
 */
public class MS2Peak extends SimplePeak {

    private final Ms2Spectrum spectrum;

    public MS2Peak(Ms2Spectrum spectrum, double mz, double intensity) {
        super(mz, intensity);
        this.spectrum = spectrum;
    }

    public MS2Peak(MS2Peak p) {
        this(p.spectrum, p.mz, p.intensity);
    }

    public Ms2Spectrum getSpectrum() {
        return spectrum;
    }

    public double getMz() {
        return mz;
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
        return Double.compare(peak.mz, mz) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = mz != +0.0d ? Double.doubleToLongBits(mz) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = intensity != +0.0d ? Double.doubleToLongBits(intensity) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return intensity + "@" + mz + " Da";
    }

    public static class IntensityComparator implements Comparator<MS2Peak> {

        @Override
        public int compare(MS2Peak o1, MS2Peak o2) {
            return Double.compare(o1.getIntensity(), o2.getIntensity());
        }
    }
}
