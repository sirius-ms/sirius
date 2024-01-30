
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

package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;


public abstract class BasicSpectrum<P extends Peak> extends AbstractSpectrum<P> {
	
	protected final double[] masses;
	protected final double[] intensities;

	protected BasicSpectrum(double[] masses, double[] intensities, boolean noCopy) {
		super();
		if (masses.length != intensities.length)
			throw new IllegalArgumentException("size of masses and intensities differ");
		this.masses = noCopy ? masses : masses.clone();
		this.intensities = noCopy ? intensities : intensities.clone();
	}

	public BasicSpectrum(double[] masses, double[] intensities) {
		this(masses,intensities,false);
	}
	
	public <T extends Peak, S extends Spectrum<T>> BasicSpectrum(S s) {
		this.masses = Spectrums.copyMasses(s);
		this.intensities = Spectrums.copyIntensities(s);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone(); // possible because immutable
	}

	@Override
	public int size() {
		return masses.length;
	}

}
