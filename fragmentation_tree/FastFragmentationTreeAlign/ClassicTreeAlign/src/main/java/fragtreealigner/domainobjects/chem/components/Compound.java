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
package fragtreealigner.domainobjects.chem.components;

import fragtreealigner.domainobjects.chem.basics.MolecularFormula;
import fragtreealigner.util.Session;

@SuppressWarnings("serial")
public class Compound extends ChemicalComponent {
	
	public Compound(String name, double mass) {
		super(name, mass);
	}	
	
	public Compound(String name, double mass, MolecularFormula molecularFormula) {
		this(name, mass);
		this.molecularFormula = molecularFormula;
	}
	
	public Compound(String name, double mass, MolecularFormula molecularFormula, Session session) {
		this(name, mass, molecularFormula);
		this.session = session;
	}
	
	public Compound(String name, double mass, String molFormulaStr, Session session) {
		this(name, mass, new MolecularFormula(molFormulaStr, session));
		this.session = session;
	}
	
	@Override
	public Compound clone() {
		Compound clonedCompound = new Compound(new String(name), mass, new MolecularFormula(molecularFormula.toString(), session), session);
		return clonedCompound;
	}
}
