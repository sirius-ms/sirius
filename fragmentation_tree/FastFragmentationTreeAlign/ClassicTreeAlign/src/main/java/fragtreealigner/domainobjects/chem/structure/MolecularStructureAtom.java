
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

import fragtreealigner.domainobjects.chem.basics.Element;
import fragtreealigner.domainobjects.graphs.Node;
import fragtreealigner.domainobjects.util.Coordinate;

@SuppressWarnings("serial")
public class MolecularStructureAtom extends Node<MolecularStructureAtom, MolecularStructureBond> {
	private Element elementType;
	private int hydrogenCount;
	private Coordinate coordinate;

	public MolecularStructureAtom() {
		super();
	}
	
	public MolecularStructureAtom(String label) {
		this();
		this.label = label;
	}
	
	public MolecularStructureAtom(String label, Element elementType, int hydrogenCount, Coordinate coordinate) {
		this(label);
		this.elementType = elementType;
		this.hydrogenCount = hydrogenCount;
		this.coordinate = coordinate;
	}
	
	public MolecularStructureAtom(String label, Element elementType, int hydrogenCount, double xCoordinate, double yCoordinate) {
		this(label, elementType, hydrogenCount, new Coordinate(xCoordinate, yCoordinate));
	}
	
	public void setElementType(Element elementType) {
		this.elementType = elementType;
	}

	public Element getElementType() {
		return elementType;
	}

	public void setHydrogenCount(int hydrogenCount) {
		this.hydrogenCount = hydrogenCount;
	}

	public int getHydrogenCount() {
		return hydrogenCount;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}
	
	public void setCoordinate(double x, double y) {
		this.coordinate = new Coordinate(x, y);
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}
	
	public double getXCoordinate() {
		return coordinate.getX();
	}

	public double getYCoordinate() {
		return coordinate.getY();
	}
	
	@Override
	public String dotParams() {
		String dotParams = super.dotParams();
		dotParams += ", pos=\"" + (coordinate.getX() / 2) + "," + (coordinate.getY() / 2) + "\"" + ", shape=\"circle\", width=\"0.5\", height=\"0.5\"";
		return dotParams;		
	}
	
	@Override
	public String toString() {
		String output = "";
		output += elementType.getSymbol();// + "\\n";
//		output += hydrogenCount;
		return output;
	}
}
