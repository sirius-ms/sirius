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

package de.unijena.bioinf.ms.gui.ms_viewer.data;

import java.util.ArrayList;
import java.util.List;


public class DefaultPeakInformation implements PeakInformation{
	
	private double mass, relInt, absInt;
	
	private List<MolecularFormulaInformation> decomps;
	
	public DefaultPeakInformation(double mass, double relInt, double absInt){
		this.mass = mass;
		this.relInt = relInt;
		this.absInt = absInt;
		
		this.decomps = new ArrayList<MolecularFormulaInformation>(5);
	}
	
	public DefaultPeakInformation(){
		this(-1,-1,-1);
	}
	
	public void addMolecularFormulaInformation(MolecularFormulaInformation info){
		this.decomps.add(info);
	}
	
	public void removeMolecularFormulaInformation(MolecularFormulaInformation info){
		this.decomps.remove(info);
	}
	
	public void setMass(double mass){
		this.mass = mass;
	}
	
	public void setRelativeIntensity(double relInt){
		this.relInt = relInt;
	}
	
	public void setAbsoluteIntensity(double absInt){
		this.absInt = absInt;
	}
	
	@Override
	public double getMass() {
		return this.mass;
	}

	@Override
	public double getRelativeIntensity() {
		return this.relInt;
	}

	@Override
	public double getAbsoluteIntensity() {
		return this.absInt;
	}

	@Override
	public List<MolecularFormulaInformation> getDecompositions() {
		return this.decomps;
	}

	@Override
	public boolean isIsotope() {
		return false;
	}

}
