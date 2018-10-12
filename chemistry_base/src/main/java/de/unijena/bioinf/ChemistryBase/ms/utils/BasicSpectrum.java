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
package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public abstract class BasicSpectrum<P extends Peak> extends AbstractSpectrum<P> {
	
	protected final double[] masses;
	protected final double[] intensities;
	
	public BasicSpectrum(double[] masses, double[] intensities) {
		super();
		if (masses.length != intensities.length)
			throw new IllegalArgumentException("size of masses and intensities differ");
		this.masses = masses.clone();
		this.intensities = intensities.clone();
	}
	
	public <T extends Peak, S extends Spectrum<T>> BasicSpectrum(S s) {
		super(s);
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
