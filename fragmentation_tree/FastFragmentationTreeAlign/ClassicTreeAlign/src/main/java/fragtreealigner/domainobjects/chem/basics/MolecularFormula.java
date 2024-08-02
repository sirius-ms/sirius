
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

import fragtreealigner.util.Session;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class MolecularFormula implements Serializable {
	private HashMap<String, Integer> numberOfAtomsHash;
	private int[] numberOfAtomsList;
	private Session session;
	
	public MolecularFormula(Session session) {
		this.session = session;
	}
	
	public MolecularFormula(String molFormulaStr, Session session) {
		this.session = session;
		numberOfAtomsHash = parseMolecularFormula(molFormulaStr, session);
		updateNumberOfAtomsList();
	}

	public MolecularFormula(int[] numberOfAtomsList, Session session) {
		this.numberOfAtomsList = numberOfAtomsList;
		this.session = session;
		updateNumberOfAtomsHash();
	}
	
	public int[] getNumberOfAtomsAsList() {
		return numberOfAtomsList;
	}

	public HashMap<String, Integer> getNumberOfAtomsAsHash() {
		return numberOfAtomsHash;
	}

	public void setNumberOfAtomsAsList(int[] numberOfAtomsList) {
		this.numberOfAtomsList = numberOfAtomsList;
		updateNumberOfAtomsHash();
	}
	
	public int getNumberOfAtom(String elementSymbol) {
		if (numberOfAtomsHash.containsKey(elementSymbol)) return numberOfAtomsHash.get(elementSymbol);
		else return 0;
	}

	public void setNumberOfAtom(String elementSymbol, int quantity) {
		numberOfAtomsHash.put(elementSymbol, quantity);
		numberOfAtomsList[session.getParameters().elementTable.getElementList().indexOf(elementSymbol)] = quantity;
	}

	private void updateNumberOfAtomsList() {
		ElementTable elementTable = session.getParameters().elementTable;
		numberOfAtomsList = new int[elementTable.getNumberOfElements()];
		Integer quantity;
		int i = 0;
		for (String elementSymbol: elementTable.getElementList()) {
			quantity = numberOfAtomsHash.get(elementSymbol);
			numberOfAtomsList[i] = (quantity == null) ? 0 : quantity;
			i++;
		}
	}

	private void updateNumberOfAtomsHash() {
		ElementTable elementTable = session.getParameters().elementTable;
		numberOfAtomsHash = new HashMap<String, Integer>();
		Integer quantity;
		int i = 0;
		for (String elementSymbol: elementTable.getElementList()) {
			quantity = numberOfAtomsList[i];
			if (quantity != 0) numberOfAtomsHash.put(elementSymbol, quantity);
			i++;
		}
	}
	
	public String toString() {
		ElementTable elementTable = session.getParameters().elementTable;
		String molFormula = "";
		Integer quantity;
		for (String elementSymbol: elementTable.getElementList()) {
			quantity = numberOfAtomsHash.get(elementSymbol);
			if (quantity != null && quantity != 0) {
				molFormula += elementSymbol;
				if (quantity != 1) molFormula += Integer.toString(quantity);
			}
		}
		return molFormula;
	}

	public String toCmlString() {
		ElementTable elementTable = session.getParameters().elementTable;
		String molFormula = "";
		Integer quantity;
		for (String elementSymbol: elementTable.getElementList()) {
			quantity = numberOfAtomsHash.get(elementSymbol);
			if (quantity != null && quantity > 0) {
				molFormula += elementSymbol;
				molFormula += " ";
				molFormula += Integer.toString(quantity);	
				molFormula += " ";
			}
		}
		return molFormula;
	}

	public int size() {
		int quantity = 0;
		for (String elementSymbol: numberOfAtomsHash.keySet()) {
			if (!elementSymbol.equals("H")) quantity += numberOfAtomsHash.get(elementSymbol);
		}
		return quantity;
	}
	
	public MolecularFormula diff(MolecularFormula molFormula) {
		MolecularFormula molFormulaDiff = new MolecularFormula(session);
		int[] numberOfAtomsList1 = this.getNumberOfAtomsAsList();
		int[] numberOfAtomsList2 = molFormula.getNumberOfAtomsAsList();
		int[] numberOfAtomsListDiff = new int[numberOfAtomsList1.length];
		for (int i = 0; i < numberOfAtomsList1.length; i++) {
			numberOfAtomsListDiff[i] = numberOfAtomsList1[i] - numberOfAtomsList2[i];
		}
		molFormulaDiff.setNumberOfAtomsAsList(numberOfAtomsListDiff);
		return molFormulaDiff;
	}

	public MolecularFormula add(MolecularFormula molFormula) {
		MolecularFormula molFormulaDiff = new MolecularFormula(session);
		int[] numberOfAtomsList1 = this.getNumberOfAtomsAsList();
		int[] numberOfAtomsList2 = molFormula.getNumberOfAtomsAsList();
		int[] numberOfAtomsListAdd = new int[numberOfAtomsList1.length];
		for (int i = 0; i < numberOfAtomsList1.length; i++) {
			numberOfAtomsListAdd[i] = numberOfAtomsList1[i] + numberOfAtomsList2[i];
		}
		molFormulaDiff.setNumberOfAtomsAsList(numberOfAtomsListAdd);
		return molFormulaDiff;
	}
	
	public static HashMap<String, Integer> parseMolecularFormula(String molFormulaStr, Session session) {
		HashMap<String, Integer> numberOfAtoms = new HashMap<String, Integer>();
		Pattern pElement = Pattern.compile( "([A-Z][a-z]*) ?([0-9]*)" ); 
		Matcher mElement;
		mElement = pElement.matcher(molFormulaStr);
		String elementSymbol;
		int amount;
		while (mElement.find()) {
			elementSymbol = mElement.group(1);
			if (elementSymbol.equals("D") && session.getParameters().DmatchesH){
				elementSymbol = "H";
			}
			if (elementSymbol.equals("Tms")) continue;
			amount = (mElement.group(2).equalsIgnoreCase("")) ? 1 : Integer.valueOf(mElement.group(2));
			if (!numberOfAtoms.containsKey(elementSymbol)) numberOfAtoms.put(elementSymbol, amount);
			else numberOfAtoms.put(elementSymbol, numberOfAtoms.get(elementSymbol) + amount);
		}
		return numberOfAtoms;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) return false; 
		if ( obj == this ) return true; 
		if ( ! obj.getClass().equals(getClass()) ) return false; 
		
		MolecularFormula molecularFormula = (MolecularFormula) obj; 
		return Arrays.equals(this.getNumberOfAtomsAsList(), molecularFormula.getNumberOfAtomsAsList());
	}
}
