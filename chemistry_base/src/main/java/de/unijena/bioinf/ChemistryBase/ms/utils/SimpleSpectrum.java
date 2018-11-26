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

import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.util.Arrays;

/**
 * Simple implementation of an immutable Mass Spectrum.
 * Peaks are stored ordered by mass in arrays.
 */
public class SimpleSpectrum extends BasicSpectrum<Peak> implements OrderedSpectrum<Peak>{

	protected final int hash;
	
	public SimpleSpectrum(double[] masses, double[] intensities) {
		this(new ArrayWrapperSpectrum(masses, intensities));
	}
	
	public <T extends Peak, S extends Spectrum<T>> SimpleSpectrum(S s) {
		super(orderedSpectrum(s));
		this.hash = Arrays.hashCode(this.masses) ^ Arrays.hashCode(this.intensities);
	}

	@Override
	public double getMzAt(int index) {
		return masses[index];
	}
	
	@Override
	public double getIntensityAt(int index) {
		return intensities[index];
	}
	
	@Override
	public Peak getPeakAt(int index) {
		return new Peak(masses[index], intensities[index]);
	}
	
	
	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public SimpleSpectrum clone() {
		return new SimpleSpectrum(masses, intensities);
	}
		
	private static Spectrum<? extends Peak> orderedSpectrum(Spectrum<? extends Peak> s) {
        if (s instanceof OrderedSpectrum) return s;
        final MutableSpectrum<? extends Peak> t = new SimpleMutableSpectrum(s);
		Spectrums.sortSpectrumByMass(t);
		return t;
	}
	
	
}
