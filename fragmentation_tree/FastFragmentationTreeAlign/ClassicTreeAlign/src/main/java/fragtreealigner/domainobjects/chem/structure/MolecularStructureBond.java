
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *  
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker, 
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

package fragtreealigner.domainobjects.chem.structure;

import fragtreealigner.domainobjects.graphs.Edge;

@SuppressWarnings("serial")
public class MolecularStructureBond extends Edge<MolecularStructureBond, MolecularStructureAtom> {
	private BondType bondType;

	public MolecularStructureBond(MolecularStructureAtom atom1, MolecularStructureAtom atom2) {
		super(atom1, atom2);
	}

	public MolecularStructureBond(MolecularStructureAtom atom1, MolecularStructureAtom atom2, String label, BondType bondType) {
		super(atom1, atom2, label);
		this.bondType = bondType;
	}

	public void setContent(MolecularStructureBond bond) {
		super.setContent(bond);
		this.bondType = bond.getBondType();
	}

	public void setBondType(BondType bondType) {
		this.bondType = bondType;
	}

	public BondType getBondType() {
		return bondType;
	}
	
	@Override
	public String dotParams() {
		String dotParams = super.dotParams();
		dotParams += ", penwidth=\"" + (bondType.ordinal() * 3 + 1) + "\"";
		return dotParams;		
	}
}
