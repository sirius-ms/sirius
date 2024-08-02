
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

package fragtreealigner.domainobjects.chem.components;

import java.io.Serializable;


@SuppressWarnings("serial")
public class FunctionalGroupSubstitution implements Serializable {
	private FunctionalGroup functionalGroupBefore;
	private FunctionalGroup functionalGroupAfter;
	private boolean doubleBondBefore;
	private boolean doubleBondAfter;
	
	public FunctionalGroupSubstitution(FunctionalGroup functionalGroupBefore, FunctionalGroup functionalGroupAfter, boolean doubleBondBefore, boolean doubleBondAfter) {
		super();
		this.functionalGroupBefore = functionalGroupBefore;
		this.functionalGroupAfter = functionalGroupAfter;
		this.doubleBondBefore = doubleBondBefore;
		this.doubleBondAfter = doubleBondAfter;
	}

	public FunctionalGroup getFunctionalGroupBefore() {
		return functionalGroupBefore;
	}
	
	public FunctionalGroup getFunctionalGroupAfter() {
		return functionalGroupAfter;
	}
	
	public String getFunctionalGroupBeforeAsString() {
		return functionalGroupBefore.getMolecularFormula().toString();
	}
	
	public String getFunctionalGroupAfterAsString() {
		return functionalGroupAfter.getMolecularFormula().toString();
	}	
	
	public boolean isDoubleBondBefore() {
		return doubleBondBefore;
	}
	
	public boolean isDoubleBondAfter() {
		return doubleBondAfter;
	}
}
