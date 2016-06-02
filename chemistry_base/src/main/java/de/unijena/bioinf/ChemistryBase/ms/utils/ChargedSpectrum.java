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

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
@Deprecated
public class ChargedSpectrum extends BasicSpectrum<ChargedPeak>{

	private final Ionization ionization;
	
	public ChargedSpectrum(ChargedSpectrum p) {
		super(p);
		this.ionization = p.ionization;
	}
	
	public ChargedSpectrum(Spectrum<?> s, Ionization ionization) {
		super(s);
		this.ionization = ionization;
	}
	
	public ChargedSpectrum(Spectrum<?> s, int charge) {
		this(s, new Charge(charge));
	}
	
	public ChargedSpectrum(double[] masses, double[] intensities, int charge) {
		this(masses, intensities, new Charge(charge));
	}
	
	public ChargedSpectrum(double[] masses, double[] intensities, Ionization ionization) {
		super(masses, intensities);
		this.ionization = ionization;
	}

    public Ionization getIonization() {
        return ionization;
    }
	
	public SimpleSpectrum getNeutralMassSpectrum() {
		return Spectrums.neutralMassSpectrum(this, ionization);
	}

	@Override
	public ChargedPeak getPeakAt(int index) {
		return new ChargedPeak(masses[index], intensities[index], ionization);
	}
	
	
	
	
}
