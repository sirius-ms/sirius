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


public class DefaultMolecularFormulaInformation implements MolecularFormulaInformation{
	
	private String formulaString;
	private double mass;
	private PeakInformation peak;
	
	private boolean useFormula;
	
	public DefaultMolecularFormulaInformation(){
		this("",0,true,null);
	}
	
	public DefaultMolecularFormulaInformation(String formula, double mass, boolean useFormula,PeakInformation peak){
		this.formulaString = formula;
		this.mass = mass;
		this.useFormula = useFormula;
		this.peak = peak;
	}
	
	public void setPeak(PeakInformation peak){
		this.peak = peak;
	}
	
	public void setMass(double mass){
		this.mass = mass;
	}
	
	public void setMolecularFormulaString(String formula){
		this.formulaString = formula;
	}

	@Override
	public String getMolecularFormula() {
		return formulaString;
	}

	@Override
	public double getMass() {
		return mass;
	}

	@Override
	public void useFormula(boolean use) {
		this.useFormula = use;
	}

	@Override
	public boolean formulaUsed() {
		return this.useFormula;
	}
	
	public String toString(){
		return this.formulaString+ " - "+this.mass;
	}

	@Override
	public PeakInformation getPeak() {
		return this.peak;
	}

}
