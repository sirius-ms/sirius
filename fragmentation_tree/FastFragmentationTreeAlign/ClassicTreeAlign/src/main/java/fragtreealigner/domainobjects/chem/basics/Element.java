
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

package fragtreealigner.domainobjects.chem.basics;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Element implements Serializable {
	private String symbol;
	private String name;
	private double mass;
	private int numberOfBondingElectrons;
	
	public Element(String symbol, String name, double mass, int numberOfBondingElectrons) {
		super();
		this.symbol = symbol;
		this.name = name;
		this.mass = mass;
		this.numberOfBondingElectrons = numberOfBondingElectrons;
	}

	public Element(String symbol) {
		super();
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}
	
	public String getName() {
		return name;
	}
	
	public double getMass() {
		return mass;
	}

	public int getNumberOfBondingElectrons() {
		return numberOfBondingElectrons;
	}
}
