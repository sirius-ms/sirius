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

import fragtreealigner.util.Session;

@SuppressWarnings("serial")
public class FunctionalGroup extends ChemicalComponent {
	private int type;
	
	public FunctionalGroup(String name, double mass, String molFormulaStr, int type, Session session) {
		super(name, mass, molFormulaStr, session);
		this.setType(type);
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
}
