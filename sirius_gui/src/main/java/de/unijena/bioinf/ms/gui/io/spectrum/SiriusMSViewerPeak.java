/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.io.spectrum;


import de.unijena.bioinf.ms.gui.ms_viewer.data.MolecularFormulaInformation;
import de.unijena.bioinf.ms.gui.ms_viewer.data.PeakInformation;

import java.util.Collections;
import java.util.List;

public class SiriusMSViewerPeak implements PeakInformation {
	
	private double absInt, relInt, mass, sn;

	public SiriusMSViewerPeak() {
		absInt = 0;
		relInt = 0;
		mass = 0;
		sn = 0;
	}
	
	

	public void setAbsoluteIntensity(double absInt) {
		this.absInt = absInt;
	}



	public void setRelativeIntensity(double relInt) {
		this.relInt = relInt;
	}



	public void setMass(double mass) {
		this.mass = mass;
	}



	public void setSn(double sn) {
		this.sn = sn;
	}
	
	@Override
	public double getAbsoluteIntensity() {
		return absInt;
	}

	@Override
	public List<MolecularFormulaInformation> getDecompositions() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public double getMass() {
		return mass;
	}

	@Override
	public double getRelativeIntensity() {
		return relInt;
	}

	@Override
	public boolean isIsotope() {
		return false;
	}
	
}
