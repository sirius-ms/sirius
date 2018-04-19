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
