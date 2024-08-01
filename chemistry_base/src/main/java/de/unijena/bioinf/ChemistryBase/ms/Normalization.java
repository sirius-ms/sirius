
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

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import static de.unijena.bioinf.ChemistryBase.ms.NormalizationMode.*;

@HasParameters
public class Normalization {
	
	private final NormalizationMode mode;
	private final double norm;
	
	public final static Normalization Sum = new Normalization(SUM, 1d);
	public final static Normalization Max = new Normalization(MAX, 1d);
    public final static Normalization First = new Normalization(FIRST, 1d);
	
	public static Normalization Sum(double norm) {
		return new Normalization(SUM, norm);
	}
	
	public static Normalization Max(double norm) {
		return new Normalization(MAX, norm);
	}
	
	public Normalization(@Parameter("mode") NormalizationMode mode, @Parameter("base") double norm) {
		this.mode = mode;
		this.norm = norm;
	}

    public static Normalization L2() {
        return new Normalization(L2, 1d);
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
        return mode == that.mode;
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
